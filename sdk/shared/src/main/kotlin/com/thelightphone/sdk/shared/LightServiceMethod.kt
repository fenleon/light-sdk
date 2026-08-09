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
    LightServiceMethod.PlayBook,
    LightServiceMethod.PausePlayback,
    LightServiceMethod.SeekTo,
    LightServiceMethod.SetPlaybackSpeed,
    LightServiceMethod.GetPlaybackState,
).associateBy { it.id }
