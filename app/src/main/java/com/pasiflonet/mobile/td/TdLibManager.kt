package com.pasiflonet.mobile.td

import android.content.Context
import android.os.Build
import com.pasiflonet.mobile.utils.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object TdLibManager {
    private var client: Client? = null
    private var appContext: Context? = null
    private var isAuthorized = false
    private const val MAX_MESSAGES = 160

    private val _authState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authState: StateFlow<TdApi.AuthorizationState?> = _authState

    private val _currentMessages = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val currentMessages: StateFlow<List<TdApi.Message>> = _currentMessages

    fun init(context: Context, apiId: Int, apiHash: String) {
        if (client != null) return
        appContext = context.applicationContext

        Client.execute(TdApi.SetLogVerbosityLevel(0))

        client = Client.create({ update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> handleAuth(update.authorizationState, apiId, apiHash)
                is TdApi.UpdateNewMessage -> { upsertMessage(update.message); appContext?.let { com.pasiflonet.mobile.utils.KeywordNotificationHelper.notifyIfMatches(it, update.message) } }
                is TdApi.UpdateMessageContent -> refreshMessage(update.chatId, update.messageId)
                is TdApi.UpdateMessageEdited -> refreshMessage(update.chatId, update.messageId)
                is TdApi.UpdateChatLastMessage -> refreshRecentMessages()
                is TdApi.UpdateConnectionState -> {
                    if (update.state is TdApi.ConnectionStateReady) {
                        setOnline(true)
                        refreshRecentMessages()
                    }
                }
                is TdApi.UpdateMessageSendSucceeded -> {
                    removeMessage(update.message.chatId, update.oldMessageId)
                    upsertMessage(update.message)
                }
                is TdApi.UpdateMessageSendFailed -> upsertMessage(update.message)
                is TdApi.UpdateDeleteMessages -> removeMessages(update.chatId, update.messageIds)
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
                val p = TdApi.SetTdlibParameters(
                    false,
                    dbDir,
                    filesDir,
                    null,
                    true,
                    true,
                    true,
                    false,
                    apiId,
                    apiHash,
                    "en",
                    Build.MODEL ?: "Android",
                    Build.VERSION.RELEASE ?: "Android",
                    "Azretr"
                )
                client?.send(p) {}
            }
            is TdApi.AuthorizationStateReady -> {
                isAuthorized = true
                setOnline(true)
                refreshRecentMessages()
            }
            is TdApi.AuthorizationStateClosed -> isAuthorized = false
        }
    }

    private fun upsertMessage(message: TdApi.Message) {
        val current = _currentMessages.value.toMutableList()
        val idx = current.indexOfFirst { it.chatId == message.chatId && it.id == message.id }
        if (idx >= 0) current[idx] = message else current.add(message)

        current.sortWith(compareByDescending<TdApi.Message> { it.date }.thenByDescending { it.id })
        while (current.size > MAX_MESSAGES) {
            val removed = current.removeAt(current.size - 1)
            appContext?.let { ctx ->
                try {
                    CacheManager.deleteTempForMessage(ctx, removed)
                    CacheManager.pruneAppTempFiles(ctx, 250)
                } catch (_: Exception) {
                }
            }
        }

        _currentMessages.value = current.toList()

        try {
            val previewId = when (val c = message.content) {
                is TdApi.MessagePhoto -> {
                    val previewPhoto =
                        c.photo.sizes.find { it.type == "x" } ?:
                        c.photo.sizes.find { it.type == "y" } ?:
                        c.photo.sizes.find { it.type == "w" } ?:
                        c.photo.sizes.lastOrNull() ?:
                        c.photo.sizes.firstOrNull()
                    previewPhoto?.photo?.id ?: 0
                }
                is TdApi.MessageVideo -> c.video.thumbnail?.file?.id ?: 0
                else -> 0
            }
            if (previewId != 0) {
                downloadFile(previewId)
            }
        } catch (_: Exception) {
        }
    }


    private fun refreshMessage(chatId: Long, messageId: Long) {
        val c = client ?: return
        c.send(TdApi.GetMessage(chatId, messageId)) { res ->
            if (res is TdApi.Message) {
                upsertMessage(res)
            }
        }
    }

    private fun removeMessage(chatId: Long, messageId: Long) {
        _currentMessages.value = _currentMessages.value.filterNot { it.chatId == chatId && it.id == messageId }
    }

    private fun removeMessages(chatId: Long, messageIds: LongArray) {
        val ids = messageIds.toSet()
        _currentMessages.value = _currentMessages.value.filterNot { it.chatId == chatId && ids.contains(it.id) }
    }

    fun refreshRecentMessages() {
        val c = client ?: return
        if (!isAuthorized) return

        c.send(TdApi.GetChats(TdApi.ChatListMain(), 40)) { res ->
            if (res !is TdApi.Chats) return@send
            res.chatIds.take(30).forEach { chatId ->
                c.send(TdApi.GetChatHistory(chatId, 0, 0, 12, false)) { history ->
                    if (history is TdApi.Messages) {
                        val merged = _currentMessages.value.toMutableList()
                        history.messages.forEach { msg ->
                            val idx = merged.indexOfFirst { it.chatId == msg.chatId && it.id == msg.id }
                            if (idx >= 0) merged[idx] = msg else merged.add(msg)
                        }
                        merged.sortWith(compareByDescending<TdApi.Message> { it.date }.thenByDescending { it.id })
                        _currentMessages.value = merged.take(MAX_MESSAGES)
                    }
                }
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

    fun getFilePath(fileId: Int): String? {
        if (fileId == 0) return null
        val c = client ?: return null
        val latch = CountDownLatch(1)
        var out: String? = null
        c.send(TdApi.GetFile(fileId)) { r ->
            if (r is TdApi.File) out = r.local?.path
            latch.countDown()
        }
        latch.await(1500, TimeUnit.MILLISECONDS)
        return out
    }

    fun getFilePath(fileId: Int, onResult: (String?) -> Unit) {
        val c = client ?: run {
            onResult(null)
            return
        }
        c.send(TdApi.GetFile(fileId)) { r ->
            if (r is TdApi.File) onResult(r.local?.path) else onResult(null)
        }
    }

    private fun buildCaptionWithSignature(text: String): String {
        val base = text.trim()
        if (base.isBlank()) return ""
        val signature = appContext
            ?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            ?.getString("text_signature", "")
            ?.trim()
            .orEmpty()

        return if (signature.isBlank()) base else "$base\n\n$signature"
    }

    fun sendFinalMessage(targetUsername: String, caption: String, filePath: String?, silent: Boolean) {
        sendFinalMessage(targetUsername, caption, filePath) { }
    }

    fun sendFinalMessage(
        targetUsername: String,
        caption: String,
        filePath: String?,
        onError: (String) -> Unit = {}
    ) {
        if (!isAuthorized) {
            onError("Not authorized")
            return
        }

        val username = targetUsername.trim().removePrefix("@")
        if (username.isBlank()) {
            onError("Target username is empty")
            return
        }

        val c = client ?: run {
            onError("Client null")
            return
        }

        c.send(TdApi.SearchPublicChat(username)) { chatRes ->
            when (chatRes) {
                is TdApi.Error -> {
                    onError(chatRes.message)
                    return@send
                }
                !is TdApi.Chat -> {
                    onError("Chat not found")
                    return@send
                }
            }

            val chatId = (chatRes as TdApi.Chat).id
            val finalCaption = buildCaptionWithSignature(caption)

            val content: TdApi.InputMessageContent =
                if (filePath.isNullOrBlank()) {
                    val linkPreviewOptions = TdApi.LinkPreviewOptions(
                        true, "", false, false, false
                    )
                    TdApi.InputMessageText(
                        TdApi.FormattedText(finalCaption, null),
                        linkPreviewOptions,
                        false
                    )
                } else {
                    val f = File(filePath)
                    val input = TdApi.InputFileLocal(f.absolutePath)
                    val ft = TdApi.FormattedText(finalCaption, null)

                    if (filePath.endsWith(".mp4", true)) {
                        TdApi.InputMessageVideo(
                            input, null, null, 0, intArrayOf(),
                            0, 0, 0, true, ft, false, null, false
                        )
                    } else {
                        TdApi.InputMessagePhoto(
                            input, null, intArrayOf(), 0, 0, ft, false, null, false
                        )
                    }
                }

            c.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { r ->
                if (r is TdApi.Error) onError(r.message)
            }
        }
    }

    fun setOnline(online: Boolean) {
        val c = client ?: return
        try {
            c.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(online))) {}
            c.send(TdApi.SetOption("is_background", TdApi.OptionValueBoolean(!online))) {}
        } catch (_: Exception) {
        }
    }
}
