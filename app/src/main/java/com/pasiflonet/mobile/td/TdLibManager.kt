package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.utils.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

object TdLibManager {
    private var client: Client? = null
    private var isAuthorized = false
    private var appContext: Context? = null

    private const val MAX_MESSAGES = 100

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState

    private val _currentMessages = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val currentMessages: StateFlow<List<TdApi.Message>> = _currentMessages

    fun init(context: Context, apiId: Int, apiHash: String) {
        if (client != null) return
        appContext = context.applicationContext

        // שקט לוגים של TDLib (לא קבצי לוג, רק רמת verbosity פנימית)
        Client.execute(TdApi.SetLogVerbosityLevel(0))

        client = Client.create({ update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState, apiId, apiHash)
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

    private fun handleAuthState(state: TdApi.AuthorizationState, apiId: Int, apiHash: String) {
        _authState.value = state

        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val ctx = appContext ?: return
                val params = TdApi.TdlibParameters().apply {
                    databaseDirectory = File(ctx.filesDir, "tdlib_db").absolutePath
                    filesDirectory = File(ctx.filesDir, "tdlib_files").absolutePath
                    useMessageDatabase = true
                    useSecretChats = false
                    this.apiId = apiId
                    this.apiHash = apiHash
                    systemLanguageCode = "en"
                    deviceModel = "Android"
                    systemVersion = android.os.Build.VERSION.RELEASE ?: "Unknown"
                    applicationVersion = "1.0"
                    enableStorageOptimizer = true
                }
                client?.send(TdApi.SetTdlibParameters(params)) {}
            }
            is TdApi.AuthorizationStateReady -> isAuthorized = true
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

    fun sendFinalMessage(targetUsername: String, caption: String, filePath: String?, isVideo: Boolean) {
        if (!isAuthorized) return
        val username = targetUsername.removePrefix("@")

        client?.send(TdApi.SearchPublicChat(username)) { chatRes ->
            if (chatRes !is TdApi.Chat) return@send
            val chatId = chatRes.id

            // טקסט בלבד
            if (filePath.isNullOrBlank()) {
                val inputText = TdApi.InputMessageText(TdApi.FormattedText(caption, null), null, false)
                client?.send(TdApi.SendMessage(chatId, 0, null, null, null, inputText)) {}
                return@send
            }

            val local = File(filePath)
            if (!local.exists()) {
                Log.e("TdLib", "sendFinalMessage: file not found: $filePath")
                return@send
            }

            val inputFile = TdApi.InputFileLocal(local.absolutePath)
            val content: TdApi.InputMessageContent =
                if (isVideo) TdApi.InputMessageVideo(inputFile, null, null, 0, 0, 0, false, false, TdApi.FormattedText(caption, null))
                else TdApi.InputMessagePhoto(inputFile, null, null, 0, TdApi.FormattedText(caption, null))

            client?.send(TdApi.SendMessage(chatId, 0, null, null, null, content)) {}
        }
    }
}
