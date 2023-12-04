package chain.link.linkster.ui.events

import android.content.Context
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chain.link.linkster.RandomStringManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.common.domain.entities.EnvEntity
import technology.polygon.polygonid_android_sdk.credential.domain.entities.ClaimEntity
import technology.polygon.polygonid_android_sdk.iden3comm.domain.entities.Iden3MessageEntity
import java.math.BigInteger
import java.nio.charset.StandardCharsets

const val TAG = "PolygonIdSdk"
const val apiKey = "1861e8a1291f423298faf6a50c9c0a6d"
const val authMessage =
    "{\"id\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"body\":{\"callbackUrl\":\"https://self-hosted-demo-backend-platform.polygonid.me/api/callback?sessionId=98378\",\"reason\":\"test flow\",\"scope\":[]},\"from\":\"did:polygonid:polygon:mumbai:2qLhNLVmoQS7pQtpMeKHDqkTcENBZUj1nkZiRNPGgV\"}"
const val fetchMessage =
    "{\"id\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/credentials/1.0/offer\",\"thid\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"body\":{\"url\":\"https://issuer-testing.polygonid.me/v1/agent\",\"credentials\":[{\"id\":\"2bcb98bc-e8db-11ed-938b-0242ac180006\",\"description\":\"KYCAgeCredential\"}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\",\"to\":\"did:polygonid:polygon:mumbai:2qJGQxEf8n3XiT7fYbqaBdYCUCPQVgkK8rYKbRLTMe\"}"
const val credentialRequestMessage =
    "{\"id\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"body\":{\"callbackUrl\":\"https://self-hosted-testing-backend-platform.polygonid.me/api/callback?sessionId=174262\",\"reason\":\"test flow\",\"scope\":[{\"id\":1,\"circuitId\":\"credentialAtomicQuerySigV2\",\"query\":{\"allowedIssuers\":[\"*\"],\"context\":\"https://raw.githubusercontent.com/iden3/claim-schema-vocab/main/schemas/json-ld/kyc-v3.json-ld\",\"credentialSubject\":{\"birthday\":{\"\$lt\":20000101}},\"skipClaimRevocationCheck\":true,\"type\":\"KYCAgeCredential\"}}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\"}"

class EventsViewModel : ViewModel() {

    private val _authenticationStatus = MutableLiveData<Boolean>()
    val authenticationStatus: LiveData<Boolean> = _authenticationStatus

    val fetchStatus = MutableLiveData<Boolean>()

    private val _text = MutableLiveData<String>().apply {
        value = "This is events Fragment"
    }
    val text: LiveData<String> = _text
    val eventClaimsLiveData: MutableLiveData<List<ClaimEntity>> = MutableLiveData()
    val secret = RandomStringManager.randomString

    fun init(context: Context) {
        val mumbai = EnvEntity(
            blockchain = "polygon",
            network = "mumbai",
            web3Url = "https://polygon-mumbai.infura.io/v3/",
            web3RdpUrl = "wss://polygon-mumbai.infura.io/v3/",
            web3ApiKey = apiKey,
            idStateContract = "0x134B1BE34911E39A8397ec6289782989729807a4",
            pushUrl = "https://push-staging.polygonid.com/api/v1"
        )
        viewModelScope.launch {
            PolygonIdSdk.init(
                context = context,
                env = mumbai,
            )
            PolygonIdSdk.getInstance().switchLog(context = context, true).thenAccept {
                println("SwitchOnLog Done")
            }
        }
    }

