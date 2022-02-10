package com.sirdave.filetransferapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toFile
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket


private const val REQUEST_EXTERNAL_STORAGE = 1
private const val CHOOSE_FILES = 1000
private const val TAG = "SendReceiveFileActivity"
class SendReceiveFileActivity : AppCompatActivity() {
    private var ip: String? = null
    private var port: Int? = null
    var clientSocket: Socket? = null
    private lateinit var myThread: MyThread
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_receive_file)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        ip = intent.getStringExtra("ip")
        port = intent.getStringExtra("port")?.toInt()

        verifyStoragePermissions(this)

        sendButton = findViewById(R.id.btnSend)

        sendButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            //intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Choose Files"), CHOOSE_FILES)
        }

        myThread = MyThread()
        Thread(myThread).start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_FILES) {
            if (resultCode == RESULT_OK) {
                if (data!!.clipData != null) {
                    val count = data.clipData!!.itemCount
                    var currentItem = 0
                    while (currentItem < count) {
                        val imageUri: Uri = data.clipData!!.getItemAt(currentItem).uri
                        Log.d(TAG, "imageUri is $imageUri")

                        currentItem += 1
                    }
                }
                else if (data.data != null) {
                    val imageUri = data.data!!
                    Log.d(TAG, "imageUri is $imageUri")
                }
            }
        }
    }

    private fun getFileFRomUri(uri: Uri){
        val inputStream = contentResolver.openInputStream(uri)
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
            Log.d(TAG, "Socket Connected")

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            while (true){
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
    }

    private fun downloadFile(fileName: String?, fileContent: ByteArray?) {
        fileName?.let {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            //val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            //file.createNewFile()
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(fileContent)
            fileOutputStream.close()
            Log.d(TAG, "File $file received")
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