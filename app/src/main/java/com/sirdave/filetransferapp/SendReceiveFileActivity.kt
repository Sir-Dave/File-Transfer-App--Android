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
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import kotlin.math.min
import kotlin.math.round


private const val REQUEST_EXTERNAL_STORAGE = 1
private const val CHOOSE_FILES = 1000
private const val TAG = "SendReceiveFileActivity"
class SendReceiveFileActivity : AppCompatActivity() {
    private var ip: String? = null
    private var port: Int? = null
    var clientSocket: Socket? = null
    private lateinit var myThread: MyThread
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentage: TextView
    private var dataInputStream: DataInputStream? = null
    private var dataOutputStream: DataOutputStream? = null
    private val allFiles = ArrayList<FileHandler>()
    private var timeSentTaken = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_receive_file)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        ip = intent.getStringExtra("ip")
        port = intent.getStringExtra("port")?.toInt()

        verifyStoragePermissions(this)
        recyclerView = findViewById(R.id.file_recycler_view)
        progressBar = findViewById(R.id.progressBar)
        progressPercentage = findViewById(R.id.progressPercentage)

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

    private fun setUpRecyclerView(fileList: List<FileHandler>) {
        Log.d(TAG, "setUpRecyclerView started")
        val adapter = FileRecyclerAdapter(fileList)
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager
        adapter.notifyItemInserted(allFiles.size - 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_FILES) {
            if (resultCode == RESULT_OK) {
                if (data!!.clipData != null) {
                    val count = data.clipData!!.itemCount
                    var currentItem = 0
                    while (currentItem < count) {
                        val uri = data.clipData!!.getItemAt(currentItem).uri
                        val fileInfo = getDataFromUri(uri)
                        sendFile(uri, fileInfo)

                        currentItem += 1
                    }
                    showFileTransferCompletedDialog(count, timeSentTaken)
                    timeSentTaken = 0.0 // reset the time taken to 0
                }
                else if (data.data != null) {
                    val uri = data.data!!
                    val fileInfo = getDataFromUri(uri)
                    sendFile(uri, fileInfo)
                    showFileTransferCompletedDialog(1, timeSentTaken)
                    timeSentTaken = 0.0 // reset the time taken to 0
                }
            }
        }
    }

    private fun getDataFromUri(uri: Uri): Pair<String, Long> {
        val cursor = contentResolver.query(
            uri, null, null,
            null, null
        )
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)!!
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        val name = cursor.getString(nameIndex)
        val size = cursor.getLong(sizeIndex)
        cursor.close()
        return Pair(name, size)
    }


    private fun sendFile(uri: Uri, fileInfo: Pair<String, Long>) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bytesIn = ByteArray(4096)
            var read: Int

            var total = 0
            var progress: Double

            val fileName = fileInfo.first
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "File $fileName sent")

            // send file name
            val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)
            dataOutputStream!!.writeInt(fileNameBytes.size)
            dataOutputStream!!.write(fileNameBytes)

            // send file size
            dataOutputStream!!.writeLong(fileInfo.second)

            while (inputStream!!.read(bytesIn).also { read = it } != -1) {
                dataOutputStream!!.write(bytesIn, 0, read)

                total += read
                progress = (total * 100.0 / fileInfo.second)
                runOnUiThread {
                    updateProgressBar(round(progress).toInt())
                }

                dataOutputStream!!.flush()

                val estimatedTime = System.currentTimeMillis() - startTime
                val estimatedTimeInSecs = estimatedTime / 1000.0
                timeSentTaken += estimatedTimeInSecs
            }
            inputStream.close()
            hideProgressBar()


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

    inner class MyThread : Runnable {

        override fun run() {
            clientSocket = Socket()
            clientSocket?.connect(port?.let { InetSocketAddress(ip, it) }, 5000)
            Log.d(TAG, "Socket Connected")

            dataInputStream = DataInputStream(clientSocket?.getInputStream())
            dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())

            while (true) {
                try {
                    val fileNameLength = dataInputStream?.readInt()

                    if (fileNameLength!! > 0) {
                        val fileNameBytes = ByteArray(fileNameLength)
                        dataInputStream?.readFully(fileNameBytes, 0, fileNameLength)
                        val filename = String(fileNameBytes, StandardCharsets.UTF_8)

                        var bytes = 0

                        val file = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            filename
                        )
                        val fileOutputStream = FileOutputStream(file)

                        var size = dataInputStream!!.readLong() // read file size
                        val originalSize = size

                        var total = 0
                        var progress: Double

                        val buffer = ByteArray(4096)
                        while (size > 0 && dataInputStream!!.read(buffer, 0,
                                min(buffer.size.toLong(), size).toInt()
                            ).also { bytes = it } != -1) {
                            fileOutputStream.write(buffer, 0, bytes)
                            size -= bytes // read up to file size

                            total += bytes
                            progress = (total * 100.0 / originalSize)
                            runOnUiThread {
                                updateProgressBar(round(progress).toInt())
                            }
                        }

                        val fileHandler = FileHandler(filename, size.toInt(), file.absolutePath)
                        allFiles.add(fileHandler)
                        fileOutputStream.close()

                        runOnUiThread {
                            setUpRecyclerView(allFiles.asReversed())
                            hideProgressBar()
                        }
                    }
                } catch (ex: EOFException) {
                    ex.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(
                            this@SendReceiveFileActivity,
                            "Connection reset", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }


    private fun verifyStoragePermissions(activity: Activity?) {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_EXTERNAL_STORAGE)
        }
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        progressPercentage.visibility = View.GONE
    }

    private fun updateProgressBar(progress: Int){
        if (progressBar.visibility == View.GONE){
            progressBar.visibility = View.VISIBLE
        }

        if (progressPercentage.visibility == View.GONE){
            progressPercentage.visibility = View.VISIBLE
        }
        progressBar.progress = progress
        progressPercentage.text = resources.getString(R.string.processing,
            "$progress%")
    }

    private fun showFileTransferCompletedDialog(number: Int, timeTaken: Double){
        val df = DecimalFormat("0.00");
        AlertDialog.Builder(this)
            .setTitle("Files Sent")
            .setMessage("""
                $number file(s) successfully transferred
                Time Taken: ${df.format(timeTaken)}s
                """.trimIndent())
            .setIcon(R.drawable.success)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}