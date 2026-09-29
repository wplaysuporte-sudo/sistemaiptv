package com.boyprovod.player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class MediaAdapter(
    private var items: List<IptvItem>,
    private val onClick: (IptvItem) -> Unit
) : RecyclerView.Adapter<MediaAdapter.Holder>() {

    fun submit(newItems: List<IptvItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ImageView = view.findViewById(R.id.itemLogo)
        val title: TextView = view.findViewById(R.id.itemTitle)
        val category: TextView = view.findViewById(R.id.itemCategory)
        val type: TextView = view.findViewById(R.id.itemType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media_card, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.category.text = item.group.ifBlank { "Sem categoria" }
        holder.type.text = item.kind.label
        holder.logo.load(item.logo.takeIf { it.isNotBlank() }) {
            crossfade(true)
            placeholder(android.R.drawable.ic_media_play)
            error(android.R.drawable.ic_media_play)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size
}