package com.example.ecodexandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class SpeciesAdapter(
    private var items: List<Species>,
    private val isFavorite: (Species) -> Boolean,
    private val onToggleFavorite: (Species) -> Unit,
    private val onItemClick: (Species) -> Unit
) : RecyclerView.Adapter<SpeciesAdapter.SpeciesViewHolder>() {

    inner class SpeciesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPlant: ImageView = view.findViewById(R.id.imgPlant)
        val tvSci: TextView = view.findViewById(R.id.tvSciName)
        val tvCommon: TextView = view.findViewById(R.id.tvCommonName)
        val btnFav: Button = view.findViewById(R.id.btnFav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeciesViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_species, parent, false)
        return SpeciesViewHolder(v)
    }

    override fun onBindViewHolder(holder: SpeciesViewHolder, position: Int) {
        val p = items[position]
        holder.tvSci.text = p.scientificName ?: "Sin nombre científico"
        holder.tvCommon.text = p.commonName ?: "Sin nombre común"

        if (!p.imageUrl.isNullOrEmpty()) {
            holder.imgPlant.load(p.imageUrl)
        } else {
            holder.imgPlant.setImageResource(R.mipmap.ic_launcher)
        }

        val fav = isFavorite(p)
        holder.btnFav.text = if (fav) "★" else "☆"

        holder.btnFav.setOnClickListener {
            onToggleFavorite(p)
        }

        holder.itemView.setOnClickListener {
            onItemClick(p)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Species>) {
        items = newItems
        notifyDataSetChanged()
    }
}
