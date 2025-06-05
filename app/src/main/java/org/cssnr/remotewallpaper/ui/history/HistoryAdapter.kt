package org.cssnr.remotewallpaper.ui.history

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.cssnr.remotewallpaper.MainActivity.Companion.LOG_TAG
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onItemClick: (View, HistoryItem) -> Unit,
    private val onItemLongClick: (HistoryItem) -> Unit,
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private lateinit var context: Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemWrapper: LinearLayout = view.findViewById(R.id.item_wrapper)
        val itemUrl: TextView = view.findViewById(R.id.item_url)
        val itemTimestamp: TextView = view.findViewById(R.id.item_timestamp)
        val itemCode: TextView = view.findViewById(R.id.item_code)
        val itemId: TextView = view.findViewById(R.id.item_id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        //Log.d(LOG_TAG, "LOAD: $position - $data")

        // On Click
        holder.itemView.setOnClickListener {
            //val currentData = items[holder.bindingAdapterPosition]
            Log.i(LOG_TAG, "setOnClickListener: $position - $data")
            onItemClick(holder.itemView, data)
        }
        // On Long Click
        holder.itemView.setOnLongClickListener {
            Log.i(LOG_TAG, "setOnLongClickListener: $position - $data")
            onItemLongClick(data)
            true
        }

        // Data
        if (!items[position].error.isNullOrEmpty()) {
            holder.itemUrl.text = items[position].error
        } else {
            holder.itemUrl.text = items[position].url
        }
        holder.itemCode.text = items[position].status.toString()
        holder.itemId.text = items[position].id.toString()
        // Date
        val instant = Instant.ofEpochMilli(items[position].timestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val display = zonedDateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
        holder.itemTimestamp.text = display
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<HistoryItem>) {
        Log.i(LOG_TAG, "updateData: ${newItems.size}")
        items = newItems
        notifyDataSetChanged()
    }

    //@SuppressLint("NotifyDataSetChanged")
    //fun addItem(item: HistoryItem) {
    //    Log.i(LOG_TAG, "addItem: $item")
    //    items + item
    //    Log.d(LOG_TAG, "getItemCount(): ${getItemCount()}")
    //    notifyItemInserted(getItemCount())
    //}
}
