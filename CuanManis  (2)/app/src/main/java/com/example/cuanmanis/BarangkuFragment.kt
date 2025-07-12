package com.example.cuanmanis.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cuanmanis.BarangEditActivity
import com.example.cuanmanis.BarangPostActivity
import com.example.cuanmanis.LoginActivity
import com.example.cuanmanis.adapter.BarangAdapter
import com.example.cuanmanis.databinding.FragmentBarangkuBinding
import com.example.cuanmanis.model.Barang
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BarangkuFragment : Fragment() {

    private var _binding: FragmentBarangkuBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BarangAdapter
    private val barangList = mutableListOf<Barang>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarangkuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tombol "Tambah"
        binding.btnAddPost.setOnClickListener {
            startActivity(Intent(requireContext(), BarangPostActivity::class.java))
        }

        setupRecyclerView()
        loadBarangUser()
    }

    private fun setupRecyclerView() {
        adapter = BarangAdapter(
            onEditClick = { barang ->
                val intent = Intent(requireContext(), BarangEditActivity::class.java).apply {
                    putExtra("barangId", barang.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { barang ->
                barang.id?.let { docId ->
                    db.collection("barang").document(docId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Barang dihapus", Toast.LENGTH_SHORT).show()
                            loadBarangUser()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } ?: Toast.makeText(requireContext(), "ID barang tidak valid", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvPostedItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPostedItems.adapter = adapter
        adapter.updateData(barangList) // Inisialisasi data awal
    }

    private fun loadBarangUser() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.rvPostedItems.visibility = View.GONE
        binding.tvEmptyState.visibility = View.GONE

        db.collection("barang")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                barangList.clear()
                for (doc in result) {
                    val b = doc.toObject(Barang::class.java).apply {
                        id = doc.id
                    }
                    barangList.add(b)
                }
                adapter.updateData(barangList)
                binding.progressBar.visibility = View.GONE
                binding.rvPostedItems.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = if (barangList.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        loadBarangUser() // Refresh data saat fragment kembali aktif
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}