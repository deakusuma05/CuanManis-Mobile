package com.example.cuanmanis

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.example.cuanmanis.adapter.HomeBarangAdapter
import com.example.cuanmanis.adapter.HomeTerbaruAdapter
import com.example.cuanmanis.databinding.FragmentHomeBinding
import com.example.cuanmanis.model.Barang
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }

    private lateinit var terbaruAdapter : HomeTerbaruAdapter
    private lateinit var katalogAdapter : HomeBarangAdapter
    private var loading = 0

    /* ─── helpers ─── */
    private inline fun safe(block: (FragmentHomeBinding) -> Unit) {
        if (_b != null && isAdded) block(b)
    }
    private fun showPb() { if (++loading == 1) safe { it.progressBar.visibility = View.VISIBLE } }
    private fun hidePb() { if (--loading <= 0) { loading = 0; safe { it.progressBar.visibility = View.GONE } } }

    /* ─── lifecycle ─── */
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false)
        return b.root
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        greetUser()
        setupRecycler()
        loadTerbaru()
        loadKatalog()
        setupSearch()
        b.ivFilter.setOnClickListener { showFilterDialog() }
    }

    /* ─── 1. Sapaan ─── */
    private fun greetUser() {
        auth.currentUser?.uid ?: return
        db.collection("users").document(auth.uid!!).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("username") ?: "Sobat Mahasiswa"
                safe {
                    it.tvGreeting.text    = "Hai, $name! 👋"
                    it.tvSubGreeting.text = "Yuk cari barang preloved kesukaanmu"
                }
            }
    }

    /* ─── 2. RecyclerView ─── */
    private fun setupRecycler() {
        // Terbaru (carousel horizontal)
        b.rvBarang.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            terbaruAdapter = HomeTerbaruAdapter(emptyList()) { openDetail(it.id) }
            adapter = terbaruAdapter
        }
        // Katalog (grid 2 kolom)
        b.rvRekomendasi.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            katalogAdapter = HomeBarangAdapter(emptyList()) { openDetail(it.id) }
            adapter = katalogAdapter
            setHasFixedSize(true)
            val space = resources.getDimensionPixelSize(R.dimen.rv_space)
            addItemDecoration(SpaceItemDecoration(space))
        }
    }
    private fun openDetail(id: String) =
        startActivity(Intent(requireContext(), BarangDetailActivity::class.java)
            .putExtra("DetailBarangId", id))

    /* ─── 3A. loader TERBARU ─── */
    private fun loadTerbaru() {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val q = db.collection("barang")
            .whereGreaterThanOrEqualTo("waktuPosting", Timestamp(yesterday))
            .orderBy("waktuPosting", Query.Direction.DESCENDING)
            .limit(10)
        showPb()
        q.get()
            .addOnSuccessListener { snap ->
                val list = snap.map { it.toObject(Barang::class.java).apply { id = it.id } }
                safe { terbaruAdapter.updateData(list) }
                hidePb()
            }
            .addOnFailureListener { hidePb() }
    }

    /* ─── 3B. loader KATALOG (tanpa syarat) ─── */
    private fun loadKatalog() {
        showPb()
        db.collection("barang")
            .orderBy("waktuPosting", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { it.toObject(Barang::class.java).apply { id = it.id } }
                safe { katalogAdapter.updateData(list) }
                hidePb()
            }
            .addOnFailureListener { hidePb() }
    }

    /* ─── 4. Search (barang saja) ─── */
    private fun setupSearch() = b.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(txt: String?): Boolean { txt?.let { search(it) }; return true }
        override fun onQueryTextChange(txt: String?): Boolean {
            if (txt.isNullOrBlank()) { loadTerbaru(); loadKatalog() }
            return true
        }
    })
    private fun search(keywordRaw: String) {
        val key = keywordRaw.trim().lowercase()
        if (key.isBlank()) return
        showPb()

        db.collection("barang")
            .orderBy("waktuPosting", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                val hasil = snap.map { it.toObject(Barang::class.java).apply { id = it.id } }
                    .filter { it.nama.lowercase().contains(key) }

                safe {
                    katalogAdapter.updateData(hasil)
                    terbaruAdapter.updateData(emptyList())
                    it.rvBarang.visibility = View.GONE
                    it.rvRekomendasi.visibility = View.VISIBLE

                    if (hasil.isEmpty()) {
                        Toast.makeText(it.root.context, "Tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
                hidePb()
            }
            .addOnFailureListener {
                hidePb()
                safe { Toast.makeText(it.root.context, "Search error", Toast.LENGTH_SHORT).show() }
            }
    }


    /* ─── 5. Filter dialog ─── */
    private fun showFilterDialog() {
        val kategoriOpt = arrayOf(
            "Buku & Alat Tulis","Elektronik","Perabot Kost","Pakaian & Aksesori",
            "Alat Masak","Transportasi","Hiburan","Kebersihan","Barang Seni","Lainnya"
        )
        val kondisiOpt = arrayOf(
            "Baru (belum pernah dipakai)","Seperti Baru (jarang dipakai)",
            "Bekas, Masih Bagus (fungsi normal, kondisi oke)",
            "Bekas, Ada Cacat Ringan (misal: baret, tombol longgar)","Rusak, Bisa untuk Sparepart"
        )
        val v = layoutInflater.inflate(R.layout.dialog_filter, null)
        v.findViewById<Spinner>(R.id.spKategori).adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kategoriOpt)
        v.findViewById<Spinner>(R.id.spKondisi).adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, kondisiOpt)

        AlertDialog.Builder(requireContext())
            .setTitle("Filter")
            .setView(v)
            .setPositiveButton("Terapkan") { _, _ ->
                val kat = kategoriOpt[v.findViewById<Spinner>(R.id.spKategori).selectedItemPosition]
                val kon = kondisiOpt[v.findViewById<Spinner>(R.id.spKondisi).selectedItemPosition]
                applyFilter(kat, kon)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    /* filter harus memenuhi KATEGORI & KONDISI */
    private fun applyFilter(kategori: String, kondisi: String) {
        showPb()
        db.collection("barang")
            .whereEqualTo("kategori", kategori)
            .whereEqualTo("kondisi", kondisi)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { it.toObject(Barang::class.java).apply { id = it.id } }
                    .sortedByDescending { it.waktuPosting }
                safe {
                    katalogAdapter.updateData(list)
                    b.rvBarang.visibility = View.GONE
                    b.rvRekomendasi.visibility = View.VISIBLE
                    if (list.isEmpty())
                        Toast.makeText(it.root.context,
                            "Tidak ada barang sesuai filter", Toast.LENGTH_SHORT).show()
                }
                hidePb()
            }
            .addOnFailureListener {
                hidePb()
                safe { Toast.makeText(it.root.context, "Gagal memuat data", Toast.LENGTH_SHORT).show() }
            }
    }

    /* ─── dekorasi jarak ─── */
    private class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(r: Rect, v: View, p: RecyclerView, s: RecyclerView.State) {
            r.set(space, space, space, space)
        }
    }
}
