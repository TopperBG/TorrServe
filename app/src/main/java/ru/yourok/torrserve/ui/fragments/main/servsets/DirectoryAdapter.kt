package ru.yourok.torrserve.ui.fragments.main.servsets

import android.os.Build
import android.os.StatFs
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.yourok.torrserve.R
import ru.yourok.torrserve.utils.ByteFmt
import java.io.File

class DirectoryAdapter : RecyclerView.Adapter<DirectoryAdapter.ViewHolder>() {
    private var path = ""
    private var list = emptyList<String>()

    var onClick: ((String) -> Unit)? = null

    fun setPath(path: String) {
        this.path = path
        update()
    }

    fun update() {
        list = File(path).listFiles().filter {
            it.isDirectory
        }.map { it.name }.toList()
        notifyDataSetChanged()
    }

    fun getPath(): String {
        return path
    }

    fun getSize(): String {
        val stat = StatFs(path)
        val bytesAvailable = if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.JELLY_BEAN_MR2
        ) {
            stat.blockSizeLong * stat.availableBlocksLong
        } else {
            stat.blockSize.toLong() * stat.availableBlocks.toLong()
        }
        return ByteFmt.byteFmt(bytesAvailable)
    }

    fun dirUp() {
        
    }

    class ViewHolder(val view: View, val adapter: DirectoryAdapter) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                val ff = File(adapter.path, adapter.list[adapterPosition])
                adapter.setPath(ff.path)
                adapter.onClick?.invoke(ff.path)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val vi = LayoutInflater.from(parent.context).inflate(R.layout.directory_item, parent, false)
        return ViewHolder(vi, this)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.view as TextView).setText(list[position])
    }

    override fun getItemCount() = list.size
}
