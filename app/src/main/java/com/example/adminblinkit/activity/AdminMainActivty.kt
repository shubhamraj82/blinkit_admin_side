package com.example.adminblinkit.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.example.adminblinkit.R
import com.example.adminblinkit.databinding.ActivityAdminMainActivtyBinding

class AdminMainActivty : AppCompatActivity() {
    private lateinit var binding : ActivityAdminMainActivtyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminMainActivtyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NavigationUI.setupWithNavController(binding.bottomMenu, Navigation.findNavController(this,
            R.id.fragmentContainerView2
        ))
    }
}