package com.example.cuanmanis.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cuanmanis.R

class PhotoPagerAdapter(private val photos: List<Any>) : RecyclerView.Adapter<PhotoPagerAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_pager, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoPagerAdapter.PhotoViewHolder, position: Int) {
        val photoUrl = photos[position]
        Glide.with(holder.itemView.context)
            .load(photoUrl)
            .placeholder(R.drawable.sample_item) // Ganti dengan resource yang ada
            .error(R.drawable.sample_item) // Ganti dengan resource yang ada
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivPhoto)
    }
}