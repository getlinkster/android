package chain.link.linkster.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import chain.link.linkster.MainActivity
import chain.link.linkster.R

class DIDCreationActivity : AppCompatActivity() {
    private lateinit var viewModel: DIDCreationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_did_creation)
        viewModel = ViewModelProvider(this)[DIDCreationViewModel::class.java]

        viewModel.init(applicationContext)

        findViewById<Button>(R.id.button_add_identity).setOnClickListener {
            viewModel.addIdentity(applicationContext)
        }

        findViewById<Button>(R.id.button_get_identities).setOnClickListener {
            viewModel.getIdentities(applicationContext)
        }

        findViewById<Button>(R.id.button_authenticate).setOnClickListener {
            val intent = Intent(this, QRCodeScannerActivity::class.java)
            startActivityForResult(intent, AUTHENTICATE_REQUEST_CODE)
        }

        findViewById<Button>(R.id.button_fetch).setOnClickListener {
            val intent = Intent(this, QRCodeScannerActivity::class.java)
            startActivityForResult(intent, FETCH_CREDENTIAL_REQUEST_CODE)
        }

        findViewById<Button>(R.id.move_on).setOnClickListener {
            navigateToMainFlow()
        }

        viewModel.authenticationStatus.observe(this) { isAuthenticated ->
            if (isAuthenticated) {
                navigateToMainFlow()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTHENTICATE_REQUEST_CODE
            || requestCode == FETCH_CREDENTIAL_REQUEST_CODE
        ) {
            if (resultCode == Activity.RESULT_OK) {
                val scanResult = data?.getStringExtra("SCAN_RESULT")
                when (requestCode) {
                    AUTHENTICATE_REQUEST_CODE -> {
                        viewModel.authenticate(applicationContext, scanResult ?: "")
                    }

                    FETCH_CREDENTIAL_REQUEST_CODE -> {
                        viewModel.fetch(applicationContext, scanResult ?: "")
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // no scan
            }
        }
    }

    private fun navigateToMainFlow() {
        // Store onboarding completion state
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        with (sharedPref.edit()) {
            putBoolean("OnboardingCompleted", true)
            apply()
        }

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        const val AUTHENTICATE_REQUEST_CODE = 0
        const val FETCH_CREDENTIAL_REQUEST_CODE = 1
    }
}