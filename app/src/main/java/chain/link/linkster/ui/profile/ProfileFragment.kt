package chain.link.linkster.ui.profile

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import chain.link.linkster.ClientManager
import chain.link.linkster.MainActivity
import chain.link.linkster.R
import chain.link.linkster.databinding.FragmentProfileBinding
import chain.link.linkster.ui.onboarding.DIDCreationActivity

class ProfileFragment : Fragment() {

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
        val profileViewModel =
            ViewModelProvider(this)[ProfileViewModel::class.java]

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        accountManager = AccountManager.get(requireContext())

        val textView: TextView = binding.textProfile
        profileViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.buttonLogout.setOnClickListener {
            logoutUser()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        sharedPref.apply()

        startOnboarding()
    }

    private fun startOnboarding() {
        val intent = Intent(requireActivity() as MainActivity, DIDCreationActivity::class.java)
        startActivity(intent)
        (requireActivity() as MainActivity).finish() // Close MainActivity as we're starting a new flow
    }
}