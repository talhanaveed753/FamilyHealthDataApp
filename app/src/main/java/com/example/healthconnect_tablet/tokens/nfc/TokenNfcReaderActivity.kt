package com.example.healthconnect_tablet.tokens.nfc

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcV
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.healthconnect_tablet.tokens.data.TokenRemoteLogger
import com.example.healthconnect_tablet.ui.theme.HealthConnect_TabletTheme

class TokenNfcReaderActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private var userId: String = ""
    private var familyName: String? = null
    private var stepsLimit: Int = 0
    private var sleepLimit: Int = 0
    private var heartLimit: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        familyName = intent.getStringExtra(EXTRA_FAMILY_NAME)
        stepsLimit = intent.getIntExtra(EXTRA_STEPS_LIMIT, 0)
        sleepLimit = intent.getIntExtra(EXTRA_SLEEP_LIMIT, 0)
        heartLimit = intent.getIntExtra(EXTRA_HEART_LIMIT, 0)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            HealthConnect_TabletTheme {
                val messageState = remember { mutableStateOf("Hold the NFC token near the device") }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Scan NFC Token") },
                            navigationIcon = {
                                IconButton(onClick = {
                                    setResult(RESULT_CANCELED)
                                    finish()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close"
                                    )
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = messageState.value,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { showWirelessSettings() },
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text(text = "Open NFC Settings")
                        }
                    }
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        val a = NfcA.get(tag)
        if (a != null) try {
            a.connect(); a.close()
        } catch (_: Exception) {
        }
        val v = NfcV.get(tag)
        if (v != null) try {
            v.connect(); v.close()
        } catch (_: Exception) {
        }

        val ndef = Ndef.get(tag)
        if (ndef == null) {
            finishWithMessage("This tag does not support NDEF.")
            playBeep()
            return
        }

        try {
            ndef.connect()
            val msg: NdefMessage = ndef.ndefMessage ?: run {
                finishWithMessage("Empty tag")
                return
            }
            val result = TokenScanService.processOnceWithLimits(
                context = this,
                userId = userId,
                familyName = familyName,
                message = msg,
                stepsLimit = stepsLimit,
                sleepLimit = sleepLimit,
                heartLimit = heartLimit
            ) { scan ->
                familyName?.let { family ->
                    TokenRemoteLogger.saveScan(family, scan)
                }
            }
            finishWithMessage(result ?: "No JSON token found on tag.")
            playBeep()
        } catch (e: Exception) {
            finishWithMessage("NFC read error: ${e.message}")
        } finally {
            try {
                ndef.close()
            } catch (_: Exception) {
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null) {
            if (!nfcAdapter!!.isEnabled) {
                showWirelessSettings()
            }
            val options = Bundle().apply {
                putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            }
            nfcAdapter!!.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options
            )
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun finishWithMessage(message: String) {
        val data = Intent().apply {
            putExtra(EXTRA_SCAN_RESULT, message)
        }
        setResult(RESULT_OK, data)
        finish()
    }

    private fun showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show()
        startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    private fun playBeep() {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100).startTone(ToneGenerator.TONE_CDMA_PIP, 150)
    }

    companion object {
        const val EXTRA_USER_ID = "person_id"
        const val EXTRA_FAMILY_NAME = "family_name"
        const val EXTRA_STEPS_LIMIT = "steps_limit"
        const val EXTRA_SLEEP_LIMIT = "sleep_limit"
        const val EXTRA_HEART_LIMIT = "heart_limit"
        const val EXTRA_SCAN_RESULT = "scan_result_message"
    }
}
