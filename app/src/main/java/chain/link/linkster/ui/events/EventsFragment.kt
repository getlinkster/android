package chain.link.linkster.ui.events

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import chain.link.linkster.R
import chain.link.linkster.databinding.FragmentEventsBinding
import chain.link.linkster.ui.connections.ConnectionsFragment
import chain.link.linkster.ui.connections.ConnectionsViewModel
import chain.link.linkster.ui.onboarding.DIDCreationActivity
import chain.link.linkster.ui.onboarding.DIDCreationViewModel
import chain.link.linkster.ui.onboarding.QRCodeScannerActivity

class EventsFragment : Fragment() {

    private val viewModel: EventsViewModel by viewModels()
    private var _binding: FragmentEventsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.init(requireContext())

        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textEvents
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
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
                        viewModel.testFetch(requireContext())
//                        openScanQRCode()
                        true
                    }
                    else -> false
                }
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun openScanQRCode() {
        val intent = Intent(requireContext(), QRCodeScannerActivity::class.java)
        startActivityForResult(intent, AUTHENTICATE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FETCH_CREDENTIAL_REQUEST_CODE
        ) {
            if (resultCode == Activity.RESULT_OK) {
                val scanResult = data?.getStringExtra("SCAN_RESULT")
                when (requestCode) {

                    ConnectionsFragment.AUTHENTICATE_REQUEST_CODE -> {
                        viewModel.authenticate(requireContext(), scanResult ?: "")
                    }

                    FETCH_CREDENTIAL_REQUEST_CODE -> {
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

    companion object {
        const val AUTHENTICATE_REQUEST_CODE = 0
        const val FETCH_CREDENTIAL_REQUEST_CODE = 1
    }
}