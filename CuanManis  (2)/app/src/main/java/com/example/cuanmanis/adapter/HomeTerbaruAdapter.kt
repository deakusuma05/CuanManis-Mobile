package com.example.cuanmanis.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.cuanmanis.R
import com.example.cuanmanis.model.Barang
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class HomeTerbaruAdapter(
    private var list: List<Barang>,
    private val onViewClick: (Barang) -> Unit
) : RecyclerView.Adapter<HomeTerbaruAdapter.ViewHolder>() {

    /** Cache untuk menyimpan <uid, Pair(username, jurusan)> agar tidak memanggil Firestore berulang */
    private val userCache = mutableMapOf<String, Pair<String, String>>()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivBarang: ImageView       = v.findViewById(R.id.ivBarang)
        val tvKategori: TextView      = v.findViewById(R.id.tvKategori)
        val tvNamaBarang: TextView    = v.findViewById(R.id.tvNamaBarang)
        val tvHarga: TextView         = v.findViewById(R.id.tvHarga)
        val tvPenjual: TextView       = v.findViewById(R.id.tvPenjual)
        val btnViewProduct: MaterialButton = v.findViewById(R.id.btn_view_product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.itemcard_terbaru, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barang = list[position]
        val ctx = holder.itemView.context

        // Gambar pertama
        val firstPhoto = barang.foto.firstOrNull()
        Glide.with(ctx)
            .load(firstPhoto)
            .placeholder(R.drawable.sample_barang)
            .error(R.drawable.sample_barang)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.ivBarang)

        // Data teks
        holder.tvKategori.text = barang.kategori.ifEmpty { "Tidak diketahui" }
        holder.tvNamaBarang.text = barang.nama.ifEmpty { "Tanpa nama" }
        holder.tvHarga.text = barang.harga?.takeIf { it != 0 }?.let {
            "Rp %,d".format(it)
        } ?: "N/A"

        // Info penjual
        val uid = barang.userId
        if (uid.isNotEmpty()) {
            userCache[uid]?.let { (uname, jur) ->
                holder.tvPenjual.text = "$uname, $jur"
            } ?: FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val uname = doc.getString("username") ?: "Penjual"
                    val jur = doc.getString("jurusan") ?: "-"
                    userCache[uid] = uname to jur
                    if (holder.adapterPosition == position || holder.layoutPosition == position) {
                        holder.tvPenjual.text = "$uname, $jur"
                    }
                }
        } else {
            holder.tvPenjual.text = "Penjual"
        }

        // Klik detail
        holder.btnViewProduct.setOnClickListener {
            onViewClick(barang)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Barang>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return list[oldItemPosition] == newList[newItemPosition]
            }
        })
        list = newList
        diff.dispatchUpdatesTo(this)
    }
}
