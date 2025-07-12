package com.example.cuanmanis.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cuanmanis.R
import com.example.cuanmanis.databinding.ItemWishlistBarangBinding
import com.example.cuanmanis.model.Barang

class WishlistAdapter(
    private val onItemClick: (Barang) -> Unit,
    private val onDeleteClick: (Barang) -> Unit     // wajib: delete callback
) : RecyclerView.Adapter<WishlistAdapter.ViewHolder>() {

    private val wishlistItems = mutableListOf<Barang>()

    fun updateData(newList: List<Barang>) {
        wishlistItems.clear()
        wishlistItems.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val b: ItemWishlistBarangBinding)
        : RecyclerView.ViewHolder(b.root) {

        fun bind(item: Barang) = with(b) {
            // --- isi teks ---
            tvNamaBarang.text = item.nama
            tvHarga.text      = "Rp %,d".format(item.harga ?: 0)
            tvKategori.text   = item.kategori
            tvPenjual.text    = item.userId   // ganti nanti dgn username

            // --- foto barang (ambil foto[0] jika list) ---
            val fotoUrl = when (item.foto) {
                is List<*> -> item.foto.firstOrNull()
                else       -> item.foto
            }
            Glide.with(ivBarang.context)
                .load(fotoUrl)
                .placeholder(R.drawable.sample_item)
                .error(R.drawable.sample_item)
                .into(ivBarang)

            // --- klik kartu -> detail ---
            root.setOnClickListener { onItemClick(item) }

            // --- ikon wishlist SELALU tampil ---
            ivWishlist.setImageResource(R.drawable.ic_heart)
            ivWishlist.setOnClickListener { onDeleteClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWishlistBarangBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(wishlistItems[position])

    override fun getItemCount(): Int = wishlistItems.size
}
