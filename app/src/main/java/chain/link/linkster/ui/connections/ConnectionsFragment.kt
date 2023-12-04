package chain.link.linkster.ui.connections

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import chain.link.linkster.ClientManager
import chain.link.linkster.QRCodeResponse
import chain.link.linkster.R
import chain.link.linkster.RetrofitClient
import chain.link.linkster.databinding.FragmentConnectionsBinding
import chain.link.linkster.ui.conversation.ConversationDetailActivity
import chain.link.linkster.ui.conversation.ConversationsAdapter
import chain.link.linkster.ui.conversation.ConversationsClickListener
import chain.link.linkster.ui.conversation.NewConversationBottomSheet
import chain.link.linkster.ui.dialog.QRCodeDialogFragment
import chain.link.linkster.ui.onboarding.QRCodeScannerActivity
import chain.link.linkster.utils.KeyUtil
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import retrofit2.Call
import retrofit2.Response
import org.xmtp.android.library.Conversation
import retrofit2.Callback

class ConnectionsFragment : Fragment(), ConversationsClickListener {

    private val viewModel: ConnectionsViewModel by viewModels()
    private var _binding: FragmentConnectionsBinding? = null
    private lateinit var accountManager: AccountManager
    private lateinit var adapter: ConversationsAdapter
    private var bottomSheet: NewConversationBottomSheet? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.init(requireContext())

        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val keys = KeyUtil(requireContext()).loadKeys()
        ClientManager.createClient(keys!!)

        adapter = ConversationsAdapter(clickListener = this)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.refresh.setOnRefreshListener {
            if (ClientManager.clientState.value is ClientManager.ClientState.Ready) {
                viewModel.fetchConversations()
            }
        }

        binding.validateClaims.setOnClickListener {
            openFetchScanQRCode()
        }

        binding.fetchClaims.setOnClickListener {
            fetchClaims()
        }

        binding.showMyCode.setOnClickListener {
            createAuthLinkQRCode("e5d3d537-edb1-4302-ab36-b72f9c0c154f", "e5d3d537-edb1-4302-ab36-b72f9c0c154f")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ClientManager.clientState.collect(::ensureClientState)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::ensureUiState)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stream.collect(::addStreamedItem)
            }
        }

        viewModel.claimsLiveData.observe(viewLifecycleOwner) { claims ->
            // Handle the fetched claims list here
            claims.forEach { claim ->
                println("Fetched Claim ID: ${claim.id}")

                val credentialSubject = claim.info["credentialSubject"] as? JsonObject
                if (credentialSubject != null) {
                    val wallet = credentialSubject["wallet"]
                    val name = credentialSubject["name"]

                    // Use the 'wallet' and 'name' values as needed
                    println("Wallet: $wallet")
                    println("Name: $name")

                    val input = wallet.toString().trim().replace("\"", "")
                    val matcher = viewModel.ADDRESS_PATTERN.matcher(input)
                    if (matcher.matches()) {
                        viewModel.createConversation(input)
                    }
                } else {
                    // Handle the case where 'credentialSubject' is not present or is of the wrong type
                    println("credentialSubject not found or of the wrong type")
                }
            }
        }

        viewModel.authenticationStatus.observe(viewLifecycleOwner) { isAuthenticated ->
            binding.progress.visibility = View.GONE
            if (isAuthenticated) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Authentication Successful")
                    .setMessage("Do you want to add this credential to your wallet?")
                    .setPositiveButton("Yes") { _, _ ->
                        openFetchScanQRCode()
                    }
                    .setNegativeButton("No") { _, _ ->
                        // User clicked "No," do nothing or handle accordingly
                    }
                    .create()
                    .show()
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Authentication Unsuccessful")
                    .setMessage("Do you want to try again?")
                    .setPositiveButton("Yes") { _, _ ->
                        openScanQRCode()
                    }
                    .setNegativeButton("No") { _, _ ->
                        // User clicked "No," do nothing or handle accordingly
                    }
                    .create()
                    .show()
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Inflate your menu resource here
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle menu item selection
                return when (menuItem.itemId) {
                    R.id.action_scan -> {
                        openScanQRCode()
                        true
                    }
                    else -> false
                }
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        fetchClaims()
    }

    private fun showQRCodeDialog(qrCodeBitmap: Bitmap) {
        val dialog = QRCodeDialogFragment().apply {
            setQRCodeImage(qrCodeBitmap)
        }
        dialog.show(parentFragmentManager, "QRCodeDialog")
    }

    fun openScanQRCode() {
        val intent = Intent(requireContext(), QRCodeScannerActivity::class.java)
        startActivityForResult(intent, AUTHENTICATE_REQUEST_CODE)
    }

    fun openFetchScanQRCode() {
        val intent = Intent(requireContext(), QRCodeScannerActivity::class.java)
        startActivityForResult(intent, FETCH_CREDENTIAL_REQUEST_CODE)
    }

    fun fetchClaims() {
        viewModel.getClaims(requireContext())
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
                        binding.progress.visibility = View.VISIBLE
                        viewModel.authenticate(requireContext(), scanResult ?: "")
                    }

                    FETCH_CREDENTIAL_REQUEST_CODE -> {
                        binding.progress.visibility = View.VISIBLE
                        viewModel.fetch(requireContext(), scanResult ?: "")
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // no scan
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConversationClick(conversation: Conversation) {
        startActivity(
            ConversationDetailActivity.intent(
                requireContext(),
                topic = conversation.topic,
                peerAddress = conversation.peerAddress
            )
        )
    }

    override fun onFooterClick(address: String) {
        copyWalletAddress()
    }

    private fun ensureClientState(clientState: ClientManager.ClientState) {
        when (clientState) {
            is ClientManager.ClientState.Ready -> {
                viewModel.fetchConversations()
//                binding.fab.visibility = View.VISIBLE
//                binding.fabFetch.visibility = View.VISIBLE
            }
            is ClientManager.ClientState.Error -> showError(clientState.message)
            is ClientManager.ClientState.Unknown -> Unit
        }
    }

    private fun addStreamedItem(item: ConnectionsViewModel.MainListItem?) {
        item?.let {
            adapter.addItem(item)
        }
    }

    private fun ensureUiState(uiState: ConnectionsViewModel.UiState) {
        binding.progress.visibility = View.GONE
        when (uiState) {
            is ConnectionsViewModel.UiState.Loading -> {
                if (uiState.listItems.isNullOrEmpty()) {
                    binding.progress.visibility = View.VISIBLE
                } else {
                    adapter.setData(uiState.listItems)
                }
            }
            is ConnectionsViewModel.UiState.Success -> {
                binding.refresh.isRefreshing = false
                adapter.setData(uiState.listItems)
            }
            is ConnectionsViewModel.UiState.Error -> {
                binding.refresh.isRefreshing = false
                showError(uiState.message)
            }
        }
    }

    private fun showError(message: String) {
        val error = message.ifBlank { resources.getString(R.string.error) }
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
    }

    private fun copyWalletAddress() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("address", ClientManager.client.address)
        clipboard.setPrimaryClip(clip)
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

    private fun openConversationDetail() {
        bottomSheet = NewConversationBottomSheet.newInstance()
        bottomSheet?.show(
            requireFragmentManager(),
            NewConversationBottomSheet.TAG
        )
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

    companion object {
        const val AUTHENTICATE_REQUEST_CODE = 0
        const val FETCH_CREDENTIAL_REQUEST_CODE = 1
    }
}