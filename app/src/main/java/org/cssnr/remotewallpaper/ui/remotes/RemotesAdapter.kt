package org.cssnr.remotewallpaper.ui.remotes

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
import com.google.android.material.color.MaterialColors
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.Remote

class RemotesAdapter(
    private val onItemClick: (Remote) -> Unit,
    private val onItemLongClick: (Remote) -> Unit,
) : ListAdapter<Remote, RemotesAdapter.ViewHolder>(DiffCallback) {

    private lateinit var context: Context

    private val selectedUrls = mutableSetOf<String>()

    fun isAllSelected(): Boolean =
        currentList.isNotEmpty() && selectedUrls.size == currentList.size

    val hasSelection: Boolean
        get() = selectedUrls.isNotEmpty()

    val selected: List<String>
        get() = selectedUrls.toList()

    fun toggleSelection(url: String) {
        val position = currentList.indexOfFirst { it.url == url }
        if (!selectedUrls.add(url)) {
            selectedUrls.remove(url)
        }
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }

    fun clearSelection() {
        if (selectedUrls.isNotEmpty()) {
            selectedUrls.clear()
            notifyItemRangeChanged(0, currentList.size)
        }
    }

    fun toggleSelectAll() {
        if (isAllSelected()) {
            selectedUrls.clear()
        } else {
            selectedUrls.addAll(currentList.map { it.url })
        }
        notifyItemRangeChanged(0, currentList.size)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemCard: MaterialCardView = view.findViewById(R.id.item_card)
        val propertiesName: TextView = view.findViewById(R.id.properties_name)
        //val propertiesID: TextView = view.findViewById(R.id.properties_id)
        //val propertiesElevation: TextView = view.findViewById(R.id.properties_elevation)
        //val propertiesCoordinates: TextView = view.findViewById(R.id.properties_coordinates)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_remote, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        //Log.d(LOG_TAG, "LOAD: $position - $data")

        // On Click
        holder.itemView.setOnClickListener {
            //val currentData = items[holder.bindingAdapterPosition]
            Log.i(LOG_TAG, "setOnClickListener: $position - $data")
            onItemClick(data)
        }
        holder.itemView.setOnLongClickListener {
            Log.i(LOG_TAG, "setOnLongClickListener: $position - $data")
            onItemLongClick(data)
            true
        }

        // URL
        holder.propertiesName.text = data.url

        // Selected (show checkmark)
        holder.itemCard.isChecked = data.url in selectedUrls

        // Active (fill tile background)
        holder.itemCard.setCardBackgroundColor(
            MaterialColors.getColor(
                context,
                if (data.active) {
                    com.google.android.material.R.attr.colorSecondaryContainer
                } else {
                    com.google.android.material.R.attr.colorSurface
                },
                0
            )
        )
    }

    fun updateData(newItems: List<Remote>, onCommitted: (() -> Unit)? = null) {
        Log.i(LOG_TAG, "updateData: ${newItems.size}")
        selectedUrls.retainAll(newItems.map { it.url }.toSet())
        submitList(newItems) { onCommitted?.invoke() }
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun addItem(item: Remote) {
//        Log.i(LOG_TAG, "addItem: $item")
//        items + item
//        Log.d(LOG_TAG, "getItemCount(): ${getItemCount()}")
//        notifyItemInserted(getItemCount())
//    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Remote>() {
            override fun areItemsTheSame(oldItem: Remote, newItem: Remote): Boolean =
                oldItem.url == newItem.url

            override fun areContentsTheSame(oldItem: Remote, newItem: Remote): Boolean =
                oldItem.url == newItem.url && oldItem.active == newItem.active
        }
    }
}
