package com.sirdave.filetransferapp

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import androidx.appcompat.app.AlertDialog
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.text.DecimalFormat
import kotlin.math.roundToInt

open class Download(var context: Context, var ip: String, var port: Int) :
    AsyncTask<String?, Int?, Boolean>() {

    companion object {
        const val DOWNLOAD_REQUEST = "::1"
        const val FTP_DOWNLOAD_REQUEST = "::3"
        const val BUFFER_SIZE = 1024 * 20
        const val BUFFER_SIZE_FTP = 1024000
        const val TCP_SIZE_LIMIT = 8000000
        private val df = DecimalFormat("0.00")
    }

    private lateinit var progressDialog: ProgressDialog
    var ftp: FTPClient? = null
    var fileName: String? = null
    var filePath: String? = null
    var downloadPath: String? = null
    var pathToSave: String? = null
    var fileSize: Long? = 0
    var timeTaken = 0.0
    var s: Socket? = null


    private fun safeMin(a: Long, b: Long): Int { // Considers the min value will always be in int range as buffer_size is int
        return if (a < b) a.toInt() else b.toInt()
    }

    override fun doInBackground(vararg p0: String?): Boolean? {
        fileName = p0[0]
        filePath = p0[1]
        fileSize = p0[2]?.toLong()
        val dInputStream: DataInputStream?
        val dataOutputStream: DataOutputStream?
        try {
            s = Socket()
            s!!.connect(InetSocketAddress(ip, Integer.valueOf(port)), 5000)
            dInputStream = DataInputStream(s!!.getInputStream())
            dataOutputStream = DataOutputStream(s!!.getOutputStream())
            downloadPath = Environment.getExternalStorageDirectory()
                .toString() + File.separator + "File Transfer App"
            val folder = File(downloadPath!!)
            println(folder.listFiles())
            if (!folder.exists()) {
                println("Folder Created: " + folder.mkdirs())
            }
            pathToSave = downloadPath + File.separator + fileName
            var ftpDownloadStatus = false
            //            progressDialog.setTitle("Download Started");
            if (fileSize!! > TCP_SIZE_LIMIT) { // TODO: Gotta FIX FTP Speed. ONLY THEN I CAN USE THIS BLOCK
                dataOutputStream.writeUTF(FTP_DOWNLOAD_REQUEST + filePath)
                val ftpPort =
                    dInputStream.readUTF() // Should return port number of server; -1 is failure
                if (ftpPort != "-1") {
                    progressDialog.setMessage("Downloading using FTP connection")
                    val startTime = System.currentTimeMillis()
                    try {
                        ftpDownloader(ip, Integer.valueOf(ftpPort), "admin", "admin")
                        downloadFileFTP(fileName, pathToSave)
                        disconnect()
                        dataOutputStream.writeUTF("true") // Download Success
                        ftpDownloadStatus = true
                        val estimatedTime = System.currentTimeMillis() - startTime
                        timeTaken = estimatedTime / 1000.0
                        s!!.close()
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (!ftpDownloadStatus) {
                try {
                    progressDialog.setMessage("Downloading using TCP connecttion")
                    dataOutputStream.writeUTF(DOWNLOAD_REQUEST + filePath)
                    var fos: FileOutputStream? = null
                    fos = FileOutputStream(pathToSave)
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    var totalRead = 0
                    var remaining: Long = fileSize!!
                    var completed: Double
                    publishProgress(0)
                    val startTime = System.currentTimeMillis()
                    while (dInputStream.read(buffer, 0, safeMin(buffer.size.toLong(), remaining))
                            .also { read = it } > 0
                    ) {
                        totalRead += read
                        remaining -= read.toLong()
                        completed = totalRead * 100.0 / fileSize!!
                        publishProgress(Math.round(completed).toInt())
                        fos.write(buffer, 0, read)
                    }
                    val estimatedTime = System.currentTimeMillis() - startTime
                    timeTaken = estimatedTime / 1000.0
                    fos.close()
                    s!!.close()
                    return true
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        values[0]?.let { progressDialog.setProgress(it) }
        progressDialog.setTitle("Downloading...")
        progressDialog.setMessage("Downloading file: $filePath")
    }

    override fun onPreExecute() {
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Trying to establish a connection...")
        progressDialog.setTitle("Starting Download...")
        progressDialog.max = 100
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.isIndeterminate = false
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, _ ->
            try {
                this@Download.cancel(true)
                println("OK DELETING")
                if (ftp != null) {
                    disconnect() // To close ftp if exists
                }
                dialog.dismiss()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    val file = File(pathToSave)
                    if (file.delete()) {
                        println("Deleted Incomplete Download")
                    } else {
                        println("Couldn't Delete Incomplete Download")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        progressDialog.show()
    }

    override fun onPostExecute(result: Boolean) {
        progressDialog.dismiss()
        if (result) {
            AlertDialog.Builder(context)
                .setTitle("Download Completed")
                .setMessage(
                    """
                    File Downloaded Successfully
                    Time Taken: ${df.format(timeTaken)}s
                    Avg. Transfer Rate: ${df.format(fileSize!! * 0.000001 / timeTaken)}MBps
                    """.trimIndent()
                )
                .setIcon(R.drawable.success)
                .setPositiveButton("Show Files"
                ) { _, _ ->
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    val uri = Uri.parse(downloadPath) // a directory
                    intent.setDataAndType(uri, "*/*")
                    context.startActivity(Intent.createChooser(intent, "Open Folder"))
                }.setNegativeButton("CLOSE", null).show()
        } else {
            AlertDialog.Builder(context)
                .setTitle("Download Failed")
                .setMessage("Couldn't complete downloading. Please try again.")
                .setIcon(R.drawable.warning)
                .setNegativeButton("CLOSE", null).show()
        }
    }


    private fun ftpDownloader(host: String, port: Int, user: String, pwd: String) {
        ftp = FTPClient()
        ftp?.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
        ftp?.connect(host, port)
        ftp?.bufferSize = BUFFER_SIZE_FTP
        val reply: Int = ftp?.replyCode!!
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp?.disconnect()
            throw Exception("Exception in connecting to FTP Server")
        }
        ftp?.login(user, pwd)
        ftp?.setFileType(FTP.BINARY_FILE_TYPE)
        ftp?.enterLocalActiveMode()
    }

    private fun downloadFileFTP(remoteFilePath: String?, localFilePath: String?) {
        // APPROACH #2: using InputStream retrieveFileStream(String)
        try {
            val downloadFile2 = File(localFilePath!!)
            val outputStream2: OutputStream = BufferedOutputStream(FileOutputStream(downloadFile2))
            val inputStream: InputStream = ftp?.retrieveFileStream(remoteFilePath)!!
            val bytesArray = ByteArray(BUFFER_SIZE_FTP)
            var total = 0
            var bytesRead: Int
            var completed: Double
            while (inputStream.read(bytesArray).also { bytesRead = it } != -1) {
                outputStream2.write(bytesArray, 0, bytesRead)
                total += bytesRead
                completed = total * 100.0 / fileSize!!
                publishProgress(completed.roundToInt())
            }
            val success: Boolean = ftp?.completePendingCommand()!!
            if (success) {
                println("File has been downloaded successfully.")
            }
            outputStream2.close()
            inputStream.close()
        } catch (ex: IOException) {
            println("Error: " + ex.message)
            ex.printStackTrace()
        } finally {
            try {
                if (ftp?.isConnected!!) {
                    ftp?.logout()
                    ftp?.disconnect()
                }
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
    }

    private fun disconnect() {
        if (ftp?.isConnected!!) {
            try {
//                this.ftp.logout();
                ftp?.disconnect()
                ftp = null
            } catch (f: IOException) {
                // do nothing as file is already downloaded from FTP server
            }
        }
    }
}