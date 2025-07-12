package com.example.cuanmanis.model

data class Wishlist(
    var userId: String = "",
    var barangId: String = "",       // contoh: ID barang wishlist
    var createdAt: Long = System.currentTimeMillis() // contoh: timestamp
)