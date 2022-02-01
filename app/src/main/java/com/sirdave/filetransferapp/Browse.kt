package com.sirdave.filetransferapp

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.AsyncTask
import androidx.appcompat.app.AlertDialog
import org.json.JSONArray
import org.json.JSONException
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.collections.ArrayList

class Browse(private val context: Context, var ip: String, var port: Int) :
    AsyncTask<String?, Void?, ArrayList<FileHandler>?>() {
    val BROWSE_REQUEST = "::2"

    private lateinit var progressDialog: ProgressDialog

    override fun doInBackground(vararg p0: String?): ArrayList<FileHandler>? {
        val dir = p0[0] // Directory of root to be browsed
        var s: Socket? = null
        try {
            s = Socket()
            s.connect(InetSocketAddress(ip, Integer.valueOf(port)), 5000)
            val fileList: JSONArray
            val files = ArrayList<FileHandler>()
            val dOutputStream = DataOutputStream(s.getOutputStream())
            val dataInputStream = DataInputStream(s.getInputStream())
            val bufferedReader = BufferedReader(InputStreamReader(dataInputStream))
            println("Requesting Files...")
            dOutputStream.writeUTF(BROWSE_REQUEST + dir)
            println("File List...")
            fileList = JSONArray(bufferedReader.readLine())
            val numberOfItems = fileList.length()
            for (i in 0 until numberOfItems) {
                files.add(
                    FileHandler(
                        fileList.getJSONObject(i)["name"].toString(),
                        fileList.getJSONObject(i)["type"].toString(),
                        fileList.getJSONObject(i)["path"].toString(),
                        java.lang.Long.valueOf(fileList.getJSONObject(i)["size"].toString())
                    )
                )
            }
            s.close()
            return files
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPreExecute() {
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Loading Data")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.isIndeterminate = true
        progressDialog.setMessage("Please wait while we finish loading...")
        progressDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "Cancel"
        ) { dialog, _ ->
            cancel(true)
            dialog.dismiss()
        }
        progressDialog.show()
    }

    override fun onPostExecute(list: ArrayList<FileHandler>?) {
        progressDialog.dismiss()
        if (list == null) {
            AlertDialog.Builder(context)
                .setTitle("Connection Failure")
                .setMessage("Make sure your PC Server Agent is connected to the same network and recheck ip and port.")
                .setIcon(R.drawable.warning)
                .setPositiveButton(android.R.string.yes) { _, _ -> }.show()
        }
    }
}