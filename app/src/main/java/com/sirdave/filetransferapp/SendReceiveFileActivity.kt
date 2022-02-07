package com.sirdave.filetransferapp

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket


private const val REQUEST_EXTERNAL_STORAGE = 1
class SendReceiveFileActivity : AppCompatActivity() {
    private var ip: String? = null
    private var port: Int? = null
    var clientSocket: Socket? = null
    private lateinit var myThread: MyThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_receive_file)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        ip = intent.getStringExtra("ip")
        port = intent.getStringExtra("port")?.toInt()

        verifyStoragePermissions(this)

        myThread = MyThread()
        Thread(myThread).start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

    inner class MyThread: Runnable {
        private var dataInputStream: DataInputStream? = null
        private var dataOutputStream: DataOutputStream? = null

        override fun run() {
            clientSocket = Socket()
            clientSocket?.connect(port?.let { InetSocketAddress(ip, it) }, 5000)
            Log.d("SendReceiveFileActivity", "Socket Connected")

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())
            val fileNameLength = dataInputStream?.readInt()

            if (fileNameLength!! > 0){
                val fileNameBytes = ByteArray(fileNameLength)
                dataInputStream?.readFully(fileNameBytes, 0, fileNameLength)
                val filename = String(fileNameBytes)

                val fileContentLength = dataInputStream?.readInt()
                if (fileContentLength!! > 0){
                    val fileContent = ByteArray(fileContentLength)
                    dataInputStream?.readFully(fileContent, 0, fileContentLength)
                    downloadFile(filename, fileContent)
                }
            }
        }
    }

    private fun downloadFile(fileName: String?, fileContent: ByteArray?) {
        fileName?.let {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            //val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            //file.createNewFile()
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(fileContent)
            fileOutputStream.close()
            Log.d("SendReceiveFileActivity",
                "File $file received")
        }
    }


    private fun verifyStoragePermissions(activity: Activity?) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permission = ActivityCompat.checkSelfPermission(activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_EXTERNAL_STORAGE)
        }
    }
}