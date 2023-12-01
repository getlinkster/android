package chain.link.linkster.ui.onboarding

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.common.domain.entities.EnvEntity
import technology.polygon.polygonid_android_sdk.iden3comm.domain.entities.Iden3MessageEntity
import java.math.BigInteger

const val TAG = "PolygonIdSdk"
const val secret = "some secret table yep fff so GJ"
const val apiKey = "theApiKey"
const val authMessage =
    "{\"id\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"f6a69960-763f-48f5-a7e5-b3ea066cfbc7\",\"body\":{\"callbackUrl\":\"https://self-hosted-demo-backend-platform.polygonid.me/api/callback?sessionId=98378\",\"reason\":\"test flow\",\"scope\":[]},\"from\":\"did:polygonid:polygon:mumbai:2qLhNLVmoQS7pQtpMeKHDqkTcENBZUj1nkZiRNPGgV\"}"
const val fetchMessage =
    "{\"id\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/credentials/1.0/offer\",\"thid\":\"bae3a15c-3570-4e33-9cdd-739b6105fc15\",\"body\":{\"url\":\"https://issuer-testing.polygonid.me/v1/agent\",\"credentials\":[{\"id\":\"2bcb98bc-e8db-11ed-938b-0242ac180006\",\"description\":\"KYCAgeCredential\"}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\",\"to\":\"did:polygonid:polygon:mumbai:2qJGQxEf8n3XiT7fYbqaBdYCUCPQVgkK8rYKbRLTMe\"}"
const val credentialRequestMessage =
    "{\"id\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"typ\":\"application/iden3comm-plain-json\",\"type\":\"https://iden3-communication.io/authorization/1.0/request\",\"thid\":\"b11bdbb1-5a6c-49ca-a180-6e5040a50f41\",\"body\":{\"callbackUrl\":\"https://self-hosted-testing-backend-platform.polygonid.me/api/callback?sessionId=174262\",\"reason\":\"test flow\",\"scope\":[{\"id\":1,\"circuitId\":\"credentialAtomicQuerySigV2\",\"query\":{\"allowedIssuers\":[\"*\"],\"context\":\"https://raw.githubusercontent.com/iden3/claim-schema-vocab/main/schemas/json-ld/kyc-v3.json-ld\",\"credentialSubject\":{\"birthday\":{\"\$lt\":20000101}},\"skipClaimRevocationCheck\":true,\"type\":\"KYCAgeCredential\"}}]},\"from\":\"did:polygonid:polygon:mumbai:2qFXmNqGWPrLqDowKz37Gq2FETk4yQwVUVUqeBLmf9\"}"

class DIDCreationViewModel : ViewModel() {
    val authenticationStatus = MutableLiveData<Boolean>()

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

