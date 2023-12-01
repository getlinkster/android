package chain.link.linkster.ui.conversation

import org.xmtp.android.library.Conversation

interface ConversationsClickListener {
    fun onConversationClick(conversation: Conversation)
    fun onFooterClick(address: String)
}
