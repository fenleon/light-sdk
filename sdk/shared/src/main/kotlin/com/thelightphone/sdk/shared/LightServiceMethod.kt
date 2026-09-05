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

    // LightOS's mollysocket push endpoint for this device (beta). The emulator
    // serves a configurable fake; real LightOS answers per-device.
    object GetMollySocketUri : LightServiceMethod<Unit, GetMollySocketUri.Response> {
        override val id = "GetMollySocketUri"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val mollySocketUri: String)
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
        data class Chapter(
            val title: String,
            /** Chapter start, offset within its part. */
            val startMs: Long,
            /** Chapter end within its part; open-ended chapters are resolved to the next chapter's start or the part duration. */
            val endMs: Long,
        )

        @Serializable
        data class Part(
            val title: String,
            val durationMs: Long,
            /** Playback reference for this part (a content URI the player can open). */
            val playbackReference: String = "",
            /** Embedded chapters (MP3 CHAP frames / M4B bookmarks) within this part. Default empty — backward compatible. */
            val chapters: List<Chapter> = emptyList(),
        )

        @Serializable
        data class Book(
            val id: String,
            val title: String,
            val author: String,
            val durationMs: Long,
            val progressMs: Long,
            val partCount: Int,
            /** Playback reference for single-file books (folder books reference their parts). */
            val playbackReference: String = "",
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

    /** The global playback speed, persisted by the companion. */
    object GetPlaybackSpeed : LightServiceMethod<Unit, GetPlaybackSpeed.Response> {
        override val id = "GetPlaybackSpeed"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val speed: Float)
    }

    object SetPlaybackSpeed : LightServiceMethod<SetPlaybackSpeed.Request, Unit> {
        override val id = "SetPlaybackSpeed"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val speed: Float)
    }

    /** Whether a Bluetooth audio device is connected (drives the connected-BT icon). */
    object GetBluetoothConnected : LightServiceMethod<Unit, GetBluetoothConnected.Response> {
        override val id = "GetBluetoothConnected"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val connected: Boolean)
    }

    /**
     * Blocks until the media-stream volume changes (or [WAIT_TIMEOUT_MS]
     * elapses), then reports the current level — a long-poll so the volume
     * panel can react instantly to a BT device's own volume buttons (AVRCP)
     * instead of polling. Returns immediately when [Request.knownLevel]
     * already differs from the current level.
     */
    object WaitForVolumeChange : LightServiceMethod<WaitForVolumeChange.Request, WaitForVolumeChange.Response> {
        override val id = "WaitForVolumeChange"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val knownLevel: Int)

        @Serializable
        data class Response(val level: Int, val max: Int)

        const val WAIT_TIMEOUT_MS = 2_000L
    }

    /** The current media-stream volume level and its maximum (the LP3's media stream is 0–14). */
    object GetVolumeLevel : LightServiceMethod<Unit, GetVolumeLevel.Response> {
        override val id = "GetVolumeLevel"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            val level: Int,
            val max: Int,
        )
    }

    /** Reports a book's listening position to the companion for persistence. */
    object SaveProgress : LightServiceMethod<SaveProgress.Request, Unit> {
        override val id = "SaveProgress"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val bookId: String,
            val positionMs: Long = 0,
            val durationMs: Long = 0,
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
        data class Code(
            val id: String,
            val data: String,
            /** Base64-encoded raw (binary) payload, when the code carries one. */
            val rawData: String? = null,
            val type: String,
            /** True when the code was typed manually — only typed codes show
             *  their text under the barcode (scanned payloads are noise). */
            val typed: Boolean = false,
        )

        @Serializable
        data class Pass(
            val id: String,
            val name: String,
            /** The pass's stacked codes, in order (a pass is a name + details
             *  shared by all its codes). */
            val codes: List<Code>,
            /** Optional detail fields — only filled ones are shown in the tool. */
            val issuer: String? = null,
            val date: String? = null,
            val endDate: String? = null,
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

        /** Creates a new pass (name + details to come) with its first code. */
        @Serializable
        data class Request(
            val name: String,
            val data: String,
            val rawData: String? = null,
            val type: String,
            val typed: Boolean = false,
        )
    }

    /** Adds another code to an existing pass — the barcode panel's "+" flow. */
    object AddCode : LightServiceMethod<AddCode.Request, Unit> {
        override val id = "AddCode"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(
            val passId: String,
            val data: String,
            val rawData: String? = null,
            val type: String,
            val typed: Boolean = false,
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
            val endDate: String? = null,
            val startTime: String? = null,
            val endTime: String? = null,
            val location: String? = null,
            val notes: String? = null,
        )
    }

    /** Deletes one stacked code from its pass (the last code removes the pass). */
    object DeleteCode : LightServiceMethod<DeleteCode.Request, Unit> {
        override val id = "DeleteCode"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val codeId: String)
    }

    /** Renders a pass code's barcode in the companion; the PNG bytes cross the binder as base64. */
    object GetBarcode : LightServiceMethod<GetBarcode.Request, GetBarcode.Response> {
        override val id = "GetBarcode"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            /** The code's id (a pass can hold several stacked codes). */
            val codeId: String,
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
            val needsVerification: Boolean = false,
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
            val needsVerification: Boolean = false,
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
            /** Direct (1:1) chat — the thread can hide per-message sender names. */
            val isDirect: Boolean = false,
            /**
             * The room's other participant for 1:1s — the single non-bot hero
             * (Beeper bridged DMs list the contact; bridge bots like
             * @whatsappbot are excluded). Null for groups. Drives the contact
             * overlay's phone/username line (chats, feedback 2026-08-21).
             */
            val contactId: String? = null,
            /**
             * The contact's phone number for 1:1s, when the room data carries
             * one: a @whatsapp_<number> ghost, or the number the bridge used
             * as the displayname before syncing the profile name (the LID
             * ghost's member event prev_content). Null otherwise — not every
             * contact exposes the number in room data (chats, feedback
             * 2026-08-23).
             */
            val contactPhone: String? = null,
            /**
             * Bridged network label ("WhatsApp", "Instagram", …), derived by the
             * companion from Beeper's per-network spaces. Null = not in any
             * space (Beeper-internal rooms, user-created spaces stay ungrouped).
             */
            val network: String? = null,
            /**
             * For rooms inside a bridged community (Beeper sub-spaces under an
             * account space — WhatsApp community groups): the community's own
             * name ("1 euro film"). Null otherwise. Groups carry the community
             * where 1:1s carry [contactId] (chats, feedback 2026-09-01).
             */
            val community: String? = null,
            /**
             * The user muted this room in the tool (chats): its messages stop
             * notifying; the unread badge and room list stay. Set via
             * [SetRoomMuted].
             */
            val muted: Boolean = false,
            /**
             * The user archived this room on Beeper: hidden from the main room list,
             * silent, reachable only via search VIEW ALL.
             */
            val archived: Boolean = false,
            /**
             * The user pinned this room (m.favourite tag): sorted to the top of the
             * room list; its row shows no latest timestamp.
             */
            val pinned: Boolean = false,
        )

        @Serializable
        data class Response(val rooms: List<Room>)
    }

    /**
     * The full room census — every room the resolver knows, trimmed rows (no
     * preview/unread/last event) so the whole account crosses one binder
     * transaction. The contacts list + search need to find ANY room, not just
     * the newest window [GetRooms] serves (chats 2026-08-30).
     */
    object GetAllRooms : LightServiceMethod<Unit, GetRooms.Response> {
        override val id = "GetAllRooms"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<GetRooms.Response>()
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
            /**
             * Beeper send status for an outgoing message ("PENDING" /
             * "FAIL_RETRIABLE" / …), resolved by the companion from the room's
             * `com.beeper.message_send_status` events. Null = no status event
             * (not ours, or delivered without a status report).
             */
            val sendStatus: String? = null,
            /**
             * How the tool renders the row: "text" | "image" (Chats has since
             * added "audio", "notice", and — Phase C, 2026-09-03 —
             * "redacted", the "Message unsent" tombstone). Images carry
             * their bytes over [GetMessageMedia] (keyed by room + event id).
             */
            val contentType: String = "text",
            /**
             * Whether the other party has read this outgoing message (the
             * room's m.read receipts). Only meaningful on the newest page —
             * older pages always report false (receipts describe the newest
             * events, and re-fetching them per page isn't worth it).
             */
            val read: Boolean = false,
            /**
             * Distinct reaction emoji on this message (the room's m.reaction
             * events), newest-page scope — older pages report empty. The tool
             * renders them as a small tag under the message.
             */
            val reactions: List<String> = emptyList(),
            /**
             * Voice-note length in milliseconds (the m.audio event's info
             * duration). Null for non-audio rows; the tool shows it on the row
             * and counts up to it while the note plays.
             */
            val durationMs: Long? = null,
            /**
             * Caption of an image message (the m.image event's body — most
             * clients put the caption there; the file name is a separate
             * field). Null for non-image rows and caption-less photos. The
             * tool renders it under the thumbnail.
             */
            val caption: String? = null,
            /**
             * Whether an m.replace edit updated this message (chats): the
             * companion serves the EDITED body in [body] and this flag makes
             * the tool render an "edited" tag under the row.
             */
            val edited: Boolean = false,
            /**
             * Whether the bridge flagged this message as forwarded (chats): the
             * WhatsApp forward header ("↷ Forwarded") is lifted out of [body] /
             * [caption], and this flag makes the tool render a small header
             * above the row's content.
             */
            val forwarded: Boolean = false,
            /**
             * Whether EDIT is offered on this own row (chats Phase C,
             * 2026-09-03): the bridge's `com.beeper.room_features` edit
             * capability (support level ≥ 1) AND inside its `edit_max_age`
             * window when one is set AND the row is text (the tool's edit is
             * text-only). False hides the action; the default keeps native
             * Matrix rooms and caps-less bridges ungated.
             */
            val canEdit: Boolean = true,
            /**
             * Whether UNSEND is offered on this own row (same gate as
             * [canEdit], the bridge's delete capability / `delete_max_age`).
             */
            val canUnsend: Boolean = true,
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
            /**
             * Event id of the voice note currently playing in the companion
             * (null = nothing playing). The thread poll carries it so the tool
             * can highlight the row that's playing without an extra RPC.
             */
            val audioPlayingEventId: String? = null,
            /**
             * Playback position (ms) of the currently playing voice note
             * ([audioPlayingEventId]); null when nothing is playing. The tool
             * interpolates between polls so the counter runs smoothly.
             */
            val audioPositionMs: Long? = null,
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

        /**
         * The outbox transaction id; the message may still be pending delivery.
         * [eventId] is the timeline event id once the homeserver acked the send
         * (read back from the outbox after the ack); null if it isn't known yet
         * — the tool uses it for an optimistic thread row that the sync echo
         * replaces.
         */
        @Serializable
        data class Response(
            val transactionId: String,
            val eventId: String? = null,
        )
    }

    object MarkRead : LightServiceMethod<MarkRead.Request, Unit> {
        override val id = "MarkRead"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val eventId: String)
    }

    /**
     * Re-sends a message that failed to leave the device: clears the outbox
     * error on [Request.transactionId], so Trixnity re-sends the same
     * transaction (idempotent — no duplicate event if it had reached the
     * homeserver). The tool offers this on rows marked locally failed.
     */
    object RetrySend : LightServiceMethod<RetrySend.Request, Unit> {
        override val id = "RetrySend"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val transactionId: String)
    }

    /**
     * Sends a reaction (an m.reaction annotation) with [Request.key] (an
     * emoji) on [Request.eventId] (chats, 2026-09-03). Unsending is
     * [UnsendReaction].
     */
    object SendReaction : LightServiceMethod<SendReaction.Request, Unit> {
        override val id = "SendReaction"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val eventId: String, val key: String)
    }

    /** Redacts the signed-in user's [SendReaction] with [Request.key] on
     *  [Request.eventId] (chats, 2026-09-03). */
    object UnsendReaction : LightServiceMethod<UnsendReaction.Request, Unit> {
        override val id = "UnsendReaction"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val eventId: String, val key: String)
    }

    /**
     * Edits the signed-in user's text message [Request.eventId] to
     * [Request.newBody] — an m.replace edit event (chats, 2026-09-03). The
     * companion serves the edited body back through [GetMessages.Message].
     */
    object EditMessage : LightServiceMethod<EditMessage.Request, Unit> {
        override val id = "EditMessage"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val eventId: String, val newBody: String)
    }

    /**
     * Unsends the signed-in user's message [Request.eventId] — a plain Matrix
     * redaction, so the message is removed for everyone the bridge can reach
     * (chats, 2026-09-03). The row renders as a "redacted" tombstone after.
     */
    object UnsendMessage : LightServiceMethod<UnsendMessage.Request, Unit> {
        override val id = "UnsendMessage"
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

    /** Mutes or unmutes a room's notifications (chats, 2026-08-23). */
    object SetRoomMuted : LightServiceMethod<SetRoomMuted.Request, Unit> {
        override val id = "SetRoomMuted"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val muted: Boolean)
    }

    /** Pins or unpins a room (m.favourite tag, synced, chats 2026-08-28). */
    object SetRoomPinned : LightServiceMethod<SetRoomPinned.Request, Unit> {
        override val id = "SetRoomPinned"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val pinned: Boolean)
    }

    /**
     * Snapshot of a room's pinned/muted/archived flags (chats, 2026-08-28):
     * the contact panel polls this while open so Beeper-side changes reach it
     * live (the flags live in the companion's synced cache).
     */
    object GetRoomFlags : LightServiceMethod<GetRoomFlags.Request, GetRoomFlags.Response> {
        override val id = "GetRoomFlags"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String)

        @Serializable
        data class Response(
            val pinned: Boolean = false,
            val muted: Boolean = false,
            val archived: Boolean = false,
        )
    }

    /** Archives or unarchives a room (Beeper auto_archive account data, synced, chats 2026-08-28). */
    object SetRoomArchived : LightServiceMethod<SetRoomArchived.Request, Unit> {
        override val id = "SetRoomArchived"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val roomId: String, val archived: Boolean)
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
            /** Whether the companion's sync loop is running (Settings → Sync toggle). */
            val syncEnabled: Boolean = true,
            /** Background key-backup restore crawl in progress (Account screen's
             *  "Recovering… x of y rooms" line, 2026-08-29). */
            val restoreScanning: Boolean = false,
            val restoreScanned: Int = 0,
            val restoreRoomsTotal: Int = 0,
            /** True when the crawl ran to completion; the Account screen pairs it
             *  with [syncEnabled] to show "All messages restored". */
            val restoreCompleted: Boolean = false,
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
            /** The device that accepted the verification request, when known. */
            val deviceId: String? = null,
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
     * Verifies this device non-interactively using the account's recovery key.
     * Beeper's interactive verification is unreliable (its own guidance says
     * to use a recovery code instead), so this is the dependable unlock for
     * E2EE on a new device.
     */
    object RecoverWithKey : LightServiceMethod<RecoverWithKey.Request, RecoverWithKey.Response> {
        override val id = "RecoverWithKey"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val recoveryKey: String)

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

    /**
     * Starts the "attach a photo" flow for [Request.roomId]: the companion
     * records the room and returns the flattened component name of its photo
     * picker activity. The tool launches it via
     * `SimpleLightScreen.startServerActivity` (the tool runtime forbids
     * startActivity; the companion can't launch activities from the
     * background). The activity shows the system photo picker, then uploads
     * and sends the chosen photo in the room itself; the tool's thread poll
     * picks up the resulting image message.
     */
    object StartPhotoSend : LightServiceMethod<StartPhotoSend.Request, StartPhotoSend.Response> {
        override val id = "StartPhotoSend"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String)

        @Serializable
        data class Response(val activityComponent: String)
    }

    /**
     * Display-ready JPEG bytes for an image message (the companion downloads
     * the media — decrypting when the room is encrypted — and compresses it
     * so the binder transaction stays small). Null/empty when the media can't
     * be fetched (e.g. still-encrypted); the tool falls back to the row's
     * text body.
     */
    object GetMessageMedia : LightServiceMethod<GetMessageMedia.Request, GetMessageMedia.Response> {
        override val id = "GetMessageMedia"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val roomId: String,
            val eventId: String,
            /**
             * Whether downloads are allowed on the mobile-data connection
             * (Settings → Mobile data downloads). When false the companion
             * skips the download while the active network is cellular — the
             * tool's image row keeps its text fallback until Wi-Fi or the
             * toggle flips. Defaults to false (the data-conscious default).
             */
            val allowMobileData: Boolean = false,
        )

        @Serializable
        data class Response(val bytes: ByteArray? = null)
    }

    /**
     * Saves an image message to the device's Pictures/Chats album (Chats
     * photo viewer save button, 2026-09-03). The companion re-fetches the
     * original bytes (media cache hit after a view) and inserts them into
     * the media store — app-contributed media needs no storage permission
     * on API 29+.
     */
    object SaveMessageImage : LightServiceMethod<SaveMessageImage.Request, SaveMessageImage.Response> {
        override val id = "SaveMessageImage"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String, val eventId: String)

        @Serializable
        data class Response(val ok: Boolean = true)
    }

    /**
     * Toggles playback of a voice-note (m.audio) message in the companion:
     * plays [Request.eventId] (stopping any other playback), or stops it if it
     * is already the one playing. The companion downloads the audio
     * (decrypting when the room is encrypted) and plays it with a plain
     * MediaPlayer — everything privileged lives server-side, like photos.
     */
    object PlayVoiceNote : LightServiceMethod<PlayVoiceNote.Request, PlayVoiceNote.Response> {
        override val id = "PlayVoiceNote"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String, val eventId: String)

        @Serializable
        data class Response(
            /** Whether the requested event is now playing (false = stopped). */
            val playing: Boolean,
            /** Human-readable failure, when the audio couldn't be fetched/played. */
            val error: String? = null,
        )
    }

    /**
     * Starts the send-a-voice-note flow: records the room and returns the
     * flattened component name of the companion's recording activity, which
     * the tool launches via `SimpleLightScreen.startServerActivity` (the tool
     * runtime forbids startActivity). The activity records an m4a and sends it
     * as an m.audio message in the room itself (same pattern as photos).
     */
    object StartVoiceNoteSend : LightServiceMethod<StartVoiceNoteSend.Request, StartVoiceNoteSend.Response> {
        override val id = "StartVoiceNoteSend"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String)

        @Serializable
        data class Response(val activityComponent: String)
    }

    /**
     * Pauses/resumes the companion's Matrix sync loop + foreground service
     * (Settings → Sync, audit 2026-08-14). Pausing stops all background sync
     * work — the escape hatch when sync is draining the battery or the account
     * is misbehaving; messages arrive again once it is re-enabled.
     */
    object SetSyncEnabled : LightServiceMethod<SetSyncEnabled.Request, SetSyncEnabled.Response> {
        override val id = "SetSyncEnabled"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val enabled: Boolean)

        @Serializable
        data class Response(val ok: Boolean)
    }

    /**
     * Monotonic revision of the served room list (chats, 2026-09-01): bumped
     * server-side every time the published list content changes. The tool's
     * periodic list refresh asks for this cheap number first and only fetches
     * the full [GetRooms] payload when it moved — idle polls stop crossing the
     * binder with the 400-room cap.
     */
    object GetRoomListRevision : LightServiceMethod<Unit, GetRoomListRevision.Response> {
        override val id = "GetRoomListRevision"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val revision: Long)
    }

    /**
     * Monotonic revision of a room's cached newest page (chats, 2026-09-01):
     * bumped whenever the page cache's content changes (new/edited events,
     * read-receipt patches, pending-echo state). The thread's 3s poll asks for
     * this first and skips the [GetMessages] round trip while the page is
     * unchanged.
     */
    object GetMessagePageRevision : LightServiceMethod<GetMessagePageRevision.Request, GetMessagePageRevision.Response> {
        override val id = "GetMessagePageRevision"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val roomId: String)

        @Serializable
        data class Response(val revision: Long)
    }

    /**
     * Long-poll wait for a change (chats, 2026-09-06): holds the binder call
     * until the watched revision moves past [Request.lastSeen] or
     * [Request.timeoutMs] elapses, then returns the current revision either
     * way. Replaces the tool screens' fixed-delay poll ticks (list 2 s /
     * thread 1.5 s) — an idle screen costs one held call per timeout window
     * instead of a wakeup every tick, and a change lands within milliseconds
     * instead of at the next tick. watch = "rooms" (the [GetRoomListRevision]
     * counter, roomId ignored) or "page" (the [GetMessagePageRevision] counter
     * for [Request.roomId]). Same model as [WaitForVolumeChange]; safe because
     * each sync binder call runs on its own pool thread.
     */
    object WaitForChange : LightServiceMethod<WaitForChange.Request, WaitForChange.Response> {
        override val id = "WaitForChange"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(
            val watch: String,
            val roomId: String? = null,
            val lastSeen: Long,
            val timeoutMs: Long,
        )

        @Serializable
        data class Response(val revision: Long)
    }

    // --- Passkey methods (local, additive addition for the Passkey companion;
    // upstreamable — production com.lightos should implement these for a real LP3).
    /**
     * Starts a caBLE v2 hybrid sign-in session in the companion from the
     * desktop's scanned QR payload (the FIDO:/… digits string): opens the
     * tunnel, advertises the EID, answers the KNpsk0 handshake, and dispatches
     * CTAP MakeCredential/GetAssertion frames through the authenticator. UV is
     * prompted via a companion UV activity when a frame requires it.
     */
    object StartPasskeySession : LightServiceMethod<StartPasskeySession.Request, StartPasskeySession.Response> {
        override val id = "StartPasskeySession"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Request(val qrPayload: String)

        @Serializable
        data class Response(val ok: Boolean, val error: String? = null)
    }

    object StopSession : LightServiceMethod<Unit, Unit> {
        override val id = "StopSession"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Unit>()
    }

    /** The passkey session's live state, for the tool's status view. */
    object GetSessionState : LightServiceMethod<Unit, GetSessionState.Response> {
        override val id = "GetSessionState"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(
            /** "idle" | "running" | "pick" | "done" | "error" */
            val state: String,
            /** Recent status lines, newest last (the scanner/status view). */
            val lines: List<String>,
            /** Short outcome line when the session ended ("Signed in", "Passkey created", …). */
            val summary: String? = null,
            /**
             * Candidate user names when [state] == "pick" — a GetAssertion
             * matched more than one credential for the RP and the tool must ask
             * which account to sign in as (PickAccount by index).
             */
            val candidates: List<String> = emptyList(),
        )
    }

    /** One passkey's metadata row, for the tool's list screen. */
    @Serializable
    data class CredentialInfo(
        /** Base64url credential id (also the Keystore alias suffix). */
        val credentialId: String,
        val rpId: String,
        val userName: String,
        /** Epoch millis; 0 = legacy row created before timestamps were stored. */
        val createdAt: Long,
        /** Epoch millis; 0 = never used for a sign-in yet. */
        val lastUsedAt: Long,
    )

    /** The stored passkeys, newest first, for the tool's list screen. */
    object ListPasskeys : LightServiceMethod<Unit, ListPasskeys.Response> {
        override val id = "ListPasskeys"
        override val requestSerializer = serializer<Unit>()
        override val responseSerializer = serializer<Response>()

        @Serializable
        data class Response(val credentials: List<CredentialInfo> = emptyList())
    }

    /** Deletes a stored passkey (Keystore key + metadata row). No-op when unknown. */
    object DeletePasskey : LightServiceMethod<DeletePasskey.Request, Unit> {
        override val id = "DeletePasskey"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val credentialId: String)
    }

    /**
     * Answers a pending account picker ("pick" session state): selects the
     * credential at [Request.index] of the state's candidates. -1 cancels the
     * GetAssertion (CTAP_ERR_OPERATION_DENIED). No-op when no pick is pending.
     */
    object PickAccount : LightServiceMethod<PickAccount.Request, Unit> {
        override val id = "PickAccount"
        override val requestSerializer = serializer<Request>()
        override val responseSerializer = serializer<Unit>()

        @Serializable
        data class Request(val index: Int)
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
    LightServiceMethod.GetAutoPlayNext,
    LightServiceMethod.SetAutoPlayNext,
    LightServiceMethod.GetPlaybackSpeed,
    LightServiceMethod.SetPlaybackSpeed,
    LightServiceMethod.GetBluetoothConnected,
    LightServiceMethod.WaitForVolumeChange,
    LightServiceMethod.GetVolumeLevel,
    LightServiceMethod.SaveProgress,
    LightServiceMethod.GetPasses,
    LightServiceMethod.AddPass,
    LightServiceMethod.AddCode,
    LightServiceMethod.UpdatePass,
    LightServiceMethod.DeleteCode,
    LightServiceMethod.GetBarcode,
    LightServiceMethod.ChatPing,
    LightServiceMethod.SetAccount,
    LightServiceMethod.BeeperRequestCode,
    LightServiceMethod.SetBeeperAccount,
    LightServiceMethod.GetAccountState,
    LightServiceMethod.Logout,
    LightServiceMethod.GetRooms,
    LightServiceMethod.GetAllRooms,
    LightServiceMethod.GetMessages,
    LightServiceMethod.SendMessage,
    LightServiceMethod.SendReaction,
    LightServiceMethod.UnsendReaction,
    LightServiceMethod.EditMessage,
    LightServiceMethod.UnsendMessage,
    LightServiceMethod.RetrySend,
    LightServiceMethod.MarkRead,
    LightServiceMethod.SetTyping,
    LightServiceMethod.SetRoomMuted,
    LightServiceMethod.SetRoomPinned,
    LightServiceMethod.GetRoomFlags,
    LightServiceMethod.SetRoomArchived,
    LightServiceMethod.GetConnectionState,
    LightServiceMethod.GetE2eeState,
    LightServiceMethod.StartDeviceVerification,
    LightServiceMethod.GetVerificationState,
    LightServiceMethod.VerifyAction,
    LightServiceMethod.RecoverWithKey,
    LightServiceMethod.SetActiveRoom,
    LightServiceMethod.TakeNotifyRoom,
    LightServiceMethod.StartPhotoSend,
    LightServiceMethod.GetMessageMedia,
    LightServiceMethod.SaveMessageImage,
    LightServiceMethod.PlayVoiceNote,
    LightServiceMethod.StartVoiceNoteSend,
    LightServiceMethod.SetSyncEnabled,
    LightServiceMethod.GetRoomListRevision,
    LightServiceMethod.GetMessagePageRevision,
    LightServiceMethod.GetMollySocketUri,
    LightServiceMethod.StartPasskeySession,
    LightServiceMethod.StopSession,
    LightServiceMethod.GetSessionState,
    LightServiceMethod.ListPasskeys,
    LightServiceMethod.DeletePasskey,
    LightServiceMethod.PickAccount,
).associateBy { it.id }
