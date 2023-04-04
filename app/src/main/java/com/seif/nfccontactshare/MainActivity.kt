package com.seif.nfccontactshare

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.seif.nfccontactshare.databinding.ActivityMainBinding

// Main activity class extending AppCompatActivity
class MainActivity : AppCompatActivity() {
    // Declare NFC adapter, PendingIntent and intent filters
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var ndefExchangeFilters: Array<IntentFilter>
    private lateinit var binding: ActivityMainBinding

    // onCreate method is called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the activity layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Get the default NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        // Create a PendingIntent for the NFC adapter
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE
        )

        // Create an intent filter for NDEF messages
        ndefExchangeFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType("application/com.seif.nfccontactshare")
            }
        )

        // Set an onClick listener for the share button
        binding.shareButton.setOnClickListener {
            setupNdefSharing()
        }
    }

    // Set up the NDEF message sharing
    private fun setupNdefSharing() {
        // Set the callback for creating an NDEF message
        nfcAdapter.setNdefPushMessageCallback(NfcAdapter.CreateNdefMessageCallback {
            // Get the contact information from the EditText fields
            val name = binding.userName.text.toString()
            val phoneNumber = binding.userNumber.text.toString()
            val email = binding.userEmail.text.toString()
            val address = binding.userAddress.text.toString()

            // Concatenate the contact information into a single string
            val contactInfo = "$name, $phoneNumber, $email, $address"
            // Create an NDEF record with the contact information
            val ndefRecord = NdefRecord.createMime(
                "application/com.seif.nfccontactshare", contactInfo.toByteArray()
            )
            // Create an NDEF message with the NDEF record
            val ndefMessage = NdefMessage(arrayOf(ndefRecord))
            ndefMessage
        }, this)
    }

    // onResume method is called when the activity becomes visible
    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch for the NFC adapter
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, ndefExchangeFilters, null)
    }

    // onPause method is called when the activity is no longer visible
    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch for the NFC adapter
        nfcAdapter.disableForegroundDispatch(this)
    }

    // onNewIntent method is called when the activity receives a new intent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Check if the intent action is NDEF_DISCOVERED
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            // Get the received NDEF messages
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val messages = Array(rawMessages.size) { i ->
                    rawMessages[i] as NdefMessage
                }
                // Process the received NDEF messages
                processNdefMessages(messages)
            }
        }
    }

    // Process the received NDEF messages
    private fun processNdefMessages(messages: Array<NdefMessage>) {
        // Get the first NDEF record from the first NDEF message
        val record = messages[0].records[0]
        // Get the MIME type of the NDEF record
        val mimeType = record.toMimeType()
        // Check if the MIME type matches the expected one
        if (mimeType == "application/com.seif.nfccontactshare") {
            // Get the payload of the NDEF record
            val payload = record.payload
            // Convert the payload to a contact info string
            val contactInfo = String(payload, Charsets.UTF_8)
            // Show the contact info in a dialog
            showContactInfoDialog(contactInfo)
        }
    }

    // Show the received contact info in a dialog
    private fun showContactInfoDialog(contactInfo: String) {
        // Split the contact info string into a list of values
        val contactInfoList = contactInfo.split(", ")

        // Get the individual contact info values
        val name = contactInfoList[0]
        val phoneNumber = contactInfoList[1]
        val email = contactInfoList[2]
        val address = contactInfoList[3]

        // Create a formatted message with the contact info
        val message = "Name: $name\nPhone Number: $phoneNumber\nEmail: $email\nAddress: $address"

        // Show an AlertDialog with the contact info
        AlertDialog.Builder(this)
            .setTitle("Received Contact Information")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
