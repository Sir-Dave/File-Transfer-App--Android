package com.sirdave.filetransferapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow


class FileRecyclerAdapter(private var fileList: List<FileHandler>):
    RecyclerView.Adapter<FileRecyclerAdapter.RecyclerViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_row, parent, false)
        return RecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        val file = fileList[position]
        holder.name.text = file.name
        holder.size.text = formatFileSize(file.size)
        holder.path.text = file.path
        holder.parent.setOnClickListener {
            //open the file based on the type
        }
    }

    private fun formatFileSize(size: Int): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))
            .toString() + " " + units[digitGroups]
    }

    override fun getItemCount(): Int = fileList.size

    inner class RecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var icon: ImageView = itemView.findViewById(R.id.fileIcon)
        var name: TextView = itemView.findViewById(R.id.fileName)
        var size: TextView = itemView.findViewById(R.id.fileSize)
        var path: TextView = itemView.findViewById(R.id.filePath)
        var parent: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)
    }
}