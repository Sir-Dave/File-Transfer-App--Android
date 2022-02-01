package com.sirdave.filetransferapp


import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

private const val REQUEST_EXTERNAL_STORAGE = 1
private const val SHARED_PREF = "fileTransferAppPref"
private const val REGEX = "^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?"

class MainActivity : AppCompatActivity() {
    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var connectBtn: Button
    private var ip: String? = null
    private var port: String? = null

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyStoragePermissions(this)
        ipField = findViewById(R.id.txtIP)
        portField = findViewById(R.id.txtPort)
        connectBtn = findViewById(R.id.btnConnect)

        // Setting data from shared preference
        /**val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )
        val prefIP = sharedPreferences.getString("IP", "")
        val prefPort = sharedPreferences.getString("PORT", "")
        println("Test$prefIP")*/

        // Setting preffered values
        //ipField.setText(prefIP)
        //portField.setText(prefPort)

        // ipField only takes valid input like XXX.XXX.XXX.XXX
        /**val filters = arrayOfNulls<InputFilter>(1)
        filters[0] = InputFilter { source, start, end, dest, dstart, dend ->
            if (end > start) {
                val destTxt = dest.toString()
                val resultingTxt = destTxt.substring(0, dstart) +
                        source.subSequence(start, end) + destTxt.substring(dend)
                if (!resultingTxt.matches(Regex(REGEX))) {
                    return@InputFilter ""
                }
                else {
                    val splits = resultingTxt.split("\\.").toTypedArray()
                    Log.d("MainActivity", "splits is $splits")
                    for (i in splits.indices) {
                        if (Integer.valueOf(splits[i]) > 255) {
                            return@InputFilter ""
                        }
                    }
                }
            }
            null
        }*/

        //ipField.filters = filters
        val newConnectionRequest = intent.getBooleanExtra("reqNewConnection", false)
        connectBtn.setOnClickListener {
            ip = ipField.text.toString()
            port = portField.text.toString()
            if (!(ip == "" || port == "")) {
                // Saving data automatically
                /**val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                    applicationContext
                )
                val editor = sharedPreferences.edit()
                editor.putString("IP", ip)
                editor.putString("PORT", port)
                editor.apply()*/
                val client = Client(this@MainActivity)
                client.execute(ip, port)
            }
        }
        if (!newConnectionRequest) { // Only will be initiated if MainActivity is not called in runtime
            // Trying initially to connect
            ip = ipField.getText().toString()
            port = portField.getText().toString()
            if (!(ip == "" || port == "")) {
                Client(this).execute(ip, port)
            }
        }
    }

    private fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}