package com.thelightphone.sdk.shared

import com.thelightphone.sdk.shared.LightServiceMethod.SetRingtone.Request
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

val lightJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Defines a typed method that a client can call on the server's bound service.
 */
sealed interface LightServiceMethod<TRequest, TResponse> {

    val id: String
    val requestSerializer: KSerializer<TRequest>
    val responseSerializer: KSerializer<TResponse>

    fun encodeRequest(request: TRequest): String =
        lightJson.encodeToString(requestSerializer, request)

    fun decodeRequest(json: String): TRequest =
        lightJson.decodeFromString(requestSerializer, json)

    fun encodeResponse(response: TResponse): String =
        lightJson.encodeToString(responseSerializer, response)

    fun decodeResponse(json: String): TResponse =
        lightJson.decodeFromString(responseSerializer, json)

    /**
     * Define all service methods below. DO NOT CHANGE EXISTING METHODS
     */
    object GetToken : LightServiceMethod<Unit, GetToken.Response> {
        override val id = "GetToken"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val token: String)
    }

    object GetVersion : LightServiceMethod<Unit, GetVersion.Response> {
        override val id = "GetVersion"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val version: String)
    }

    object SetRingtone : LightServiceMethod<Request, Unit> {
        override val id = "SetRingtone"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val type: Int, val uri: String)
    }

    object GetKeyboardOptions : LightServiceMethod<Unit, GetKeyboardOptions.Response> {
        override val id = "GetKeyboardOptions"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            // "😅😅😅😅😅😅" -> keyboard will parse out emoji code points
            val emojisAsString: String?,
            val displayVoice: Boolean,
            val enableKeyAnimation: Boolean,
            // optional for older sdk servers that omit this field
            val swipeEnabled: Boolean? = null,
        )
    }

    object GetUserPreferences : LightServiceMethod<Unit, GetUserPreferences.Response> {
        override val id = "GetUserPreferences"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val hapticsEnabled: Boolean,
        )
    }

    object GetPermission : LightServiceMethod<GetPermission.Request, GetPermission.Response> {
        enum class Result {
            Unknown, BlockedByServer, Granted, Denied
        }
        override val id = "GetPermission"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val permissionName: String)

        @Serializable
        data class Response(
            val permissionResult: Result
        )
    }

    object RequestPermissionComponent : LightServiceMethod<Unit, RequestPermissionComponent.Response> {
        const val PERMISSION_NAME_KEY = "PermissionName"
        override val id = "RequestPermissionComponent"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val componentName: String)
    }

    object DeviceKeyEvent : LightServiceMethod<DeviceKeyEvent.Request, Unit> {
        override val id = "DeviceKeyEvent"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val keyCode: Int,
            val repeatCount: Int?,
            val action: Int, // Android KeyEvent actions
            val characters: String?,
            val unicodeChar: Int,
            // if this key event will trigger the server to take over the screen
            // optionally pass the flattened component to re-launch when it is done
            val componentToRelaunch: String?,
        )
    }

    object OpenDialer : LightServiceMethod<OpenDialer.Request, Unit> {
        override val id = "OpenDialer"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val phoneNumber: String,
        )
    }

    // --- Media methods (local, additive addition for the Audiobooks companion;
    // upstreamable — production com.lightos should implement these for a real LP3).
    object GetBooks : LightServiceMethod<Unit, GetBooks.Response> {
        override val id = "GetBooks"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Part(
            val title: String,
            val durationMs: Long,
        )

        @Serializable
        data class Book(
            val id: String,
            val title: String,
            val author: String,
            val durationMs: Long,
            val progressMs: Long,
            val partCount: Int,
            val parts: List<Part> = emptyList(),
        )

        @Serializable
        data class Response(val books: List<Book>)
    }

    object ScanLibrary : LightServiceMethod<Unit, GetBooks.Response> {
        override val id = "ScanLibrary"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<GetBooks.Response>()
    }

    /**
     * Deletes a book's files. On Android 11+ the companion needs the system
     * consent dialog to delete media it doesn't own; [Response.consentPending]
     * signals that the dialog was shown and the deletion completes if the user
     * confirms (the tool's library refreshes when it resumes).
     */
    object DeleteBook : LightServiceMethod<DeleteBook.Request, DeleteBook.Response> {
        override val id = "DeleteBook"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val bookId: String)

        @Serializable
        data class Response(
            val deleted: Boolean = false,
            val consentPending: Boolean = false,
            /** Simple class name of the companion's consent activity to launch (resolved in the server package) when [consentPending]. */
            val consentComponent: String? = null,
        )
    }

    /**
     * Jumps to a part (chapter) on the already-loaded book, preserving the
     * current play/pause state — switching chapters never starts playback.
     */
    object SeekToPart : LightServiceMethod<SeekToPart.Request, Unit> {
        override val id = "SeekToPart"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val partIndex: Int)
    }

    /** Whether playback should continue into the next chapter when one ends. */
    object GetAutoPlayNext : LightServiceMethod<Unit, GetAutoPlayNext.Response> {
        override val id = "GetAutoPlayNext"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val enabled: Boolean)
    }

    object SetAutoPlayNext : LightServiceMethod<SetAutoPlayNext.Request, Unit> {
        override val id = "SetAutoPlayNext"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val enabled: Boolean)
    }

    object PlayBook : LightServiceMethod<PlayBook.Request, Unit> {
        override val id = "PlayBook"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val bookId: String,
            val partIndex: Int = 0,
            val positionMs: Long = 0,
        )
    }

    /** Loads a book paused at its saved position (the player opens, nothing plays). */
    object OpenBook : LightServiceMethod<OpenBook.Request, Unit> {
        override val id = "OpenBook"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val bookId: String,
            val partIndex: Int = 0,
            val positionMs: Long = 0,
        )
    }

    object PausePlayback : LightServiceMethod<Unit, Unit> {
        override val id = "PausePlayback"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Unit>()
    }

    object SeekTo : LightServiceMethod<SeekTo.Request, Unit> {
        override val id = "SeekTo"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val positionMs: Long)
    }

    object SetPlaybackSpeed : LightServiceMethod<SetPlaybackSpeed.Request, Unit> {
        override val id = "SetPlaybackSpeed"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val speed: Float)
    }

    object GetPlaybackState : LightServiceMethod<Unit, GetPlaybackState.Response> {
        override val id = "GetPlaybackState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val bookId: String? = null,
            val title: String? = null,
            val author: String? = null,
            val partIndex: Int = 0,
            val partCount: Int = 0,
            val partTitle: String? = null,
            val positionMs: Long = 0,
            val durationMs: Long = 0,
            val playing: Boolean = false,
            val speed: Float = 1.0f,
        )
    }

    // --- Passes methods (local, additive addition for the Passes companion;
    // upstreamable — production com.lightos should implement these for a real LP3).
    object GetPasses : LightServiceMethod<Unit, GetPasses.Response> {
        override val id = "GetPasses"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Pass(
            val id: String,
            val name: String,
            val data: String,
            /** Base64-encoded raw (binary) payload, when the code carries one. */
            val rawData: String? = null,
            val type: String,
            /** Optional detail fields — only filled ones are shown in the tool. */
            val issuer: String? = null,
            val date: String? = null,
            val startTime: String? = null,
            val endTime: String? = null,
            val location: String? = null,
            val notes: String? = null,
        )

        @Serializable
        data class Response(val passes: List<Pass>)
    }

    object AddPass : LightServiceMethod<AddPass.Request, Unit> {
        override val id = "AddPass"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val name: String,
            val data: String,
            val rawData: String? = null,
            val type: String,
        )
    }

    object UpdatePass : LightServiceMethod<UpdatePass.Request, Unit> {
        override val id = "UpdatePass"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val passId: String,
            val name: String,
            val issuer: String? = null,
            val date: String? = null,
            val startTime: String? = null,
            val endTime: String? = null,
            val location: String? = null,
            val notes: String? = null,
        )
    }

    object DeletePass : LightServiceMethod<DeletePass.Request, Unit> {
        override val id = "DeletePass"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val passId: String)
    }

    /** Renders a pass's barcode in the companion; the PNG bytes cross the binder as base64. */
    object GetBarcode : LightServiceMethod<GetBarcode.Request, GetBarcode.Response> {
        override val id = "GetBarcode"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val passId: String,
            /** Target render width in pixels; the companion snaps to exact modules below it. */
            val width: Int = 960,
        )

        @Serializable
        data class Response(val png: ByteArray)
    }

    // --- Chats methods (local, additive addition for the Chats companion;
    // upstreamable — production com.lightos should implement these for a real LP3).
    object ChatPing : LightServiceMethod<Unit, Unit> {
        override val id = "ChatPing"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Unit>()
    }

    /**
     * Logs the Matrix account in on the companion: [Request.passwordOrToken] is
     * used as a password unless [Request.tokenLogin] is set, in which case it is
     * an access token (m.login.token). The homeserver string may be a bare
     * domain; the companion resolves it (with .well-known discovery) before login.
     */
    object SetAccount : LightServiceMethod<SetAccount.Request, SetAccount.Response> {
        override val id = "SetAccount"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val homeserver: String,
            val user: String,
            val passwordOrToken: String,
            val tokenLogin: Boolean = false,
        )

        @Serializable
        data class Response(
            val userId: String,
            val deviceId: String,
        )
    }

    /**
     * Requests a Beeper login code by email. The companion starts a Beeper
     * login request (Beeper's private API — endpoint/token live only in the
     * companion) and emails [Request.email] a 6-digit code, which is then
     * exchanged via [SetBeeperAccount].
     */
    object BeeperRequestCode : LightServiceMethod<BeeperRequestCode.Request, Unit> {
        override val id = "BeeperRequestCode"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val email: String)
    }

    /**
     * Completes a Beeper account login with the emailed [Request.code]. The
     * companion verifies the code against Beeper's private API, then performs a
     * Matrix JWT login (org.matrix.login.jwt) to matrix.beeper.com — the v1
     * login path (WhatsApp via Beeper's own bridges).
     */
    object SetBeeperAccount : LightServiceMethod<SetBeeperAccount.Request, SetBeeperAccount.Response> {
        override val id = "SetBeeperAccount"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val email: String,
            val code: String,
        )

        @Serializable
        data class Response(
            val userId: String,
            val deviceId: String,
        )
    }

    object GetAccountState : LightServiceMethod<Unit, GetAccountState.Response> {
        override val id = "GetAccountState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val loggedIn: Boolean,
            val userId: String? = null,
            val homeserver: String? = null,
            /** "beeper" (v1, Beeper account) | "homeserver" (dev/test) | null. */
            val loginMode: String? = null,
        )
    }

    object Logout : LightServiceMethod<Unit, Unit> {
        override val id = "Logout"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Unit>()
    }

    /** Snapshot of the user's rooms, newest activity first. */
    object GetRooms : LightServiceMethod<Unit, GetRooms.Response> {
        override val id = "GetRooms"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Room(
            val id: String,
            val name: String,
            val lastMessage: String,
            val unreadCount: Long,
            val lastTimestampMs: Long,
            /** Id of the newest timeline event, for the thread's initial cursor. */
            val lastEventId: String? = null,
        )

        @Serializable
        data class Response(val rooms: List<Room>)
    }

    /**
     * Page of messages in a room, oldest first. Pass [Request.beforeEventId] to
     * page further back; null returns the newest [Request.limit] messages.
     */
    object GetMessages : LightServiceMethod<GetMessages.Request, GetMessages.Response> {
        override val id = "GetMessages"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val roomId: String,
            val beforeEventId: String? = null,
            val limit: Int = 30,
        )

        @Serializable
        data class Message(
            val id: String,
            val sender: String,
            val senderName: String,
            val body: String,
            val timestampMs: Long,
            val isMine: Boolean,
        )

        @Serializable
        data class Response(
            val messages: List<Message>,
            /**
             * Whether there are more messages older than this page. The
             * companion computes it from the raw timeline page (not the
             * message-filtered list), so state events / still-encrypted events
             * don't truncate pagination early.
             */
            val hasMore: Boolean = false,
            /**
             * Whether this room is end-to-end encrypted. When the device isn't
             * verified the companion returns this without fetching (the events
             * can't decrypt anyway), so the tool can say so immediately.
             */
            val encrypted: Boolean = false,
        )
    }

    object SendMessage : LightServiceMethod<SendMessage.Request, SendMessage.Response> {
        override val id = "SendMessage"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val roomId: String,
            val body: String,
            val replyToEventId: String? = null,
        )

        /** The outbox transaction id; the message may still be pending delivery. */
        @Serializable
        data class Response(val transactionId: String)
    }

    object MarkRead : LightServiceMethod<MarkRead.Request, Unit> {
        override val id = "MarkRead"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val eventId: String)
    }

    object SetTyping : LightServiceMethod<SetTyping.Request, Unit> {
        override val id = "SetTyping"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val active: Boolean)
    }

    /** Companion connection state, for the tool's Settings/status display. */
    object GetConnectionState : LightServiceMethod<Unit, GetConnectionState.Response> {
        override val id = "GetConnectionState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            /** "logged_out" | "connecting" | "syncing" | "offline" */
            val state: String,
            val detail: String? = null,
            /** Rooms in the list cache so far — the Settings sync-progress line. */
            val roomsTotal: Int = 0,
            /** Rooms whose name/preview have been resolved in the background so far. */
            val roomsResolved: Int = 0,
        )
    }

    // --- E2EE methods (additive; Trixnity runs the crypto in the companion) ----

    /**
     * E2EE status of the current session: whether this device is cross-signing
     * verified (so messages can decrypt) and whether there are other devices on
     * the account to verify against.
     */
    object GetE2eeState : LightServiceMethod<Unit, GetE2eeState.Response> {
        override val id = "GetE2eeState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val verified: Boolean,
            val canVerify: Boolean,
            val detail: String? = null,
        )
    }

    /** Starts interactive (SAS/emoji) verification with the account's other devices. */
    object StartDeviceVerification : LightServiceMethod<Unit, StartDeviceVerification.Response> {
        override val id = "StartDeviceVerification"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val started: Boolean,
            val error: String? = null,
        )
    }

    /** The tool polls this while a verification is in progress. */
    object GetVerificationState : LightServiceMethod<Unit, GetVerificationState.Response> {
        override val id = "GetVerificationState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            /**
             * "none" | "waiting" (awaiting the other device) | "accept" (their
             * request/SAS — Accept) | "start" (Ready — Start) | "compare" (emoji
             * comparison) | "done" | "cancelled" | "error".
             */
            val state: String,
            /** The SAS emoji set to compare, when [state] == "compare". */
            val emoji: List<String>? = null,
            val detail: String? = null,
        )
    }

    /** Drives the interactive verification: "accept" | "start" | "match" | "no_match" | "cancel" | "reset". */
    object VerifyAction : LightServiceMethod<VerifyAction.Request, VerifyAction.Response> {
        override val id = "VerifyAction"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val action: String)

        @Serializable
        data class Response(val ok: Boolean, val error: String? = null)
    }

    /**
     * Tells the companion which room the tool is currently showing, so the
     * sync loop can suppress new-message notifications for it. null = no room
     * on screen (list/settings/background). Notifications for the active room
     * are also cancelled when it is set, since opening a thread marks it read.
     */
    object SetActiveRoom : LightServiceMethod<SetActiveRoom.Request, Unit> {
        override val id = "SetActiveRoom"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String? = null)
    }

    /**
     * One-shot "which room should the tool open?" — the companion sets this
     * when it posts a new-message notification (the tap target is the tool's
     * main activity, which cannot read intent extras), and returns + clears it
     * on this call. null = no pending room.
     */
    object TakeNotifyRoom : LightServiceMethod<Unit, TakeNotifyRoom.Response> {
        override val id = "TakeNotifyRoom"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val roomId: String? = null)
    }
}

