package chain.link.linkster


import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("/v1/credentials/links/callback")
    fun createAuthLinkQRCode(
        @Query("sessionID") sessionID: String,
        @Query("linkID") linkID: String
    ): Call<QRCodeResponse>

    @GET("/v1/schemas")
    @Headers("accept: application/json")
    fun getSchemas(): Call<List<SchemaResponse>>

    @POST("/v1/credentials")
    @Headers("accept: application/json")
    fun creatUserCredential(
        @Body request: CreateUserCredentialRequest
    ): Call<CredentialResponse>

    @GET("/v1/credentials/{credentialId}/qrcode")
    @Headers("accept: application/json")
    fun getQRCodeForCredential(
        @Path("credentialId") credentialId: String
    ): Call<QRCodeResponse>

    @POST("/api/v1/create/profile")
    @Headers("accept: application/json")
    fun createProfile(
        @Body request: CreateProfileRequest
    ): Call<QRCodeResponse>

    @POST("/api/v1/create/event")
    @Headers("accept: application/json")
    fun createEvent(
        @Body request: CreateEventRequest
    ): Call<QRCodeResponse>

}

data class CreateEventRequest(
    val event: EventData
)
data class EventData(
    @SerializedName("event-name") val eventName: String,
    @SerializedName("event-date") val eventDate: String,
    @SerializedName("event-location") val eventLocation: String,
    @SerializedName("additional-info") val additionalInfo: String
)

data class CreateProfileRequest(
    val name: String,
    val wallet: String,
    val profession: String,
    val company: String,
    val telegram: String
)

data class QRCodeResponse(
    val qrCodeLink: String
)

data class CreateUserCredentialRequest(
    val credentialSchema: String,
    val type: String,
    val credentialSubject: CredentialUserSubject,
    val expiration: String,
    val signatureProof: Boolean,
    val mtProof: Boolean
)

data class CredentialUserSubject(
    val name: String,
    val wallet: String,
    val profession: String
)

data class CredentialResponse(
    val id: String
)

data class SchemaResponse(
    val bigInt: String,
    val createdAt: String,
    val description: String?,
    val hash: String,
    val id: String,
    val title: String?,
    val type: String,
    val url: String,
    val version: String
)
data class ApiError(
    val message: String?,
    val code: Int?
)