package chain.link.linkster.ui.profile

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import chain.link.linkster.ClientManager
import chain.link.linkster.CreateProfileRequest
import chain.link.linkster.CreateUserCredentialRequest
import chain.link.linkster.CredentialResponse
import chain.link.linkster.CredentialUserSubject
import chain.link.linkster.MainActivity
import chain.link.linkster.QRCodeResponse
import chain.link.linkster.R
import chain.link.linkster.RetrofitClient
import chain.link.linkster.SchemaResponse
import chain.link.linkster.databinding.FragmentProfileBinding
import chain.link.linkster.ui.dialog.QRCodeDialogFragment
import chain.link.linkster.ui.onboarding.DIDCreationActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import org.xmtp.android.library.Client
import org.xmtp.android.library.messages.PrivateKeyBuilder
import org.xmtp.android.library.messages.PrivateKeyBundleV1Builder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private var _binding: FragmentProfileBinding? = null
    private lateinit var accountManager: AccountManager
    private val schemaList = mutableListOf<SchemaResponse>()
    private var profileCredentialId: String = ""


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
            getQRCodeForCredential(profileCredentialId)
        }

        binding.buttonLogout.setOnClickListener {
            logoutUser()
        }

        binding.createProfile.setOnClickListener { createProfile() }

        binding.createCredential.setOnClickListener { createUserCredential() }

        viewModel.identities.observe(viewLifecycleOwner) { identities ->
            val dids = identities.map { it.did } // Extract DIDs from identities
            val joinedDids = dids.joinToString(separator = ", ") // Join DIDs with a separator

            binding.identities.text = "DID addresses: $joinedDids"
            println("Identities: $identities")
        }


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getIdentities(requireContext())
        fetchSchemas()
        walletDetails()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun walletDetails(){
        viewModel.getPrivateKey(requireContext()) { privateKeyBytes ->
            val privateKey = PrivateKeyBuilder.buildFromPrivateKeyData(privateKeyBytes)
            val wallet = PrivateKeyBuilder(privateKey)
            binding.walletAddress.text = "Wallet Address: ${wallet.address}"
        }
    }

    private fun fetchSchemas() {
        binding.progress.visibility = View.VISIBLE
        RetrofitClient.instance.getSchemas().enqueue(object : Callback<List<SchemaResponse>> {
            override fun onResponse(call: Call<List<SchemaResponse>>, response: Response<List<SchemaResponse>>) {
                if (response.isSuccessful) {
                    val schemas = response.body()
                    println("Schemas: $schemas")
                    schemas?.forEach { schema ->
                        val schemaResponse = SchemaResponse(type = schema.type, id = schema.id, url = schema.url, title = schema.title, description = schema.description, version = schema.version, createdAt = schema.createdAt, hash = schema.hash, bigInt = schema.bigInt)
                        schemaList.add(schemaResponse)
                        binding.progress.visibility = View.GONE
                    }
                } else {
                    binding.progress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading schemas", Toast.LENGTH_SHORT).show()
                    // Handle failure, possibly logging the error or showing a message
                }
            }

            override fun onFailure(call: Call<List<SchemaResponse>>, t: Throwable) {
                binding.progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                println("Error: ${t.message}")
                // Handle the error, such as network issues
            }
        })
    }

    private fun createProfile() {
        val profileData = CreateProfileRequest(
            name = "Beff Jezos",
            wallet = "0x12312312312312123123",
            profession = "CEO",
            company = "Amazon",
            telegram = "beffy"
        )

        RetrofitClient.instance.createProfile(profileData).enqueue(object : Callback<QRCodeResponse> {
            override fun onResponse(call: Call<QRCodeResponse>, response: Response<QRCodeResponse>) {
                if (response.isSuccessful) {
                    val qrCodeUrl = response.body()?.qrCodeLink
                    if (qrCodeUrl != null) {
                        val qrCodeBitmap = generateQRCodeBitmap(qrCodeUrl)
                        showQRCodeDialog(qrCodeBitmap)
                    } else {
                        // Handle the case where the response does not contain a profile ID
                    }
                } else {
                    // Handle failure, possibly logging the error or showing a message
                }
            }

            override fun onFailure(call: Call<QRCodeResponse>, t: Throwable) {
                println("Error: ${t.message}")
                // Handle the error, such as network issues
            }
        })
    }


    private fun showQRCodeDialog(qrCodeBitmap: Bitmap) {
        val dialog = QRCodeDialogFragment().apply {
            setQRCodeImage(qrCodeBitmap)
        }
        dialog.show(parentFragmentManager, "QRCodeDialog")
    }

    private fun createUserCredential() {
        val linksterUserSchema = schemaList?.find { it.type == "LINKSTERUSER" }
        if (linksterUserSchema != null) {
            val credentialSchema = linksterUserSchema.url
            val type = linksterUserSchema.type
            val expiration = "2023-12-06T16:09:43.731Z"
            val signatureProof = true
            val mtProof = true

            val credentialSubject = CredentialUserSubject(
                name = "Steve Smith",
                wallet = "0x2Ee9B2771602596A52a094ed74a71B02D499449A",
                profession = "Software Engineer"
            )

            val request = CreateUserCredentialRequest(
                credentialSchema = credentialSchema,
                type = type,
                credentialSubject = credentialSubject,
                expiration = expiration,
                signatureProof = signatureProof,
                mtProof = mtProof
            )

            RetrofitClient.instance.creatUserCredential(request)
                .enqueue(object : Callback<CredentialResponse> {
                    override fun onResponse(
                        call: Call<CredentialResponse>,
                        response: Response<CredentialResponse>
                    ) {
                        if (response.isSuccessful) {
                            val credentialId = response.body()?.id
                            if (credentialId != null) {
                                // Credential created successfully, you can handle the response here
                                println("Credential created with ID: $credentialId")
                                profileCredentialId = credentialId
                            } else {
                                // Handle the case where the response body is null
                            }
                        } else {
                            // Handle the API request failure, possibly logging the error or showing a message
                        }
                    }

                    override fun onFailure(call: Call<CredentialResponse>, t: Throwable) {
                        println("Error: ${t.message}")
                        // Handle the error, such as network issues
                    }
                })
        } else {
            Toast.makeText(requireContext(), "Schema not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getQRCodeForCredential(credentialId: String) {
        if (credentialId.isNotBlank()) {
            RetrofitClient.instance.getQRCodeForCredential(credentialId)
                .enqueue(object : Callback<QRCodeResponse> {
                    override fun onResponse(
                        call: Call<QRCodeResponse>,
                        response: Response<QRCodeResponse>
                    ) {
                        if (response.isSuccessful) {
                            val qrCodeUrl = response.body()?.qrCodeLink
                            if (!qrCodeUrl.isNullOrEmpty()) {
                                // Handle the QR code URL, you can display it or perform further actions
                                // For example, you can generate a QR code image and display it
                                val qrCodeBitmap = generateQRCodeBitmap(qrCodeUrl)
                                showQRCodeDialog(qrCodeBitmap)
                            } else {
                                // Handle the case where the QR code URL is empty
                            }
                        } else {
                            // Handle API error or non-successful response
                        }
                    }

                    override fun onFailure(call: Call<QRCodeResponse>, t: Throwable) {
                        // Handle network or request failure
                        println("Error: ${t.message}")
                    }
                })
        } else {
            Toast.makeText(requireContext(), "Credential ID is empty", Toast.LENGTH_SHORT).show()
        }
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