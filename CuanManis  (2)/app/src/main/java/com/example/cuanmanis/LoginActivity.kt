package com.example.cuanmanis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cuanmanis.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        Log.d(TAG, "LoginActivity dimulai")

        // Cek jika sudah login
        if (auth.currentUser != null) {
            Log.d(TAG, "Pengguna sudah login: ${auth.currentUser?.email}, arahkan ke MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        } else {
            Log.d(TAG, "Pengguna belum login, tampilkan UI LoginActivity")
        }

        // Tombol Login
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Tampilkan ProgressBar
            binding.progressBar.visibility = View.VISIBLE
            binding.loginButton.isEnabled = false
            Log.d(TAG, "Mencoba login dengan email: $email")

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Log.d(TAG, "Login berhasil, arahkan ke MainActivity")
                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    binding.loginButton.isEnabled = true
                    Log.e(TAG, "Login gagal: ${e.message}")
                    Toast.makeText(this, "Login gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Link ke Register
        binding.tvRegisterLink.setOnClickListener {
            Log.d(TAG, "Navigasi ke RegisterActivity")
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Lupa Kata Sandi
        binding.forgotPassword.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Masukkan email terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            Log.d(TAG, "Mengirim email reset password untuk: $email")
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Email reset kata sandi telah dikirim", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Email reset password berhasil dikirim")
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Gagal mengirim email reset: ${e.message}")
                    Toast.makeText(this, "Gagal mengirim email reset: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Tombol logout sementara untuk pengujian (klik lama pada logo)
        binding.logoCuanManis.setOnLongClickListener {
            auth.signOut()
            Log.d(TAG, "Pengguna di-logout untuk pengujian")
            Toast.makeText(this, "Logout berhasil, silakan coba lagi", Toast.LENGTH_SHORT).show()
            recreate() // Muat ulang LoginActivity
            true
        }
    }
}