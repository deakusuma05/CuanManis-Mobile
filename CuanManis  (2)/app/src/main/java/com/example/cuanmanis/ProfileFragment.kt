package com.example.cuanmanis

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.cuanmanis.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Fragment Profil – versi aman (tidak crash NPE) dan tanpa menangani BottomNavigation sendiri.
 *
 * • Semua navigasi bottom-nav dipusatkan di MainActivity.
 * • Callback Firestore aman – tidak menyentuh view ketika binding == null.
 */
class ProfileFragment : Fragment(), MenuProvider {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    /* --------------------------------------------------------------------- */
    /*  Lifecycle                                                           */
    /* --------------------------------------------------------------------- */

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Pasang opsi menu (logout)
        (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupUI()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        // Pastikan tab di MainActivity tersorot "profile"
        (activity as? MainActivity)?.setBottomNavSelected(R.id.nav_profile)
    }

    override fun onStart() {
        super.onStart()
        // refresh bila kembali dari background
        loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null   // hindari memory leak
    }

    /* --------------------------------------------------------------------- */
    /*  UI setup                                                             */
    /* --------------------------------------------------------------------- */

    private fun setupUI() {
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
            binding.toolbar.title = "Profil \uD83D\uDC4B"  // emoji waving hand
        }

        binding.etDob.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener {
            if (validateInput()) saveProfile()
        }
        // Tambahkan listener untuk tombol Edit Foto
        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(requireContext(), "Fitur mengubah foto profil masih dalam tahap pengembangan", Toast.LENGTH_SHORT).show()
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Safe helper – hanya eksekusi bila view masih ada                     */
    /* --------------------------------------------------------------------- */

    private inline fun safeRunOnView(action: (FragmentProfileBinding) -> Unit) {
        if (view != null && _binding != null && isAdded) action(binding)
    }

    /* --------------------------------------------------------------------- */
    /*  Firestore – load & save                                              */
    /* --------------------------------------------------------------------- */

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        showLoading(true)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                safeRunOnView { b ->
                    showLoading(false)
                    if (doc != null && doc.exists()) {
                        b.etEmail.setText(doc.getString("email") ?: "")
                        b.etUsername.setText(doc.getString("username") ?: "")
                        b.etDob.setText(doc.getString("tanggalLahir") ?: "")
                        b.etGender.setText(doc.getString("gender") ?: "")
                        b.etCampus.setText(doc.getString("asalKampus") ?: "")
                        b.etMajor.setText(doc.getString("jurusan") ?: "")
                        b.etPhone.setText(doc.getString("noHP") ?: "")
                        b.etAlamat.setText(doc.getString("alamat") ?: "")
                    } else {
                        Toast.makeText(requireContext(), "Data profil tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                safeRunOnView {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveProfile() {
        val user = auth.currentUser ?: return

        val updated = mapOf(
            "email" to binding.etEmail.text.toString().trim(),
            "username" to binding.etUsername.text.toString().trim(),
            "tanggalLahir" to binding.etDob.text.toString().trim(),
            "gender" to binding.etGender.text.toString().trim(),
            "asalKampus" to binding.etCampus.text.toString().trim(),
            "jurusan" to binding.etMajor.text.toString().trim(),
            "noHP" to binding.etPhone.text.toString().trim(),
            "alamat" to binding.etAlamat.text.toString().trim()
        )

        showLoading(true)
        db.collection("users").document(user.uid).update(updated)
            .addOnSuccessListener {
                safeRunOnView {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                safeRunOnView {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Gagal memperbarui profil", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /* --------------------------------------------------------------------- */
    /*  Validation & utilities                                               */
    /* --------------------------------------------------------------------- */

    private fun validateInput(): Boolean {
        var valid = true

        if (binding.etUsername.text.toString().trim().isEmpty()) {
            binding.etUsername.error = "Username tidak boleh kosong"
            valid = false
        }
        if (binding.etEmail.text.toString().trim().isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            valid = false
        }
        binding.etPhone.text.toString().trim().takeIf { it.isNotEmpty() }?.let { phone ->
            if (!phone.matches(Regex("^[0-9+\\-\\s()]+$"))) {
                binding.etPhone.error = "Format nomor HP tidak valid"
                valid = false
            }
        }
        return valid
    }

    private fun showLoading(show: Boolean) {
        binding.btnSave.isEnabled = !show
        binding.btnSave.text = if (show) "Menyimpan..." else "Simpan"
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            binding.etDob.setText(String.format("%02d/%02d/%d", d, m + 1, y))
        }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
    }

    /* --------------------------------------------------------------------- */
    /*  MenuProvider (Logout)                                                */
    /* --------------------------------------------------------------------- */

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.profile_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.action_logout -> {
            logout(); true
        }
        else -> false
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logout berhasil", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), MainActivity::class.java).putExtra("navigate_to", "login"))
        requireActivity().finish()
    }
}