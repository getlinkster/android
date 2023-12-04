package chain.link.linkster.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import chain.link.linkster.MainActivity
import chain.link.linkster.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import technology.polygon.polygonid_android_sdk.PolygonIdSdk
import technology.polygon.polygonid_android_sdk.proof.domain.entities.DownloadInfoEntity

class SplashActivity : AppCompatActivity() {
    private lateinit var viewModel: SplashViewModel
    private lateinit var downloadProgressBar: ProgressBar
    private lateinit var downloadText: TextView
    private lateinit var nextPage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        viewModel = ViewModelProvider(this)[SplashViewModel::class.java]
        viewModel.init(applicationContext)


        downloadProgressBar = findViewById(R.id.downloadProgressBar)
        downloadText = findViewById(R.id.downloadText)

        nextPage = findViewById(R.id.next_page)

        findViewById<Button>(R.id.button_start_download).setOnClickListener {
            downloadProgressBar.visibility = View.VISIBLE
            downloadText.text = ""

            lifecycleScope.launch {
                PolygonIdSdk.getInstance().getDownloadCircuitsFlow().collectLatest { info ->

                    when (info) {
                        is DownloadInfoEntity.DownloadInfoOnProgress -> {
                            val progress = (info.downloaded.toFloat() / info.contentLength.toFloat() * 100).toInt()
                            downloadProgressBar.progress = progress
                        }
                        is DownloadInfoEntity.DownloadInfoOnDone -> {
                            downloadProgressBar.visibility = View.GONE // Hide progress bar
                            downloadText.text = "Download completed"
                            nextPage.visibility = View.VISIBLE
                            viewModel.markAsDownloaded(applicationContext)
                        }
                        is DownloadInfoEntity.DownloadInfoOnError -> {
                            downloadProgressBar.visibility = View.GONE // Hide progress bar
                            downloadText.text = "Download failed"
                        }
                    }
                }
            }

            viewModel.startDownload(applicationContext)
        }

        findViewById<Button>(R.id.button_check_download_circuits).setOnClickListener {
            viewModel.checkDownloadCircuits(applicationContext)
        }

        findViewById<Button>(R.id.button_cancel_download_circuits).setOnClickListener {
            viewModel.cancelDownloadCircuits(applicationContext)
        }

        findViewById<Button>(R.id.next_page).setOnClickListener {
            viewModel.uiState.value.let { splashState ->
                if (splashState.isDownloaded == true) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    viewModel.startDownload(applicationContext)
                }
            }
        }
    }


}