package chain.link.linkster.ui.conversation

import androidx.recyclerview.widget.RecyclerView
import chain.link.linkster.ClientManager
import chain.link.linkster.R
import chain.link.linkster.databinding.ListItemConversationBinding
import chain.link.linkster.extension.truncatedAddress
import chain.link.linkster.ui.connections.ConnectionsViewModel
import org.xmtp.android.library.Conversation

class ConversationViewHolder(
    private val binding: ListItemConversationBinding,
    clickListener: ConversationsClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private var conversation: Conversation? = null

    init {
        binding.root.setOnClickListener {
            conversation?.let {
                clickListener.onConversationClick(it)
            }
        }
    }

    fun bind(item: ConnectionsViewModel.MainListItem.ConversationItem) {
        conversation = item.conversation
        binding.peerName.text = item.name + ": " + item.conversation.peerAddress.truncatedAddress()
        binding.peerProfession.text = item.profession
        val messageBody = item.mostRecentMessage?.body.orEmpty()
        val isMe = item.mostRecentMessage?.senderAddress == ClientManager.client.address
        if (messageBody.isNotBlank()) {
            binding.messageBody.text = if (isMe) binding.root.resources.getString(
                R.string.your_message_body,
                messageBody
            ) else messageBody
        } else {
            binding.messageBody.text = binding.root.resources.getString(R.string.empty_message)
        }
    }
}
