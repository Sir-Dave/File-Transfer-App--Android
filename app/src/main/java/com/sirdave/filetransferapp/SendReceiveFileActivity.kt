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
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
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
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null

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
                    dataOutputStream?.writeInt(count) //??
                    while (currentItem < count) {
                        val uri = data.clipData!!.getItemAt(currentItem).uri
                        val fileInfo = getDataFromUri(uri)
                        sendFiles(uri, fileInfo)

                        currentItem += 1
                    }
                }
                else if (data.data != null) {
                    val uri = data.data!!
                    val fileInfo = getDataFromUri(uri)
                    sendFiles(uri, fileInfo)
                }
            }
        }
    }

    private fun getDataFromUri(uri: Uri): Pair<String, Long>{
        val cursor = contentResolver.query(uri, null, null,
            null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)!!
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        val name = cursor.getString(nameIndex)
        val size = cursor.getLong(sizeIndex)
        cursor.close()
        return Pair(name, size)
    }

    private fun sendFiles(uri: Uri, fileInfo: Pair<String, Long>){
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = fileInfo.first
            Log.d(TAG, "File $fileName sent")
            val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)
            val fileContentBytes = ByteArray(fileInfo.second.toInt())
            inputStream!!.read(fileContentBytes)
            dataOutputStream?.writeInt(fileNameBytes.size)
            dataOutputStream?.write(fileNameBytes)
            dataOutputStream?.writeInt(fileContentBytes.size)
            dataOutputStream?.write(fileContentBytes)
            dataOutputStream?.flush()

        } catch (exception: IOException) {
            exception.printStackTrace()
        }
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

        override fun run() {
            clientSocket = Socket()
            clientSocket?.connect(port?.let { InetSocketAddress(ip, it) }, 5000)
            Log.d(TAG, "Socket Connected")

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            while (true){
                try{
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
                catch(ex: EOFException){
                    ex.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@SendReceiveFileActivity,
                            "Connection reset", Toast.LENGTH_LONG).show()
                        //startActivity(Intent(this@SendReceiveFileActivity,
                        //    MainActivity::class.java))
                        //finish()
                    }
                }
            }
        }
    }

    private fun downloadFile(fileName: String?, fileContent: ByteArray?) {
        fileName?.let {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
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