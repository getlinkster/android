package chain.link.linkster.ui.connections

import android.accounts.AccountManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import chain.link.linkster.ClientManager
import chain.link.linkster.R
import chain.link.linkster.databinding.FragmentConnectionsBinding
import chain.link.linkster.ui.conversation.ConversationDetailActivity
import chain.link.linkster.ui.conversation.ConversationsAdapter
import chain.link.linkster.ui.conversation.ConversationsClickListener
import chain.link.linkster.ui.conversation.NewConversationBottomSheet
import chain.link.linkster.utils.KeyUtil
import kotlinx.coroutines.launch
import org.xmtp.android.library.Conversation

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
        val connectionsViewModel =
            ViewModelProvider(this)[ConnectionsViewModel::class.java]

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
        binding.fab.setOnClickListener {
            openConversationDetail()
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

        return root
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
                binding.fab.visibility = View.VISIBLE
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

    private fun openConversationDetail() {
        bottomSheet = NewConversationBottomSheet.newInstance()
        bottomSheet?.show(
            requireFragmentManager(),
            NewConversationBottomSheet.TAG
        )
    }
}