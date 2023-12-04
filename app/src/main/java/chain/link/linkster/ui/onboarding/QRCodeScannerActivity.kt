package chain.link.linkster.ui.onboarding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import chain.link.linkster.R
import com.google.zxing.integration.android.IntentIntegrator

class QRCodeScannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        val scanner = IntentIntegrator(this)

        scanner.setPrompt("Scan QR Code")
        scanner.setCameraId(0)
        scanner.setOrientationLocked(true)
        scanner.setBeepEnabled(true)
        scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        scanner.captureActivity = CaptureActivityPortrait::class.java
        scanner.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if(result != null) {
            if(result.contents == null) {
                setResult(Activity.RESULT_CANCELED)
            } else {
                val intent = Intent().apply { putExtra("SCAN_RESULT", result.contents) }
                setResult(Activity.RESULT_OK, intent)
            }
            finish()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}