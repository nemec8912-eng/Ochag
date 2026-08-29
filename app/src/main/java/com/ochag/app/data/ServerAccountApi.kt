package com.ochag.app.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

import org.json.JSONArray
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import java.net.URLEncoder

import java.io.IOException
import java.util.concurrent.TimeUnit


data class ServerAccount(
    val userId: Long,
    val login: String,
    val name: String,
    val isOwner: Boolean,
    val familyId: String,
    val avatarUrl: String = "",
    val phone: String = "",
    val normalizedPhone: String = ""
)


data class ServerAccountResult(
    val success: Boolean,
    val account: ServerAccount? = null,
    val error: String = ""
)


data class ServerSimpleResult(
    val success: Boolean,
    val error: String = ""
)

data class ServerJoinRequest(
    val requestId: Long,
    val login: String,
    val name: String,
    val inviteCode: String
)

data class ServerJoinRequestsResult(
    val success: Boolean,
    val requests: List<ServerJoinRequest> = emptyList(),
    val error: String = ""
)


object ServerAccountApi {

    private const val SERVER_IP =
        "192.168.1.21"

    private const val SERVER_PORT =
        8000

    private val jsonMediaType =
        "application/json; charset=utf-8"
            .toMediaType()

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )
            .readTimeout(
                20,
                TimeUnit.SECONDS
            )
            .build()


    fun register(
        login: String,
        name: String,
        pin: String,
        isOwner: Boolean = false,
        familyId: String = "",
        onResult: (ServerAccountResult) -> Unit
    ) {

        registerWithPhone(
            login =
                login,
            name =
                name,
            pin =
                pin,
            phone =
                "",
            isOwner =
                isOwner,
            familyId =
                familyId,
            onResult =
                onResult
        )
    }


    fun registerWithPhone(
        login: String,
        name: String,
        pin: String,
        phone: String,
        isOwner: Boolean = false,
        familyId: String = "",
        onResult: (ServerAccountResult) -> Unit
    ) {

        val bodyJson =
            JSONObject()

        bodyJson.put(
            "login",
            login.trim()
        )

        bodyJson.put(
            "name",
            name.trim()
        )

        bodyJson.put(
            "pin",
            pin.trim()
        )

        bodyJson.put(
            "phone",
            phone.trim()
        )

        bodyJson.put(
            "is_owner",
            isOwner
        )

        bodyJson.put(
            "family_id",
            familyId.trim()
        )

        postAccountRequest(
            path =
                "/auth/register",
            bodyJson =
                bodyJson,
            onResult =
                onResult
        )
    }


    fun login(
        login: String,
        pin: String,
        onResult: (ServerAccountResult) -> Unit
    ) {

        val bodyJson =
            JSONObject()

        bodyJson.put(
            "login",
            login.trim()
        )

        bodyJson.put(
            "pin",
            pin.trim()
        )

        postAccountRequest(
            path =
                "/auth/login",
            bodyJson =
                bodyJson,
            onResult =
                onResult
        )
    }


    fun updatePhone(
        userName: String,
        phone: String,
        onResult: (ServerSimpleResult) -> Unit
    ) {

        val bodyJson =
            JSONObject()

        bodyJson.put(
            "user_name",
            userName.trim()
        )

        bodyJson.put(
            "phone",
            phone.trim()
        )

        val request =
            Request
                .Builder()
                .url(
                    "http://$SERVER_IP:$SERVER_PORT/profile/phone"
                )
                .post(
                    bodyJson
                        .toString()
                        .toRequestBody(
                            jsonMediaType
                        )
                )
                .build()

        client
            .newCall(
                request
            )
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        onResult(
                            ServerSimpleResult(
                                success =
                                    false,
                                error =
                                    e.message
                                        ?: "Нет соединения с сервером"
                            )
                        )
                    }


                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        response.use {

                            val raw =
                                response
                                    .body
                                    ?.string()
                                    .orEmpty()

                            if (
                                !response.isSuccessful
                            ) {

                                val error =
                                    try {

                                        JSONObject(
                                            raw
                                        )
                                            .optString(
                                                "detail",
                                                "Ошибка сервера"
                                            )

                                    } catch (_: Exception) {

                                        "Ошибка сервера: ${response.code}"
                                    }

                                onResult(
                                    ServerSimpleResult(
                                        success =
                                            false,
                                        error =
                                            error
                                    )
                                )

                                return
                            }

                            onResult(
                                ServerSimpleResult(
                                    success =
                                        true
                                )
                            )
                        }
                    }
                }
            )
    }



    fun submitJoinRequest(
        login: String,
        name: String,
        pin: String,
        inviteCode: String,
        onResult: (ServerSimpleResult) -> Unit
    ) {
        val json = JSONObject()
            .put("login", login.trim())
            .put("name", name.trim())
            .put("pin", pin.trim())
            .put("invite_code", inviteCode.trim())

        simpleRequest(
            path = "/family/join-requests",
            method = "POST",
            bodyJson = json,
            onResult = onResult
        )
    }

    fun loadJoinRequests(
        ownerName: String,
        onResult: (ServerJoinRequestsResult) -> Unit
    ) {
        val encoded = URLEncoder.encode(ownerName.trim(), "UTF-8")
        val request = Request.Builder()
            .url("http://$SERVER_IP:$SERVER_PORT/family/join-requests?owner_name=$encoded")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliver {
                    onResult(ServerJoinRequestsResult(false, error = e.message ?: "Нет соединения с сервером"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        deliver { onResult(ServerJoinRequestsResult(false, error = responseError(raw, response.code))) }
                        return
                    }
                    try {
                        val array = JSONObject(raw).optJSONArray("requests") ?: JSONArray()
                        val requests = buildList {
                            for (index in 0 until array.length()) {
                                val item = array.optJSONObject(index) ?: continue
                                add(
                                    ServerJoinRequest(
                                        requestId = item.optLong("request_id"),
                                        login = item.optString("login"),
                                        name = item.optString("name"),
                                        inviteCode = item.optString("invite_code")
                                    )
                                )
                            }
                        }
                        deliver { onResult(ServerJoinRequestsResult(true, requests)) }
                    } catch (error: Exception) {
                        deliver { onResult(ServerJoinRequestsResult(false, error = error.message ?: "Ошибка ответа сервера")) }
                    }
                }
            }
        })
    }

    fun approveJoinRequest(
        requestId: Long,
        ownerName: String,
        onResult: (ServerSimpleResult) -> Unit
    ) = decideJoinRequest(requestId, ownerName, "approve", onResult)

    fun declineJoinRequest(
        requestId: Long,
        ownerName: String,
        onResult: (ServerSimpleResult) -> Unit
    ) = decideJoinRequest(requestId, ownerName, "decline", onResult)

    private fun decideJoinRequest(
        requestId: Long,
        ownerName: String,
        action: String,
        onResult: (ServerSimpleResult) -> Unit
    ) {
        simpleRequest(
            path = "/family/join-requests/$requestId/$action",
            method = "POST",
            bodyJson = JSONObject().put("owner_name", ownerName.trim()),
            onResult = onResult
        )
    }

    private fun simpleRequest(
        path: String,
        method: String,
        bodyJson: JSONObject,
        onResult: (ServerSimpleResult) -> Unit
    ) {
        val body = bodyJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("http://$SERVER_IP:$SERVER_PORT$path")
            .method(method, body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                deliver { onResult(ServerSimpleResult(false, e.message ?: "Нет соединения с сервером")) }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val raw = response.body?.string().orEmpty()
                    deliver {
                        if (response.isSuccessful) {
                            onResult(ServerSimpleResult(true))
                        } else {
                            onResult(ServerSimpleResult(false, responseError(raw, response.code)))
                        }
                    }
                }
            }
        })
    }

    private fun responseError(raw: String, code: Int): String =
        try {
            JSONObject(raw).optString("detail", "Ошибка сервера")
        } catch (_: Exception) {
            "Ошибка сервера: $code"
        }

    private fun deliver(block: () -> Unit) {
        Handler(Looper.getMainLooper()).post(block)
    }


    private fun postAccountRequest(
        path: String,
        bodyJson: JSONObject,
        onResult: (ServerAccountResult) -> Unit
    ) {

        val request =
            Request
                .Builder()
                .url(
                    "http://$SERVER_IP:$SERVER_PORT$path"
                )
                .post(
                    bodyJson
                        .toString()
                        .toRequestBody(
                            jsonMediaType
                        )
                )
                .build()

        client
            .newCall(
                request
            )
            .enqueue(
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        onResult(
                            ServerAccountResult(
                                success =
                                    false,
                                error =
                                    e.message
                                        ?: "Нет соединения с сервером"
                            )
                        )
                    }


                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        response.use {

                            val raw =
                                response
                                    .body
                                    ?.string()
                                    .orEmpty()

                            if (
                                !response.isSuccessful
                            ) {

                                val error =
                                    try {

                                        JSONObject(
                                            raw
                                        )
                                            .optString(
                                                "detail",
                                                "Ошибка сервера"
                                            )

                                    } catch (_: Exception) {

                                        "Ошибка сервера: ${response.code}"
                                    }

                                onResult(
                                    ServerAccountResult(
                                        success =
                                            false,
                                        error =
                                            error
                                    )
                                )

                                return
                            }

                            try {

                                val json =
                                    JSONObject(
                                        raw
                                    )

                                val user =
                                    json
                                        .optJSONObject(
                                            "user"
                                        )

                                if (
                                    user == null
                                ) {

                                    onResult(
                                        ServerAccountResult(
                                            success =
                                                false,
                                            error =
                                                "Сервер не вернул профиль"
                                        )
                                    )

                                    return
                                }

                                onResult(
                                    ServerAccountResult(
                                        success =
                                            true,
                                        account =
                                            ServerAccount(
                                                userId =
                                                    user.optLong(
                                                        "user_id",
                                                        0L
                                                    ),
                                                login =
                                                    user.optString(
                                                        "login"
                                                    ),
                                                name =
                                                    user.optString(
                                                        "name"
                                                    ),
                                                isOwner =
                                                    user.optBoolean(
                                                        "is_owner",
                                                        false
                                                    ),
                                                familyId =
                                                    user.optString(
                                                        "family_id"
                                                    ),
                                                avatarUrl =
                                                    user.optString(
                                                        "avatar_url"
                                                    ),
                                                phone =
                                                    user.optString(
                                                        "phone"
                                                    ),
                                                normalizedPhone =
                                                    user.optString(
                                                        "phone_normalized"
                                                    )
                                            )
                                    )
                                )

                            } catch (
                                error: Exception
                            ) {

                                onResult(
                                    ServerAccountResult(
                                        success =
                                            false,
                                        error =
                                            error.message
                                                ?: "Ошибка ответа сервера"
                                    )
                                )
                            }
                        }
                    }
                }
            )
    }
}
