package com.sirdave.filetransferapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import java.net.Socket

class SendReceiveFileActivity : AppCompatActivity() {
    private var ip: String? = null
    private var port: Int? = null
    var path: String? = null
    var clientSocket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_receive_file)

        path = intent.getStringExtra("pathToExplore")
        ip = intent.getStringExtra("ip")
        port = intent.getStringExtra("port")?.toInt()

        val browse = port?.let { Browse(this, ip!!, it) }
        browse?.execute(path)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuConnect -> {
                // Opening Connection Interface
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("reqNewConnection", true)
                startActivity(intent)
            }
            R.id.menuExitApp -> {
                AlertDialog.Builder(this)
                    .setTitle("Exit Application")
                    .setMessage("Do you really want to exit?")
                    .setIcon(R.drawable.warning)
                    .setPositiveButton(
                        R.string.yes
                    ) { _, _ -> finishAffinity() }
                    .setNegativeButton(R.string.no, null).show()
            }
        }
        return true
    }
}