    //// IDENTITY
    fun addIdentity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().addIdentity(
                context = context, secret = secret
            ).thenApply { identity ->
                println("Identity: $identity")

                PolygonIdSdk.getInstance().addProfile(
                    context = context,
                    genesisDid = identity.did,
                    privateKey = identity.privateKey,
                    profileNonce = BigInteger("3000")
                ).thenApply {
                    println("Profile added")
                }
            }
        }
    }

    fun backupIdentity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            PolygonIdSdk.getInstance().backupIdentity(
                                context = context,
                                privateKey = privateKey,
                                genesisDid = didIdentifier
                            ).thenApply { backup ->
                                println("Backup: $backup")
                            }
                        }
                    }
                }
        }
    }

    fun restoreIdentity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            PolygonIdSdk.getInstance().backupIdentity(
                                context = context,
                                privateKey = privateKey,
                                genesisDid = didIdentifier
                            ).thenApply { backup ->
                                PolygonIdSdk.getInstance().restoreIdentity(
                                    context = context,
                                    privateKey = privateKey,
                                    genesisDid = didIdentifier,
                                    encryptedDb = backup
                                ).thenApply { restored ->
                                    println("RestoredIdentity: $restored")
                                }
                            }
                        }
                    }
                }
        }
    }

    fun checkIdentityValidity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().checkIdentityValidity(context = context, secret = secret)
                .thenApply { isValid ->
                    println("isValid: $isValid")
                }.exceptionally { throwable ->
                    println("ErrorCheckIdentityValidity: $throwable")
                }
        }
    }

    fun getState(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            PolygonIdSdk.getInstance()
                                .getState(context = context, did = didIdentifier)
                                .thenApply { state ->
                                    println("State: $state")
                                }
                        }
                    }
                }
        }
    }

    fun getDidEntity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            PolygonIdSdk.getInstance()
                                .getDidEntity(context = context, did = didIdentifier)
                                .thenApply {
                                    println("DidEntity: ${it.did}")
                                }.exceptionally {
                                    println("Error: $it")
                                }
                        }
                    }
                }
        }
    }

    fun getIdentities(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getIdentities(
                context = context
            ).thenApply { identities ->
                println("Identities: $identities")
            }.exceptionally { throwable ->
                println("Error: $throwable")
            }
        }
    }

    fun authenticate(context: Context, authMessage: String) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getIden3Message(
                context, authMessage
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
                            authenticationStatus.postValue(true)
                        }.exceptionally {
                            println("Authentication Error: $it")
                            authenticationStatus.postValue(false)
                            null
                        }
                    }
                }
            }
        }
    }

    fun fetch(context: Context, fetchMessage: String) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getIden3Message(
                context, fetchMessage
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
                            println("Fetched: ${claims.first().id}")
                        }.exceptionally {
                            println("Error: $it")
                            null
                        }
                    }
                }
            }
        }
    }

    /// GET IDENTITY
    fun getIdentity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            println("DidIdentifier: $didIdentifier")
                            PolygonIdSdk.getInstance().getIdentity(
                                context = context,
                                privateKey = privateKey,
                                genesisDid = didIdentifier
                            ).thenApply { identity ->
                                println("Identity: ${identity.did}")
                            }
                        }
                    }
                }
        }
    }

    fun removeIdentity(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            println("DidIdentifier: $didIdentifier")
                            PolygonIdSdk.getInstance().removeIdentity(
                                context = context,
                                privateKey = privateKey,
                                genesisDid = didIdentifier
                            ).thenApply { identity ->
                                println("removeIdentity: $identity")
                            }
                        }
                    }
                }
        }
    }

    fun removeProfile(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            println("DidIdentifier: $didIdentifier")
                            PolygonIdSdk.getInstance().removeProfile(
                                context = context,
                                privateKey = privateKey,
                                genesisDid = didIdentifier,
                                profileNonce = BigInteger("1000"),
                            ).thenApply { identity ->
                                println("removeIdentity: $identity")
                            }
                        }
                    }
                }
        }
    }

    fun sign(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().sign(
                        context = context,
                        privateKey = privateKey,
                        message = "0xff123456",
                    ).thenApply { signature ->
                        println("Signature: $signature")
                    }
                }

        }
    }

    fun getPrivateKeyAndDid(context: Context, completion: (String, String) -> Unit) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKey ->
                    PolygonIdSdk.getInstance().getEnv(context = context).thenApply { env ->
                        PolygonIdSdk.getInstance().getDidIdentifier(
                            context = context,
                            privateKey = privateKey,
                            blockchain = env.blockchain,
                            network = env.network,
                        ).thenApply { didIdentifier ->
                            completion(privateKey, didIdentifier)
                        }
                    }
                }
        }
    }

    fun getPrivateKey(context: Context, completion: (ByteArray) -> Unit) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getPrivateKey(context = context, secret = secret)
                .thenApply { privateKeyString ->
                    val privateKeyBytes = hexStringToByteArray(privateKeyString)
                    completion(privateKeyBytes)
                }
        }
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4) + Character.digit(hexString[i + 1], 16)).toByte()
        }
        return data
    }


}