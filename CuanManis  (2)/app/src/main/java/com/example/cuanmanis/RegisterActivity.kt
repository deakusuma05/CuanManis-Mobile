package com.example.cuanmanis

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cuanmanis.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup DatePicker untuk Tanggal Lahir
        binding.tanggalLahirInput.setOnClickListener {
            showDatePickerDialog()
        }

        // Tombol Register
        binding.buttonRegister.setOnClickListener {
            val email = binding.emailRegisterInput.text.toString().trim()
            val username = binding.usernameInput.text.toString().trim()
            val gender = when (binding.genderRadioGroup.checkedRadioButtonId) {
                R.id.radioMale -> "Laki-laki"
                R.id.radioFemale -> "Perempuan"
                else -> ""
            }
            val tanggalLahir = binding.tanggalLahirInput.text.toString().trim()
            val asalKampus = binding.asalKampusInput.text.toString().trim()
            val jurusan = binding.jurusanInput.text.toString().trim()
            val alamat = binding.alamatInput.text.toString().trim()
            val noHP = binding.noHPInput.text.toString().trim()
            val password = binding.passwordRegisterInput.text.toString().trim()
            val confirmPassword = binding.confirmpasswordRegisterInput.text.toString().trim()

            // Validasi input
            if (!validateInputs(email, username, gender, tanggalLahir, asalKampus, jurusan, alamat, noHP, password, confirmPassword)) {
                return@setOnClickListener
            }

            // Tampilkan ProgressBar
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonRegister.isEnabled = false

            // Buat akun di Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    // Simpan data pengguna ke Firestore
                    val user = hashMapOf(
                        "email" to email,
                        "username" to username,
                        "gender" to gender,
                        "tanggalLahir" to tanggalLahir,
                        "asalKampus" to asalKampus,
                        "jurusan" to jurusan,
                        "alamat" to alamat,
                        "noHP" to noHP,
                        "displayName" to username // Untuk konsistensi dengan HomeFragment
                    )
                    db.collection("users").document(authResult.user!!.uid)
                        .set(user)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            binding.buttonRegister.isEnabled = true
                            Toast.makeText(this, "Pendaftaran berhasil", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            binding.buttonRegister.isEnabled = true
                            Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(this, "Pendaftaran gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Link ke Login
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(
        email: String,
        username: String,
        gender: String,
        tanggalLahir: String,
        asalKampus: String,
        jurusan: String,
        alamat: String,
        noHP: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "Username harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (gender.isEmpty()) {
            Toast.makeText(this, "Pilih gender", Toast.LENGTH_SHORT).show()
            return false
        }
        if (tanggalLahir.isEmpty()) {
            Toast.makeText(this, "Tanggal lahir harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (asalKampus.isEmpty()) {
            Toast.makeText(this, "Asal universitas harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (jurusan.isEmpty()) {
            Toast.makeText(this, "Jurusan harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (alamat.isEmpty()) {
            Toast.makeText(this, "Alamat harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }
        if (noHP.isEmpty() || !noHP.matches(Regex("^[0-9]{10,13}$"))) {
            Toast.makeText(this, "Nomor handphone tidak valid", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty() || password.length < 6) {
            Toast.makeText(this, "Password harus minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.tanggalLahirInput.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = calendar.timeInMillis // Batasi ke tanggal hari ini
        datePickerDialog.show()
    }
}