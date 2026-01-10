package com.pasiflonet.mobile.td

import android.content.Context
import android.os.Build
import com.pasiflonet.mobile.utils.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

object TdLibManager {

    private var client: Client? = null
    private var appContext: Context? = null
    private var isAuthorized: Boolean = false

    private const val MAX_MESSAGES = 100

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState

    private val _currentMessages = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val currentMessages: StateFlow<List<TdApi.Message>> = _currentMessages

    fun init(context: Context, apiId: Int, apiHash: String) {
        if (client != null) return
        appContext = context.applicationContext

        // שקט מוחלט בלוגים של TDLib
        Client.execute(TdApi.SetLogVerbosityLevel(0))

        client = Client.create({ update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> handleAuth(update.authorizationState, apiId, apiHash)
                is TdApi.UpdateNewMessage -> {
                    val list = _currentMessages.value.toMutableList()
                    list.add(0, update.message) // newest first

                    if (list.size > MAX_MESSAGES) {
                        val removed = list.removeAt(list.size - 1) // oldest
                        appContext?.let { ctx ->
                            try {
                                CacheManager.deleteTempForMessage(ctx, removed)
                                CacheManager.pruneAppTempFiles(ctx, keep = 250)
                            } catch (_: Exception) {}
                        }
                    }

                    _currentMessages.value = list
                }
            }
        }, null, null)
    }

    private fun handleAuth(state: TdApi.AuthorizationState, apiId: Int, apiHash: String) {
        _authState.value = state

        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val ctx = appContext ?: return
                val dbDir = File(ctx.filesDir, "tdlib_db").absolutePath
                val filesDir = File(ctx.filesDir, "tdlib_files").absolutePath

                // אצלך אין TdlibParameters(), לכן משתמשים ב-SetTdlibParameters
                val p = TdApi.SetTdlibParameters(
                    false,                // useTestDc
                    dbDir,                // databaseDirectory
                    filesDir,             // filesDirectory
                    null,                 // databaseEncryptionKey (ByteArray)
                    true,                 // useFileDatabase
                    true,                 // useChatInfoDatabase
                    true,                 // useMessageDatabase
                    false,                // useSecretChats
                    apiId,
                    apiHash,
                    "en",
                    Build.MODEL ?: "Android",
                    Build.VERSION.RELEASE ?: "Android",
                    "Azretr"
                )

                client?.send(p) { /* ignore */ }
            }

            is TdApi.AuthorizationStateReady -> {
                isAuthorized = true
            }

            is TdApi.AuthorizationStateClosed -> {
                isAuthorized = false
            }
        }
    }

    fun sendPhone(phone: String, onError: (String) -> Unit) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null)) { r ->
            if (r is TdApi.Error) onError(r.message)
        }
    }

    fun sendCode(code: String, onError: (String) -> Unit) {
        client?.send(TdApi.CheckAuthenticationCode(code)) { r ->
            if (r is TdApi.Error) onError(r.message)
        }
    }

    fun sendPassword(password: String, onError: (String) -> Unit) {
        client?.send(TdApi.CheckAuthenticationPassword(password)) { r ->
            if (r is TdApi.Error) onError(r.message)
        }
    }

    fun downloadFile(fileId: Int) {
        client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) {}
    }

    fun getFilePath(fileId: Int, onResult: (String?) -> Unit) {
        client?.send(TdApi.GetFile(fileId)) { obj ->
            if (obj is TdApi.File) onResult(obj.local?.path) else onResult(null)
        }
    }

    /**
     * שליחת טקסט בלבד או קובץ.
     * חשוב: אצלך SendMessage מקבל topicId מסוג MessageTopic, לכן שולחים null (לא 0).
     */
    fun sendFinalMessage(
        targetUsername: String,
        caption: String,
        filePath: String?,
        onError: (String) -> Unit = {}
    ) {
        if (!isAuthorized) { onError("Not authorized"); return }

        val username = targetUsername.trim().removePrefix("@")
        if (username.isBlank()) { onError("Target username is empty"); return }

        client?.send(TdApi.SearchPublicChat(username)) { chatRes ->
            when (chatRes) {
                is TdApi.Error -> { onError(chatRes.message); return@send }
                !is TdApi.Chat -> { onError("Chat not found"); return@send }
            }

            val chatId = chatRes.id

            val content: TdApi.InputMessageContent =
                if (filePath.isNullOrBlank()) {
                    TdApi.InputMessageText(TdApi.FormattedText(caption, null), null, false)
                } else {
                    val f = File(filePath)
                    val input = TdApi.InputFileLocal(f.absolutePath)
                    val ft = TdApi.FormattedText(caption, null)

                    if (filePath.endsWith(".mp4", true)) {
                        TdApi.InputMessageVideo(
                            input,               // video
                            null,                // thumbnail
                            null,                // addedVideoStickerFile (InputFile)
                            0,                   // startTimestamp
                            intArrayOf(),         // addedStickerFileIds
                            0, 0, 0,             // duration,width,height (0=unknown)
                            true,                // supportsStreaming
                            ft,
                            false,               // showCaptionAboveMedia
                            null,                // selfDestructType
                            false                // hasSpoiler
                        )
                    } else {
                        TdApi.InputMessagePhoto(
                            input,
                            null,                // thumbnail
                            intArrayOf(),         // addedStickerFileIds
                            0, 0,                // width,height
                            ft,
                            false,               // showCaptionAboveMedia
                            null,                // selfDestructType
                            false                // hasSpoiler
                        )
                    }
                }

            client?.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { r ->
                if (r is TdApi.Error) onError(r.message)
            }
        }
    }
}
