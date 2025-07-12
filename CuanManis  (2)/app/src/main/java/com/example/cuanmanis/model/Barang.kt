package com.example.cuanmanis.model

import com.google.firebase.Timestamp

data class Barang(
    var id: String = "",           // ← selalu string, tidak bisa null
    var nama: String = "",
    var kategori: String = "",
    var lokasi: String = "",
    var harga: Int? = null,
    var kondisi: String = "" ,
    var deskripsi: String = "",
    var waktuPosting: Timestamp? = null,
    var foto: List<String> = listOf(),
    var userId: String = ""
)