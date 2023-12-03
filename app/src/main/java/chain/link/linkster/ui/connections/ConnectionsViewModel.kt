package chain.link.linkster.ui.connections

import android.content.Context
import android.util.Base64
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chain.link.linkster.ClientManager
import chain.link.linkster.extension.flowWhileShared
import chain.link.linkster.extension.stateFlow
import chain.link.linkster.ui.conversation.NewConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmtp.android.library.Conversation
import org.xmtp.android.library.DecodedMessage
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.common.domain.entities.EnvEntity
import technology.polygon.polygonid_android_sdk.common.domain.entities.FilterEntity
import technology.polygon.polygonid_android_sdk.credential.domain.entities.ClaimEntity
import technology.polygon.polygonid_android_sdk.iden3comm.domain.entities.Iden3MessageEntity
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

const val TAG = "PolygonIdSdk"
const val secret = "some secret table yep fff so GJ"
const val apiKey = "1861e8a1291f423298faf6a50c9c0a6d"
const val authMessage =
    "{\"id\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"body\":{\"callbackUrl\":\"https://self-hosted-demo-backend-platform.polygonid.me/api/callback?sessionId=98378\",\"reason\":\"test flow\",\"scope\":[]},\"from\":\"did:polygonid:polygon:mumbai:2qLhNLVmoQS7pQtpMeKHDqkTcENBZUj1nkZiRNPGgV\"}"
const val fetchMessage =
    "{\"id\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/credentials/1.0/offer\",\"thid\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"body\":{\"url\":\"https://issuer-testing.polygonid.me/v1/agent\",\"credentials\":[{\"id\":\"2bcb98bc-e8db-11ed-938b-0242ac180006\",\"description\":\"KYCAgeCredential\"}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\",\"to\":\"did:polygonid:polygon:mumbai:2qJGQxEf8n3XiT7fYbqaBdYCUCPQVgkK8rYKbRLTMe\"}"
const val credentialRequestMessage =
    "{\"id\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"body\":{\"callbackUrl\":\"https://self-hosted-testing-backend-platform.polygonid.me/api/callback?sessionId=174262\",\"reason\":\"test flow\",\"scope\":[{\"id\":1,\"circuitId\":\"credentialAtomicQuerySigV2\",\"query\":{\"allowedIssuers\":[\"*\"],\"context\":\"https://raw.githubusercontent.com/iden3/claim-schema-vocab/main/schemas/json-ld/kyc-v3.json-ld\",\"credentialSubject\":{\"birthday\":{\"\$lt\":20000101}},\"skipClaimRevocationCheck\":true,\"type\":\"KYCAgeCredential\"}}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\"}"
const val fetchUserMessage=
    "{\"id\":\"41e47865-b486-4612-ba10-4619ae17825a\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"41e47865-b486-4612-ba10-4619ae17825a\",\"body\":{\"callbackUrl\":\"https://issuer-admin.polygonid.me/v1/credentials/links/callback?sessionID=e2f8011c-487f-498f-868e-4e6a9f2f081a\\u0026linkID=7506b730-450c-485f-b324-c5b1df160113\",\"reason\":\"authentication\",\"scope\":null},\"from\":\"did:polygonid:polygon:mumbai:2qCwHHwTBRWSDAuksyD79mW6G3TrV9HUEC6FwB4AqH\"}"
const val fetchUser2Message=
    "{\"id\":\"5813b0c5-e035-48bf-93b0-2a7f9b2ec4e0\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"5813b0c5-e035-48bf-93b0-2a7f9b2ec4e0\",\"body\":{\"callbackUrl\":\"https://issuer-admin.polygonid.me/v1/credentials/links/callback?sessionID=cccdacc9-f71f-4a3d-ba61-278cdff47cf1\\u0026linkID=d68e7695-2ae0-4cd3-a4d9-f8140f14b7fb\",\"reason\":\"authentication\",\"scope\":null},\"from\":\"did:polygonid:polygon:mumbai:2qCwHHwTBRWSDAuksyD79mW6G3TrV9HUEC6FwB4AqH\"}"
// https://issuer-ui.polygonid.me/credentials/scan-link/7506b730-450c-485f-b324-c5b1df160113

