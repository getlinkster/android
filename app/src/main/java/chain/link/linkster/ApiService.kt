package chain.link.linkster


import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/v1/credentials/links/callback")
    fun createAuthLinkQRCode(
        @Query("sessionID") sessionID: String,
        @Query("linkID") linkID: String
    ): Call<QRCodeResponse>
}

data class QRCodeResponse(
    val qrCodeLink: String
)
