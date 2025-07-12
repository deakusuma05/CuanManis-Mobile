package com.example.cuanmanis

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cuanmanis.adapter.WishlistAdapter
import com.example.cuanmanis.databinding.FragmentWishlistBinding
import com.example.cuanmanis.model.Barang
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class WishlistFragment : Fragment() {

    private var _b: FragmentWishlistBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: WishlistAdapter
    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var currentUid = ""

    /* ───────── lifecycle ───────── */
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentWishlistBinding.inflate(i, c, false)
        return b.root
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        currentUid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        setupRecyclerView()
        loadWishlist()
    }

    /* ───── 1. RecyclerView & adapter ───── */
    private fun setupRecyclerView() {
        adapter = WishlistAdapter(
            onItemClick = { barang ->
                startActivity(
                    Intent(requireContext(), BarangDetailActivity::class.java)
                        .putExtra("DetailBarangId", barang.id)
                )
            },
            onDeleteClick = { barang -> deleteWishlistItem(barang) }
        )

        // Grid 2 kolom
        b.rvWishlist.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = this@WishlistFragment.adapter
            setHasFixedSize(true)

            // spasi antar card
            val space = resources.getDimensionPixelSize(R.dimen.rv_space)  // mis. 8dp
            addItemDecoration(SpaceItemDecoration(space))
        }
    }

    /* ───── 2. Muat data wishlist ───── */
    private fun loadWishlist() {
        b.progressBar.visibility = View.VISIBLE

        db.collection("wishlist")
            .whereEqualTo("userId", currentUid)
            .get()
            .addOnSuccessListener { wishDocs ->
                val idList = wishDocs.mapNotNull { it.getString("barangId") }
                if (idList.isEmpty()) {
                    b.progressBar.visibility = View.GONE
                    adapter.updateData(emptyList())
                    Toast.makeText(requireContext(), "Wishlist kosong", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("barang")
                    .whereIn(FieldPath.documentId(), idList.take(10))
                    .get()
                    .addOnSuccessListener { barangDocs ->
                        val items = barangDocs.documents.mapNotNull { d ->
                            d.toObject(Barang::class.java)?.copy(id = d.id)
                        }
                        b.progressBar.visibility = View.GONE
                        adapter.updateData(items)
                    }
                    .addOnFailureListener {
                        b.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(),"Gagal memuat barang",Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                b.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(),"Gagal memuat wishlist",Toast.LENGTH_SHORT).show()
            }
    }

    /* ───── 3. Hapus satu item wishlist ───── */
    private fun deleteWishlistItem(barang: Barang) {
        db.collection("wishlist")
            .whereEqualTo("userId", currentUid)
            .whereEqualTo("barangId", barang.id)
            .get()
            .addOnSuccessListener { qs ->
                if (qs.isEmpty) return@addOnSuccessListener
                qs.documents.first().reference.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            "Dihapus dari wishlist", Toast.LENGTH_SHORT).show()
                        loadWishlist()
                    }
            }
    }

    /* ───── dekorasi jarak antar card ───── */
    class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(r: Rect, v: View, p: RecyclerView, s: RecyclerView.State) {
            r.set(space, space, space, space)
        }
    }
}
