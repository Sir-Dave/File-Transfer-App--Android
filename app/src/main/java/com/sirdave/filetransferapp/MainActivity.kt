package com.sirdave.filetransferapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var ipField: EditText
    private lateinit var portField: EditText
    private lateinit var connectBtn: Button
    private var ip: String? = null
    private var port: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ipField = findViewById(R.id.txtIP)
        portField = findViewById(R.id.txtPort)
        connectBtn = findViewById(R.id.btnConnect)

        connectBtn.setOnClickListener {
            ip = ipField.text.toString()
            port = portField.text.toString()
            if ((ip != "" && port != "")) {
                val client = Client(this)
                client.execute(ip, port)
            }
        }
    }
}