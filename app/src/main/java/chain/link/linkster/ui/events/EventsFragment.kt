package chain.link.linkster.ui.events

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import chain.link.linkster.ClientManager
import chain.link.linkster.R
import chain.link.linkster.databinding.FragmentEventsBinding
import chain.link.linkster.ui.connections.ConnectionsFragment
import chain.link.linkster.ui.connections.ConnectionsViewModel
import chain.link.linkster.ui.onboarding.DIDCreationActivity
import chain.link.linkster.ui.onboarding.DIDCreationViewModel
import chain.link.linkster.ui.onboarding.QRCodeScannerActivity
import kotlinx.serialization.json.JsonObject

class EventsFragment : Fragment() {

    private val viewModel: EventsViewModel by viewModels()
    private var _binding: FragmentEventsBinding? = null

    private lateinit var listView: ListView
    private lateinit var claimAdapter: CustomAdapter

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

        listView = binding.listView
        claimAdapter = CustomAdapter(requireContext(), R.layout.list_item_claim)
        listView.adapter = claimAdapter

        binding.validateClaims.setOnClickListener {
            openFetchScanQRCode()
        }

        binding.fetchClaims.setOnClickListener {
            fetchClaims()
        }

        binding.refresh.setOnRefreshListener {
            fetchClaims()
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

        viewModel.fetchStatus.observe(requireActivity()) { isFetched ->
            binding.progress.visibility = View.GONE
        }

        viewModel.eventClaimsLiveData.observe(viewLifecycleOwner) { claims ->

            claimAdapter.clear()
            // Handle the fetched claims list here
            claims.forEach { claim ->
                println("Fetched Claim ID: ${claim.id}")

                val credentialSubject = claim.info["credentialSubject"] as? JsonObject
                if (credentialSubject != null) {
                    val additionalInfo = credentialSubject["additional-info"].toString().trim().replace("\"", "")
                    val eventDate = credentialSubject["event-date"].toString().trim().replace("\"", "")
                    val eventLocation = credentialSubject["event-location"].toString().trim().replace("\"", "")
                    val eventName = credentialSubject["event-name"].toString().trim().replace("\"", "")

                    println("Additional Info: $additionalInfo")
                    println("Event Date: $eventDate")
                    println("Event Location: $eventLocation")
                    println("Event Name: $eventName")

                    if (eventName.isNotBlank() && eventName !== "null") {
                        val claimData = ClaimData(
                            claim.id,
                            eventName,
                            eventDate,
                            eventLocation,
                            additionalInfo
                        )
                        claimAdapter.add(claimData)
                    }
                } else {
                    // Handle the case where 'credentialSubject' is not present or is of the wrong type
                    println("credentialSubject not found or of the wrong type")
                }
            }
            binding.refresh.isRefreshing = false
            binding.progress.visibility = View.GONE
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

    fun openScanQRCode() {
        val intent = Intent(requireContext(), QRCodeScannerActivity::class.java)
        startActivityForResult(intent, AUTHENTICATE_REQUEST_CODE)
    }

    fun openFetchScanQRCode() {
        val intent = Intent(requireContext(), QRCodeScannerActivity::class.java)
        startActivityForResult(intent, ConnectionsFragment.FETCH_CREDENTIAL_REQUEST_CODE)
    }

    fun fetchClaims() {
        binding.progress.visibility = View.VISIBLE
        viewModel.getClaims(requireContext())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ConnectionsFragment.AUTHENTICATE_REQUEST_CODE
            || requestCode == ConnectionsFragment.FETCH_CREDENTIAL_REQUEST_CODE
        ) {
            if (resultCode == Activity.RESULT_OK) {
                val scanResult = data?.getStringExtra("SCAN_RESULT")
                when (requestCode) {

                    ConnectionsFragment.AUTHENTICATE_REQUEST_CODE -> {
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

    companion object {
        const val AUTHENTICATE_REQUEST_CODE = 0
        const val FETCH_CREDENTIAL_REQUEST_CODE = 1
    }

    data class ClaimData(
        val id: String,
        val eventName: String,
        val eventDate: String,
        val eventLocation: String,
        val additionalInfo: String
    )
    class CustomAdapter(context: Context, resource: Int) : ArrayAdapter<ClaimData>(context, resource) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_claim, parent, false)

            val claimData = getItem(position)

            val eventNameTextView = view.findViewById<TextView>(R.id.event_name)
            val eventDateTextView = view.findViewById<TextView>(R.id.event_date)
            val eventLocationTextView = view.findViewById<TextView>(R.id.event_location)
            val additionalInfoTextView = view.findViewById<TextView>(R.id.additional_info)

            eventNameTextView.text = "Event Name: ${claimData?.eventName}"
            eventDateTextView.text = "Event Date: ${claimData?.eventDate}"
            eventLocationTextView.text = "Event Location: ${claimData?.eventLocation}"
            additionalInfoTextView.text = "Additional Info: ${claimData?.additionalInfo}"

            return view
        }
    }
}

