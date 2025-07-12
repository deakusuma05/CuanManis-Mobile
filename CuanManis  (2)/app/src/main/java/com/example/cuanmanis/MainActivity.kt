package com.example.cuanmanis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.cuanmanis.databinding.ActivityMainBinding
import com.example.cuanmanis.ui.BarangkuFragment
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val homeFragment = HomeFragment()
    private val barangkuFragment = BarangkuFragment()
    private val wishlistFragment = WishlistFragment()
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Redirect jika belum login
        if (auth.currentUser == null) {
            Log.d("MainActivity", "Pengguna belum login, arahkan ke LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Atur bottom-nav listener
        binding.bottomNavigation.setOnItemSelectedListener(navListener)

        // Tampilkan HomeFragment sebagai default saat pertama kali dibuat
        if (savedInstanceState == null) {
            openFragment(homeFragment)
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        // Handle deep-link via intent extra
        intent.getStringExtra("navigate_to")?.let { target ->
            val (navId, fragment) = when (target) {
                "home" -> R.id.nav_home to homeFragment
                "barang" -> R.id.nav_barang to barangkuFragment
                "wishlist" -> R.id.nav_wishlist to wishlistFragment
                "profile" -> R.id.nav_profile to profileFragment
                else -> null to null
            }
            if (navId != null && fragment != null) {
                openFragment(fragment)
                binding.bottomNavigation.selectedItemId = navId
            }
        }
    }

    /* ------------------------------------------------------------- */
    /*  Bottom-nav listener                                          */
    /* ------------------------------------------------------------- */
    private val navListener = NavigationBarView.OnItemSelectedListener { item ->
        Log.d("MainActivity", "Item diklik: ${item.itemId}")
        val fragment = when (item.itemId) {
            R.id.nav_home -> homeFragment
            R.id.nav_barang -> barangkuFragment
            R.id.nav_wishlist -> wishlistFragment
            R.id.nav_profile -> profileFragment
            else -> null
        }
        if (fragment != null) {
            Log.d("MainActivity", "Membuka fragment: ${fragment.javaClass.simpleName}")
            openFragment(fragment)
            true
        } else {
            Log.d("MainActivity", "Item tidak dikenali: ${item.itemId}")
            false
        }
    }

    /* ------------------------------------------------------------- */
    /*  Fragment helper                                              */
    /* ------------------------------------------------------------- */
    private fun openFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null) // Tambahkan ke back stack untuk mendukung tombol kembali
            .commit()
        return true
    }

    /* ------------------------------------------------------------- */
    /*  Utility untuk mengatur bottom navigation                     */
    /* ------------------------------------------------------------- */
    fun setBottomNavSelected(itemId: Int) {
        if (binding.bottomNavigation.selectedItemId != itemId) {
            binding.bottomNavigation.selectedItemId = itemId
        }
    }

    fun showBottomNav(show: Boolean = true) {
        binding.bottomNavigation.isVisible = show
    }

    /* ------------------------------------------------------------- */
    /*  Tangani tombol kembali untuk kembali ke HomeFragment         */
    /* ------------------------------------------------------------- */
    override fun onBackPressed() {
        if (binding.bottomNavigation.selectedItemId != R.id.nav_home) {
            openFragment(homeFragment)
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}