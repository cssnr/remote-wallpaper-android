package org.cssnr.remotewallpaper.ui.remotes

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import org.cssnr.remotewallpaper.R
import org.cssnr.remotewallpaper.db.Remote

class RemotesAdapter(
    private var items: List<Remote>,
    private val onItemClick: (Remote) -> Unit,
    private val onItemLongClick: (Remote) -> Unit,
) :
    RecyclerView.Adapter<RemotesAdapter.ViewHolder>() {

    private lateinit var context: Context

    private val selectedUrls = mutableSetOf<String>()

    fun isAllSelected(): Boolean = items.isNotEmpty() && selectedUrls.size == items.size

    val hasSelection: Boolean
        get() = selectedUrls.isNotEmpty()

    val selectedCount: Int
        get() = selectedUrls.size

    val selected: List<String>
        get() = selectedUrls.toList()

    fun toggleSelection(url: String) {
        if (!selectedUrls.add(url)) {
            selectedUrls.remove(url)
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        if (selectedUrls.isNotEmpty()) {
            selectedUrls.clear()
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelectAll() {
        if (isAllSelected()) {
            selectedUrls.clear()
        } else {
            selectedUrls.addAll(items.map { it.url })
        }
        notifyDataSetChanged()
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

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
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
        holder.propertiesName.text = items[position].url

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

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<Remote>) {
        Log.i(LOG_TAG, "updateData: ${newItems.size}")
        items = newItems
        notifyDataSetChanged()
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun addItem(item: Remote) {
//        Log.i(LOG_TAG, "addItem: $item")
//        items + item
//        Log.d(LOG_TAG, "getItemCount(): ${getItemCount()}")
//        notifyItemInserted(getItemCount())
//    }
}