val allMethods: Map<String, LightServiceMethod<*, *>> = listOf(
    LightServiceMethod.GetToken,
    LightServiceMethod.GetVersion,
    LightServiceMethod.SetRingtone,
    LightServiceMethod.GetKeyboardOptions,
    LightServiceMethod.GetPermission,
    LightServiceMethod.RequestPermissionComponent,
    LightServiceMethod.DeviceKeyEvent,
    LightServiceMethod.GetUserPreferences,
    LightServiceMethod.OpenDialer,
    LightServiceMethod.GetBooks,
    LightServiceMethod.ScanLibrary,
    LightServiceMethod.DeleteBook,
    LightServiceMethod.SeekToPart,
    LightServiceMethod.GetAutoPlayNext,
    LightServiceMethod.SetAutoPlayNext,
    LightServiceMethod.PlayBook,
    LightServiceMethod.OpenBook,
    LightServiceMethod.PausePlayback,
    LightServiceMethod.SeekTo,
    LightServiceMethod.SetPlaybackSpeed,
    LightServiceMethod.GetPlaybackState,
    LightServiceMethod.GetPasses,
    LightServiceMethod.AddPass,
    LightServiceMethod.UpdatePass,
    LightServiceMethod.DeletePass,
    LightServiceMethod.GetBarcode,
    LightServiceMethod.ChatPing,
    LightServiceMethod.SetAccount,
    LightServiceMethod.BeeperRequestCode,
    LightServiceMethod.SetBeeperAccount,
    LightServiceMethod.GetAccountState,
    LightServiceMethod.Logout,
    LightServiceMethod.GetRooms,
    LightServiceMethod.GetMessages,
    LightServiceMethod.SendMessage,
    LightServiceMethod.MarkRead,
    LightServiceMethod.SetTyping,
    LightServiceMethod.GetConnectionState,
    LightServiceMethod.GetE2eeState,
    LightServiceMethod.StartDeviceVerification,
    LightServiceMethod.GetVerificationState,
    LightServiceMethod.VerifyAction,
    LightServiceMethod.SetActiveRoom,
    LightServiceMethod.TakeNotifyRoom,
).associateBy { it.id }