    fun authenticate(context: Context, authMessage: String) {
        viewModelScope.launch {

            var rawMessage = authMessage
            if (authMessage.startsWith("iden3comm://?i_m")) {
                rawMessage = getMessageFromBase64(authMessage)
            }

            if (authMessage.startsWith("iden3comm://?request_uri")) {
                rawMessage = getMessageFromRemote(authMessage)
            }

            PolygonIdSdk.getInstance().getIden3Message(
                context, rawMessage
            ).thenApply { message ->
                PolygonIdSdk.getInstance().getPrivateKey(
                    context = context, secret = secret
                ).thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getDidIdentifier(
                        context = context,
                        privateKey = privateKey,
                        blockchain = "polygon",
                        network = "mumbai",
                    ).thenApply { did ->
                        PolygonIdSdk.getInstance().authenticate(
                            context = context,
                            message = message as Iden3MessageEntity.AuthIden3MessageEntity,
                            genesisDid = did,
                            privateKey = privateKey
                        ).thenAccept {
                            println("Authenticated")
                            updateAuthenticationStatus(true)
                        }.exceptionally {
                            println("Authentication Error: $it")
                            updateAuthenticationStatus(false)
                            null
                        }
                    }
                }
            }
        }
    }
    fun fetch(context: Context, fetchMessage: String) {
        viewModelScope.launch {

            var rawMessage = fetchMessage
            if (fetchMessage.startsWith("iden3comm://?i_m")) {
                rawMessage = getMessageFromBase64(fetchMessage)
            }

            if (fetchMessage.startsWith("iden3comm://?request_uri")) {
                rawMessage = getMessageFromRemote(fetchMessage)
            }

            PolygonIdSdk.getInstance().getIden3Message(
                context, rawMessage
            ).thenApply { message ->
                println("Message: $message")
                PolygonIdSdk.getInstance().getPrivateKey(
                    context = context, secret = secret
                ).thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getDidIdentifier(
                        context = context,
                        privateKey = privateKey,
                        blockchain = "polygon",
                        network = "mumbai",
                    ).thenApply { did ->
                        PolygonIdSdk.getInstance().fetchAndSaveClaims(
                            context = context,
                            message = message as Iden3MessageEntity.OfferIden3MessageEntity,
                            genesisDid = did,
                            privateKey = privateKey
                        ).thenAccept { claims ->
                            PolygonIdSdk.getInstance().getClaims(
                                context = context,
                                genesisDid = did,
                                privateKey = privateKey,
                                //filters = listOf(filter)
                            ).thenApply { claims ->
                                println("Fetched: ${claims.first().id}")
                                eventClaimsLiveData.postValue(claims)
                                fetchStatus.postValue(true)
                            }
                        }.exceptionally {
                            println("Error: $it")
                            fetchStatus.postValue(false)
                            null
                        }
                    }
                }
            }
        }
    }

    fun getClaims(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(
                context = context, secret = secret
            ).thenApply { privateKey ->
                PolygonIdSdk.getInstance().getDidIdentifier(
                    context = context,
                    privateKey = privateKey,
                    blockchain = "polygon",
                    network = "mumbai",
                ).thenApply { did ->
                    /*val id =
                        "https://issuer-testing.polygonid.me/v1/did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9/claims/2bcb98bc-e8db-11ed-938b-0242ac180006"
                    val listValueBuilder = ListValue.newBuilder()
                    listValueBuilder.addValues(
                        Value.newBuilder().setStringValue(id).build()
                    )
                    val value = Value.newBuilder().setListValue(listValueBuilder).build()
                    val filter =
                        FilterEntity.newBuilder().setOperator("nonEqual").setName("id")
                            .setValue(value).build()*/


                    PolygonIdSdk.getInstance().getClaims(
                        context = context,
                        genesisDid = did,
                        privateKey = privateKey,
                        //filters = listOf(filter)
                    ).thenApply { claims ->
                        println("ClaimsFiltered: $claims")
                        eventClaimsLiveData.postValue(claims)
                    }

                }
            }
        }
    }

    private suspend fun getMessageFromRemote(message: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = message.replace("iden3comm://?request_uri=", "")
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Error while getting the message from the remote")
                }

                response.body?.string() ?: throw Exception("No response body")
            } catch (error: Exception) {
                throw Exception("Error while getting the message from the remote", error)
            }
        }
    }

    private fun getMessageFromBase64(message: String): String {
        return try {
            val base64Message = message.replace("iden3comm://?i_m=", "")
            val decodedBytes = Base64.decode(base64Message, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (error: Exception) {
            throw Exception("Error while getting the message from the base64", error)
        }
    }

    fun updateAuthenticationStatus(isAuthenticated: Boolean) {
        _authenticationStatus.value = isAuthenticated
    }


}