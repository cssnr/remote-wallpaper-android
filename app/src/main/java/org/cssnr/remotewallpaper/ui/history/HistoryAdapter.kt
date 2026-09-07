package org.cssnr.remotewallpaper.ui.history

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.HistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (View, HistoryItem) -> Unit,
    private val onItemLongClick: (HistoryItem) -> Unit,
) : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(DiffCallback) {

    private lateinit var context: Context

    private val selectedIds = mutableSetOf<Long>()

    fun isAllSelected(): Boolean =
        currentList.isNotEmpty() && selectedIds.size == currentList.size

    val hasSelection: Boolean
        get() = selectedIds.isNotEmpty()

    val selected: List<Long>
        get() = selectedIds.toList()

    fun toggleSelection(id: Long) {
        val position = currentList.indexOfFirst { it.id == id }
        if (!selectedIds.add(id)) {
            selectedIds.remove(id)
        }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    fun clearSelection() {
        if (selectedIds.isNotEmpty()) {
            selectedIds.clear()
            notifyItemRangeChanged(0, currentList.size)
        }
    }

    fun toggleSelectAll() {
        if (isAllSelected()) {
            selectedIds.clear()
        } else {
            selectedIds.addAll(currentList.map { it.id })
        }
        notifyItemRangeChanged(0, currentList.size)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemCard: MaterialCardView = view.findViewById(R.id.item_card)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
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
        if (!data.error.isNullOrEmpty()) {
            holder.itemUrl.text = data.error
        } else {
            holder.itemUrl.text = data.url
        }
        holder.itemCode.text = String.format(Locale.getDefault(), "%d", data.status)
        holder.itemId.text = String.format(Locale.getDefault(), "%d", data.id)
        // Date
        val instant = Instant.ofEpochMilli(data.timestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val display = zonedDateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
        holder.itemTimestamp.text = display

        // Selected (show checkmark)
        holder.itemCard.isChecked = data.id in selectedIds
    }

    fun updateData(newItems: List<HistoryItem>) {
        Log.i(LOG_TAG, "updateData: ${newItems.size}")
        submitList(newItems)
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun addItem(item: HistoryItem) {
//        Log.i(LOG_TAG, "addItem: $item")
//        items + item
//        Log.d(LOG_TAG, "getItemCount(): ${getItemCount()}")
//        notifyItemInserted(getItemCount())
//    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
                oldItem.url == newItem.url &&
                        oldItem.status == newItem.status &&
                        oldItem.error == newItem.error &&
                        oldItem.timestamp == newItem.timestamp
        }
    }
}
