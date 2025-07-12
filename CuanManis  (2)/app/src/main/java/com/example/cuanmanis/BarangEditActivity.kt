package com.example.cuanmanis

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cuanmanis.model.Barang
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BarangEditActivity : AppCompatActivity() {

    private lateinit var etNamaBarang: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var etLokasi: EditText
    private lateinit var etHarga: EditText
    private lateinit var spinnerKondisi: Spinner
    private lateinit var etDeskripsi: EditText
    private lateinit var etLinks: EditText
    private lateinit var previewContainer: LinearLayout
    private lateinit var btnUpdate: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var barangId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barang_edit)

        // init views
        etNamaBarang = findViewById(R.id.et_nama_barang)
        spinnerKategori = findViewById(R.id.spinner_kategori)
        etLokasi = findViewById(R.id.et_lokasi)
        etHarga = findViewById(R.id.et_harga)
        spinnerKondisi = findViewById(R.id.spinner_kondisi)
        etDeskripsi = findViewById(R.id.et_deskripsi)
        etLinks = findViewById(R.id.et_links)
        previewContainer = findViewById(R.id.previewContainer)
        btnUpdate = findViewById(R.id.btn_update)

        setupSpinners()

        barangId = intent.getStringExtra("barangId")
        Log.d("BarangEditActivity", "barangId dari Intent: $barangId")

        if (barangId != null) {
            loadBarangData(barangId!!)
        } else {
            Toast.makeText(this, "Barang ID tidak ditemukan!", Toast.LENGTH_SHORT).show()
        }

        etLinks.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) previewLinks()
        }

        btnUpdate.setOnClickListener {
            updateBarang()
        }
    }

    private fun setupSpinners() {
        val kategoriList = listOf(
            "Buku & Alat Tulis", "Elektronik", "Perabot Kost",
            "Pakaian & Aksesori", "Alat Masak", "Transportasi",
            "Hiburan", "Kebersihan", "Barang Seni", "Lainnya"
        )
        val kondisiList = listOf(
            "Baru (belum pernah dipakai)",
            "Seperti Baru (jarang dipakai)",
            "Bekas, Masih Bagus (fungsi normal, kondisi oke)",
            "Bekas, Ada Cacat Ringan (misal: baret, tombol longgar)",
            "Rusak, Bisa untuk Sparepart"
        )
        spinnerKategori.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kategoriList)
        spinnerKondisi.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kondisiList)
    }

    private fun loadBarangData(id: String) {
        db.collection("barang").document(id).get()
            .addOnSuccessListener { document ->
                Log.d("BarangEditActivity", "document.exists(): ${document.exists()}")
                Log.d("BarangEditActivity", "document.data: ${document.data}")

                if (document.exists()) {
                    etNamaBarang.setText(document.getString("nama"))
                    etLokasi.setText(document.getString("lokasi"))
                    etHarga.setText(document.getLong("harga")?.toString() ?: "")
                    etDeskripsi.setText(document.getString("deskripsi"))
                    setSpinnerSelection(spinnerKategori, document.getString("kategori"))
                    setSpinnerSelection(spinnerKondisi, document.getString("kondisi"))

                    val links = document.get("foto") as? List<String> ?: emptyList()
                    etLinks.setText(links.joinToString("\n"))
                    previewLinks()
                } else {
                    Toast.makeText(this, "Barang tidak ditemukan di Firestore.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("BarangEditActivity", "Error memuat data: ${e.message}", e)
                Toast.makeText(this, "Error memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String?) {
        if (value.isNullOrEmpty()) return
        val adapter = spinner.adapter as ArrayAdapter<String>
        val pos = adapter.getPosition(value)
        if (pos >= 0) spinner.setSelection(pos)
    }

    private fun previewLinks() {
        previewContainer.removeAllViews()
        val links = etLinks.text.toString().lines().map { it.trim() }.filter { it.startsWith("http") }
        links.take(15).forEach { url ->
            val imgView = ImageView(this)
            imgView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply { bottomMargin = 16 }
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(url).into(imgView)
            previewContainer.addView(imgView)
        }
    }

    private fun updateBarang() {
        val links = etLinks.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() && it.startsWith("http") }
        if (links.isEmpty()) {
            Toast.makeText(this, "Minimal 1 link gambar harus diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        btnUpdate.isEnabled = false
        btnUpdate.text = "Menyimpan..."

        val updated = hashMapOf(
            "nama" to etNamaBarang.text.toString().trim(),
            "kategori" to spinnerKategori.selectedItem.toString(),
            "lokasi" to etLokasi.text.toString().trim(),
            "harga" to etHarga.text.toString().toIntOrNull(),
            "kondisi" to spinnerKondisi.selectedItem.toString(),
            "deskripsi" to etDeskripsi.text.toString().trim(),
            "foto" to links,
            "userId" to auth.currentUser?.uid.orEmpty(),
            "waktuUpdate" to Date()
        )

        barangId?.let { id ->
            db.collection("barang").document(id).update(updated as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Barang berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("BarangEditActivity", "Gagal memperbarui: ${e.message}", e)
                    Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "SIMPAN PERUBAHAN"
                }
        } ?: run {
            Toast.makeText(this, "Barang ID kosong, tidak bisa update.", Toast.LENGTH_SHORT).show()
        }
    }
}