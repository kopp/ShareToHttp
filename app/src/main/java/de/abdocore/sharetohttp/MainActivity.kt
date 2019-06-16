package de.abdocore.sharetohttp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Parcelable
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidhttpweb.TinyWebServer
import androidx.appcompat.app.AppCompatActivity
import java.io.*


//TODO: strange bug: smileys work some times, depending on how much characters appear in front of them
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PORT: Int = 9000;
    private val TAG: String = "ShareToHttp"

    private var sharedContent: HtmlPage? = null // content resolver parameter only available after this is constructed properly

    private fun getWebBaseDirFile(): File {
        return applicationContext.cacheDir
    }

    private fun getWebBaseDirPath(): String {
        return getWebBaseDirFile().absolutePath
    }

    private fun clearWebBaseDir() {
        getWebBaseDirFile().deleteRecursively()
    }


    // alternatively use nanohttpd -- see https://stackoverflow.com/a/18685375
    private fun startServer() {
        setServerStatus("Starting server")
        val ip = getIpAddress()
        if (ip == null) {
            setServerStatus("Unable to start server -- enable WIFI?")
        }
        else {
            TinyWebServer.startServer(ip, PORT, getWebBaseDirPath())
            setServerStatus("Server started at $ip:$PORT")
        }

    }

    private fun stopServer() {
        TinyWebServer.stopServer()
    }


    private fun setMainStatus(text: String) {
        val view = findViewById<TextView>(R.id.textMainStatus)
        view.text = text
    }

    private fun setServerStatus(text: String) {
        val view = findViewById<TextView>(R.id.textServerStatus)
        view.text = text
    }

    private fun setSharedText(text: String) {
        val view = findViewById<EditText>(R.id.textSharedContent)
        view.setText(text)
    }


    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.buttonExit -> {
                clearWebBaseDir()
                finish() // quit application
            }
            R.id.buttonSharePasted -> {
                handlePastedText()
            }
            else -> Log.e(TAG, "Button currently not handeled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedContent = HtmlPage(contentResolver, getWebBaseDirFile())

        findViewById<Button>(R.id.buttonExit).setOnClickListener(this)
        findViewById<Button>(R.id.buttonSharePasted).setOnClickListener(this)

        // check, which intent started this activity
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    handleSendText(intent) // Handle text being sent
                } else if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent) // Handle single image being sent
                }
            }
            intent?.action == Intent.ACTION_SEND_MULTIPLE
                    && intent.type?.startsWith("image/") == true -> {
                handleSendMultipleImages(intent) // Handle multiple images being sent
            }
            else -> {
                // Handle other intents, such as being started from the home screen
                handleOtherIntents(intent)
            }
        }

        startServer()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }


    //! return null if unable to get IP address -- probably no WIFI
    private fun getIpAddress(): String? {
        //return InetAddress.getLocalHost().hostAddress // probably does not work -- https://stackoverflow.com/a/35385333

        // see https://stackoverflow.com/a/6071963
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        @Suppress("DEPRECATION")
        return wifiManager?.connectionInfo?.ipAddress?.let { Formatter.formatIpAddress(it) }

    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleOtherIntents(intent: Intent) {
        setMainStatus("Share content with another app or paste text to share below.")
        sharedContent?.generateWebpage()
    }

    private fun handleSendText(intent: Intent) {
        setMainStatus("Probably text was shared")
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            // Update UI to reflect text being shared
            setMainStatus("Text was shared")
            setSharedText(it)
            sharedContent?.addText(it)
            sharedContent?.generateWebpage()
        }
    }


    private fun handlePastedText() {
        setMainStatus("Sharing pasted text")
        val view = findViewById<EditText>(R.id.textSharedContent)
        sharedContent?.clear()
        sharedContent?.addText(view.text.toString())
        sharedContent?.generateWebpage()
    }


    private fun handleSendImage(intent: Intent) {
        setMainStatus("Probably an image was shared")
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {

            if (sharedContent?.addImage(it) == true) {
                setMainStatus("image ${sharedContent?.filenameOf(it)} was shared")
                sharedContent?.generateWebpage()
                Unit
            }
            else {
                setMainStatus("Unable to share image ${sharedContent?.filenameOf(it)}")
            }
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        setMainStatus("Probably multiple images were shared")
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.let {
            setMainStatus("Multiple images where shared")
            // Update UI to reflect multiple images being shared
            var status = true
            for (parcelable in it) {
                (parcelable as? Uri)?.let {
                    val addingWorked = sharedContent?.addImage(it) ?: false
                    status = addingWorked && status
                }
            }

            if (status) {
                setMainStatus("Multiple images were shared")
            }
            else {
                setMainStatus("Error when sharing multiple images")
            }
            sharedContent?.generateWebpage()
            Unit
        }
    }

}
