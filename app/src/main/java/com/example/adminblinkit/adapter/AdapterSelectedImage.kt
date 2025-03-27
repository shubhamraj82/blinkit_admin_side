package com.example.adminblinkit.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.adminblinkit.databinding.ItemSelectedImagesBinding

class AdapterSelectedImage(val imageUris : ArrayList<Uri>) : RecyclerView.Adapter<AdapterSelectedImage.SelectedImageViewHolder>() {

    // ViewHolder class - holds references to all the views in the specific item layout
    class SelectedImageViewHolder(val binding : ItemSelectedImagesBinding) : RecyclerView.ViewHolder(binding.root)

    // inflate the view for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedImageViewHolder {
        return SelectedImageViewHolder(ItemSelectedImagesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    // set the data for each view in recycler view
    override fun onBindViewHolder(holder: SelectedImageViewHolder, position: Int) {
        val image = imageUris[position]
        holder.binding.apply {
            ivProductImage.setImageURI(image)
        }

        holder.binding.closeImage.setOnClickListener {
            if(position < imageUris.size) {
                imageUris.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

}