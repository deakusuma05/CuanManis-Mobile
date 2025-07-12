package com.example.cuanmanis

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cuanmanis.databinding.ActivityBarangpostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BarangPostActivity : AppCompatActivity() {

    private lateinit var b: ActivityBarangpostBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityBarangpostBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupSpinners()

        // Saat user keluar dari EditText, tampilkan preview
        b.etLinks.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val links = b.etLinks.text.toString().trim().lines()
                    .map { it.trim() }
                    .filter { it.startsWith("http") && it.isNotEmpty() }
                showImagePreviews(links)
            }
        }

        b.btnPost.setOnClickListener { saveToFirestore() }
    }

    private fun saveToFirestore() {
        val linkInput = b.etLinks.text.toString().trim()
        val fotoLinks = linkInput.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.startsWith("http") }

        if (fotoLinks.isEmpty()) {
            Toast.makeText(this, "Minimal 1 link gambar harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (fotoLinks.size > 15) {
            Toast.makeText(this, "Maksimal 15 link gambar!", Toast.LENGTH_SHORT).show()
            return
        }

        val barang = hashMapOf(
            "nama"         to b.etNamaBarang.text.toString().trim(),
            "kategori"     to b.spinnerKategori.selectedItem.toString(),
            "lokasi"       to b.etLokasi.text.toString().trim(),
            "harga"        to b.etHarga.text.toString().toIntOrNull(),
            "kondisi"      to b.spinnerKondisi.selectedItem.toString(),
            "deskripsi"    to b.etDeskripsi.text.toString().trim(),
            "waktuPosting" to Date(),
            "foto"         to fotoLinks,
            "userId"       to (auth.currentUser?.uid ?: "")
        )

        b.btnPost.isEnabled = false
        b.btnPost.text = "Menyimpan..."

        db.collection("barang").add(barang)
            .addOnSuccessListener {
                Toast.makeText(this, "Barang tersimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                b.btnPost.isEnabled = true
                b.btnPost.text = "POSTING BARANG"
            }
    }

    private fun showImagePreviews(links: List<String>) {
        b.previewContainer.removeAllViews()
        val inflater = layoutInflater

        links.take(15).forEach { url ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400
                ).apply {
                    bottomMargin = 16
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(this)
                .load(url)
                .into(imageView)

            b.previewContainer.addView(imageView)
        }
    }

    private fun setupSpinners() {
        val kategoriList = listOf(
            "Buku & Alat Tulis", "Elektronik", "Perabot Kost", "Pakaian & Aksesori",
            "Alat Masak", "Transportasi", "Hiburan", "Kebersihan", "Barang Seni", "Lainnya"
        )
        val kondisiList = listOf(
            "Baru (belum pernah dipakai)",
            "Seperti Baru (jarang dipakai)",
            "Bekas, Masih Bagus (fungsi normal, kondisi oke)",
            "Bekas, Ada Cacat Ringan (misal: baret, tombol longgar)",
            "Rusak, Bisa untuk Sparepart"
        )
        b.spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategoriList)
        b.spinnerKondisi.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kondisiList)
    }

}
