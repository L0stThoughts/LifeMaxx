package com.example.lifemaxx.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lifemaxx.R
import com.example.lifemaxx.models.Supplement

class SupplementsAdapter(private val supplements: List<Supplement>) :
    RecyclerView.Adapter<SupplementsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.supplementName)
        val stockStatus: TextView = itemView.findViewById(R.id.stockStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_supplement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val supplement = supplements[position]
        holder.name.text = supplement.name
        holder.stockStatus.text = "Stock: ${supplement.totalStock}"
    }

    override fun getItemCount() = supplements.size
}
