package com.example.cuanmanis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cuanmanis.adapter.PhotoPagerAdapter
import com.example.cuanmanis.databinding.ActivityBarangDetailBinding
import com.example.cuanmanis.model.Barang
import com.example.cuanmanis.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BarangDetailActivity : AppCompatActivity() {

    private lateinit var b: ActivityBarangDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBarangDetailBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.topAppBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val barangId = intent.getStringExtra("DetailBarangId")
        if (barangId.isNullOrEmpty()) {
            Toast.makeText(this, "ID barang tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Ambil data Barang
        db.collection("barang").document(barangId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Toast.makeText(this, "Barang tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                val barang = doc.toObject(Barang::class.java)?.copy(id = doc.id)
                if (barang != null) {
                    bindData(barang)
                } else {
                    Toast.makeText(this, "Gagal memuat barang", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarangDetail", "Gagal memuat data: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun bindData(item: Barang) {
        // Foto
        val photos = if (item.foto.isEmpty()) listOf(R.drawable.sample_item) else item.foto
        b.viewPagerProduct.adapter = PhotoPagerAdapter(photos)
        b.indicator.setViewPager(b.viewPagerProduct)

        // Informasi barang (kecuali uploadedBy & phone number)
        b.tvProductTitle.text = item.nama.ifEmpty { "Tanpa Nama" }
        b.tvProductPrice.text = if (item.harga != null) "Rp %,d".format(item.harga) else "Harga Tidak Tersedia"
        b.tvCategory.text = item.kategori.ifEmpty { "Tidak Diketahui" }
        b.tvCondition.text = item.kondisi.ifEmpty { "Tidak Diketahui" }
        b.tvLocation.text = item.lokasi.ifEmpty { "Tidak Diketahui" }
        b.tvDescription.text = item.deskripsi.ifEmpty { "Tidak ada deskripsi" }

        // Ambil data User untuk username dan noHP
        var phoneNumber = ""
        db.collection("users").document(item.userId).get()
            .addOnSuccessListener { userDoc ->
                val user = userDoc.toObject(User::class.java)
                phoneNumber = user?.noHP.orEmpty()
                b.tvUploadedBy.text = "Diunggah oleh: ${user?.username.orEmpty()}"
                Log.d("BarangDetail", "Nomor HP penjual: $phoneNumber")
            }
            .addOnFailureListener { e ->
                Log.e("BarangDetail", "Error ambil user: ${e.message}", e)
                b.tvUploadedBy.text = "Diunggah oleh: Tidak diketahui"
            }

        // Kontak penjual
        b.btnContact.setOnClickListener {
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Nomor HP penjual tidak tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val waUri = Uri.parse(
                "https://wa.me/62$phoneNumber?text=Halo, saya tertarik dengan barang '${item.nama}'"
            )
            startActivity(Intent(Intent.ACTION_VIEW, waUri))
        }

        // Favorit
        b.btnFavorite.setOnClickListener { view ->
            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Harap login dulu untuk menambahkan ke favorit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            view.isSelected = !view.isSelected
            if (view.isSelected) {
                val wishlistData = mapOf(
                    "userId" to uid,
                    "barangId" to item.id
                )
                db.collection("wishlist").add(wishlistData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ditambahkan ke favorit", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("BarangDetail", "Error add wishlist: ${e.message}", e)
                        Toast.makeText(this, "Gagal menambahkan ke favorit", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Dihapus dari favorit (hapus manual di Firestore jika mau)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}