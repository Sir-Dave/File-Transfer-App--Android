package com.sirdave.filetransferapp

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class Client(var context: Context) : AsyncTask<String?, Void?, Socket?>() {
    private val TAG = "AppDebug"
    var clientSocket: Socket? = null
    private lateinit var progressDialog: ProgressDialog
    var host: String? = null
    private var port: String? = null

    override fun doInBackground(vararg p0: String?): Socket? {
        host = p0[0]
        port = p0[1]
        try {
            Log.d(TAG, "Trying: $host:$port")
            clientSocket = Socket()
            clientSocket!!.connect(InetSocketAddress(host, port!!.toInt()), 5000)
            Log.d(TAG, "Socket Connected")
            return clientSocket
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPreExecute() {
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Checking Connection")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.isIndeterminate = true
        progressDialog.setMessage("Please wait while we try to establish a connection with the server...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
    }

    override fun onPostExecute(socket: Socket?) {
        if (socket == null) {
            progressDialog.dismiss()
            AlertDialog.Builder(context)
                .setTitle("Connection Failure")
                .setMessage("Make sure your PC Server Agent is connected to the same network and recheck ip and port.")
                .setIcon(R.drawable.warning)
                .setPositiveButton(android.R.string.yes) { _, _ -> }.show()
        } else {
            val intent = Intent(context, SendReceiveFileActivity::class.java)
            try {
                clientSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            intent.putExtra("ip", host)
            intent.putExtra("port", port)
            context.startActivity(intent)
        }
    }
}