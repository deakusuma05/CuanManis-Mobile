package com.example.cuanmanis.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cuanmanis.R
import com.example.cuanmanis.model.Barang

class BarangAdapter(
    private var list: MutableList<Barang> = mutableListOf(),
    private val onEditClick: (Barang) -> Unit,
    private val onDeleteClick: (Barang) -> Unit
) : RecyclerView.Adapter<BarangAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama   = view.findViewById<TextView>(R.id.tv_nama_barang)
        val tvHarga  = view.findViewById<TextView>(R.id.tv_harga_barang)
        val tvDetail = view.findViewById<TextView>(R.id.tv_lokasi_kategori)
        val btnEdit  = view.findViewById<Button>(R.id.btnEdit)
        val btnHapus = view.findViewById<Button>(R.id.btnHapus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_posted_barangku, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barang = list[position]
        holder.tvNama.text   = barang.nama
        holder.tvHarga.text  = "Rp ${barang.harga}"
        holder.tvDetail.text = "${barang.lokasi} | ${barang.kategori}"

        holder.btnEdit.setOnClickListener {
            onEditClick(barang)
        }

        holder.btnHapus.setOnClickListener {
            onDeleteClick(barang)
        }
    }

    override fun getItemCount(): Int = list.size

    /**
     * Update data set and refresh the adapter
     */
    fun updateData(newList: List<Barang>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
