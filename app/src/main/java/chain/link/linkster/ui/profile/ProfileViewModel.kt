package chain.link.linkster.ui.profile

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chain.link.linkster.RandomStringManager
import kotlinx.coroutines.launch
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.common.domain.entities.EnvEntity
import technology.polygon.polygonid_android_sdk.identity.domain.entities.IdentityEntity

const val apiKey = "1861e8a1291f423298faf6a50c9c0a6d"
class ProfileViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is profile Fragment"
    }
    val text: LiveData<String> = _text
    val secret = RandomStringManager.randomString
    private val _identities = MutableLiveData<List<IdentityEntity>>()
    val identities: LiveData<List<IdentityEntity>> = _identities


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

    fun getIdentities(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().getIdentities(
                context = context
            ).thenApply { identities ->
                println("Identities: $identities")
                _identities.postValue(identities)
            }.exceptionally { throwable ->
                println("Error: $throwable")
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