package chain.link.linkster.ui.profile

import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import chain.link.linkster.ClientManager
import chain.link.linkster.MainActivity
import chain.link.linkster.QRCodeResponse
import chain.link.linkster.R
import chain.link.linkster.RetrofitClient
import chain.link.linkster.databinding.FragmentProfileBinding
import chain.link.linkster.ui.dialog.QRCodeDialogFragment
import chain.link.linkster.ui.onboarding.DIDCreationActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private var _binding: FragmentProfileBinding? = null
    private lateinit var accountManager: AccountManager


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.init(requireContext())

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        accountManager = AccountManager.get(requireContext())

        binding.showMyCode.setOnClickListener {
            createAuthLinkQRCode("e5d3d537-edb1-4302-ab36-b72f9c0c154f", "e5d3d537-edb1-4302-ab36-b72f9c0c154f")
        }

        binding.buttonLogout.setOnClickListener {
            logoutUser()
        }

        viewModel.identities.observe(viewLifecycleOwner) { identities ->
            val dids = identities.map { it.did } // Extract DIDs from identities
            val joinedDids = dids.joinToString(separator = ", ") // Join DIDs with a separator

            binding.identities.text = joinedDids // Set the joined DIDs to your UI element
            println("Identities: $identities")
        }


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getIdentities(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showQRCodeDialog(qrCodeBitmap: Bitmap) {
        val dialog = QRCodeDialogFragment().apply {
            setQRCodeImage(qrCodeBitmap)
        }
        dialog.show(parentFragmentManager, "QRCodeDialog")
    }

    private fun createAuthLinkQRCode(sessionID: String, linkID: String) {
        RetrofitClient.instance.createAuthLinkQRCode(sessionID, linkID)
            .enqueue(object : Callback<QRCodeResponse> {
                override fun onResponse(call: Call<QRCodeResponse>, response: Response<QRCodeResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { qrCodeResponse ->
                            val qrCodeUrl = qrCodeResponse.qrCodeLink
                            val qrCodeBitmap = generateQRCodeBitmap(qrCodeUrl)
                            showQRCodeDialog(qrCodeBitmap)
                        }
                    } else {
                        // Handle failure, possibly logging the error or showing a message
                    }
                }

                override fun onFailure(call: Call<QRCodeResponse>, t: Throwable) {
                    println("Error: $t.message")
                    // Handle the error, such as network issues
                }
            })
    }

    private fun generateQRCodeBitmap(url: String): Bitmap {
        val qrCodeWriter = QRCodeWriter()
        val width = 300
        val height = 300

        return try {
            val bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            // Handle exception
            Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565) // return an empty bitmap on error
        }
    }

    private fun logoutUser() {
        ClientManager.clearClient()
        val accounts = accountManager.getAccountsByType(resources.getString(R.string.account_type))
        accounts.forEach { account ->
            accountManager.removeAccount(account, null, null, null)
        }

        // Reset the shared preferences
        val sharedPref = requireContext().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE).edit()
        sharedPref.putBoolean("OnboardingCompleted", false)
        sharedPref
        sharedPref.apply()

        startOnboarding()
    }

    private fun startOnboarding() {
        val intent = Intent(requireActivity() as MainActivity, DIDCreationActivity::class.java)
        startActivity(intent)
        (requireActivity() as MainActivity).finish() // Close MainActivity as we're starting a new flow
    }
}