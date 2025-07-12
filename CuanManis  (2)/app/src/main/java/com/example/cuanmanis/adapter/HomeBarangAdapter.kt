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

/**
 * HomeBarangAdapter FINAL — memuat gambar via URL langsung + menampilkan
 * "username, jurusan" penjual (cached) + DiffUtil untuk update halus.
 *
 * Catatan: file URI lokal (content://) sudah **tidak** didukung lagi; pastikan
 * field `foto` berisi URL publik (http/https) di Firestore.
 */
class HomeBarangAdapter(
    private var list: List<Barang>,
    private val onViewClick: (Barang) -> Unit
) : RecyclerView.Adapter<HomeBarangAdapter.ViewHolder>() {

    /** cache <uid, Pair(username, jurusan)> agar tidak hit Firestore berulang */
    private val userCache = mutableMapOf<String, Pair<String, String>>()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivBarang: ImageView       = v.findViewById(R.id.ivBarang)
        val tvKategori: TextView      = v.findViewById(R.id.tvKategori)
        val tvNamaBarang: TextView    = v.findViewById(R.id.tvNamaBarang)
        val tvHarga: TextView         = v.findViewById(R.id.tvHarga)
        val tvPenjual: TextView       = v.findViewById(R.id.tvPenjual)
        val btnViewProduct: MaterialButton = v.findViewById(R.id.btn_view_product)
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_card_prelo, p, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val barang = list[pos]
        val ctx = h.itemView.context

        // ① Gambar
        val firstPhoto = barang.foto.firstOrNull()
        Glide.with(ctx)
            .load(firstPhoto)
            .placeholder(R.drawable.sample_barang)
            .error(R.drawable.sample_barang)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(h.ivBarang)

        // ② Info
        h.tvKategori.text   = barang.kategori.ifEmpty { "Tidak diketahui" }
        h.tvNamaBarang.text = barang.nama.ifEmpty { "Tanpa nama" }
        h.tvHarga.text = barang.harga?.takeIf { it != 0 }?.let {
            "Rp %,d".format(it)
        } ?: "N/A"

        // ③ Penjual (nama + jurusan dari Firestore)
        val uid = barang.userId
        if (uid.isNotEmpty()) {
            userCache[uid]?.let { (uname, jur) ->
                h.tvPenjual.text = "$uname, $jur"
            } ?: FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (pos == h.adapterPosition) { // Cegah update ke ViewHolder yang salah
                        val uname = doc.getString("username") ?: "Penjual"
                        val jur = doc.getString("jurusan") ?: "-"
                        userCache[uid] = uname to jur
                        h.tvPenjual.text = "$uname, $jur"
                    }
                }
        } else {
            h.tvPenjual.text = "Penjual"
        }

        // ④ Klik tombol
        h.btnViewProduct.setOnClickListener {
            onViewClick(barang)
        }
    }


    override fun getItemCount() = list.size

    fun updateData(newList: List<Barang>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = list[o].id == newList[n].id
            override fun areContentsTheSame(o: Int, n: Int) = list[o] == newList[n]
        })
        list = newList
        diff.dispatchUpdatesTo(this)
    }
}
