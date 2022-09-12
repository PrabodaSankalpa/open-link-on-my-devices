package com.example.openlinkonmydevices

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import com.example.openlinkonmydevices.databinding.ActivityDashboardBinding
import com.example.openlinkonmydevices.databinding.ActivityInfoBinding
import com.example.openlinkonmydevices.databinding.ActivityMainBinding

class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_OpenLinkOnMyDevices)
        setContentView(binding.root)

        binding.tvLinks.movementMethod = LinkMovementMethod.getInstance()
        binding.tvLinks.setLinkTextColor(Color.RED)

        binding.ivBackBtn.setOnClickListener{
            this@InfoActivity.finish()
        }
    }
}