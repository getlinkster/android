package chain.link.linkster.ui.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.common.domain.entities.EnvEntity

const val apiKey = "1861e8a1291f423298faf6a50c9c0a6d"

data class SplashState(
    val isDownloaded: Boolean? = null,
)

class SplashViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SplashState())
    val uiState: StateFlow<SplashState> = _uiState.asStateFlow()

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

    fun startDownload(context: Context) {
        PolygonIdSdk.getInstance().startDownloadCircuits(context = context).thenAccept {
            println("Stream started")
        }
    }

    fun checkDownloadCircuits(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().isAlreadyDownloadedCircuitsFromServer(context)
                .thenApply { isDownloaded ->
                    _uiState.update { currentState ->
                        currentState.copy(isDownloaded = true)
                    }
                }
        }
    }

    fun markAsDownloaded(context: Context) {
        _uiState.update { currentState ->
            currentState.copy(isDownloaded = true)
        }
    }

    fun cancelDownloadCircuits(context: Context) {
        viewModelScope.launch {
            PolygonIdSdk.getInstance().cancelDownloadCircuits(context).thenApply {
                println("cancelDownloadCircuits: $it")
            }
        }
    }
}