class ConnectionsViewModel : ViewModel() {

    val ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}\$")
    private val _text = MutableLiveData<String>().apply {
        value = "This is connections Fragment"
    }
    val text: LiveData<String> = _text
    val claimsLiveData: MutableLiveData<List<ClaimEntity>> = MutableLiveData()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading(null))
    val uiState: StateFlow<UiState> = _uiState

    fun init(context: Context) {
        val mumbai = EnvEntity(
            blockchain = "polygon",
            network = "mumbai",
            web3Url = "https://polygon-mumbai.infura.io/v3/",
            web3RdpUrl = "wss://polygon-mumbai.infura.io/v3/",
            web3ApiKey = chain.link.linkster.ui.events.apiKey,
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
                        }.exceptionally {
                            println("Authentication Error: $it")
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
                    context = context, secret = chain.link.linkster.ui.events.secret
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
                            println("Fetched: ${claims.first().id}")
                            claimsLiveData.postValue(claims)
                        }.exceptionally {
                            println("Error: $it")
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
                        claimsLiveData.postValue(claims)
                    }

                }
            }
        }
    }

    @UiThread
    fun setupPush() {
        viewModelScope.launch(Dispatchers.IO) {
//            PushNotificationTokenManager.ensurePushTokenIsConfigured()
        }
    }

    @UiThread
    fun createConversation(address: String) {
        _uiState.value = UiState.Loading(null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ClientManager.client.conversations.newConversation(address)
                fetchConversations()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage.orEmpty())
            }
        }
    }

    @UiThread
    fun fetchConversations() {
        when (val uiState = uiState.value) {
            is UiState.Success -> _uiState.value = UiState.Loading(uiState.listItems)
            else -> _uiState.value = UiState.Loading(null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val listItems = mutableListOf<MainListItem>()
            try {
                val conversations = ClientManager.client.conversations.list()
//                PushNotificationTokenManager.xmtpPush.subscribe(conversations.map { it.topic })
                listItems.addAll(
                    conversations.map { conversation ->
                        val lastMessage = fetchMostRecentMessage(conversation)
                        MainListItem.ConversationItem(
                            id = conversation.topic,
                            conversation,
                            lastMessage
                        )
                    }
                )
//                listItems.add(
//                    MainListItem.Footer(
//                        id = "footer",
//                        ClientManager.client.address,
//                        ClientManager.client.apiClient.environment.name
//                    )
//                )
                _uiState.value = UiState.Success(listItems)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage.orEmpty())
            }
        }
    }

    @WorkerThread
    private fun fetchMostRecentMessage(conversation: Conversation): DecodedMessage? {
        return conversation.messages(limit = 1).firstOrNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val stream: StateFlow<MainListItem?> =
        stateFlow(viewModelScope, null) { subscriptionCount ->
            if (ClientManager.clientState.value is ClientManager.ClientState.Ready) {
                ClientManager.client.conversations.stream()
                    .flowWhileShared(
                        subscriptionCount,
                        SharingStarted.WhileSubscribed(1000L)
                    )
                    .flowOn(Dispatchers.IO)
                    .distinctUntilChanged()
                    .mapLatest { conversation ->
                        val lastMessage = fetchMostRecentMessage(conversation)
                        MainListItem.ConversationItem(conversation.topic, conversation, lastMessage)
                    }
                    .catch { emptyFlow<MainListItem>() }
            } else {
                emptyFlow()
            }
        }

    sealed class UiState {
        data class Loading(val listItems: List<MainListItem>?) : UiState()
        data class Success(val listItems: List<MainListItem>) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class MainListItem(open val id: String, val itemType: Int) {
        companion object {
            const val ITEM_TYPE_CONVERSATION = 1
            const val ITEM_TYPE_FOOTER = 2
        }

        data class ConversationItem(
            override val id: String,
            val conversation: Conversation,
            val mostRecentMessage: DecodedMessage?,
        ) : MainListItem(id, ITEM_TYPE_CONVERSATION)

        data class Footer(
            override val id: String,
            val address: String,
            val environment: String,
        ) : MainListItem(id, ITEM_TYPE_FOOTER)
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
}