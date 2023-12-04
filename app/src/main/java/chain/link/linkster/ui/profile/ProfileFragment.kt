package chain.link.linkster.ui.profile

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import chain.link.linkster.ClientManager
import chain.link.linkster.MainActivity
import chain.link.linkster.R
import chain.link.linkster.databinding.FragmentProfileBinding
import chain.link.linkster.ui.onboarding.DIDCreationActivity

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