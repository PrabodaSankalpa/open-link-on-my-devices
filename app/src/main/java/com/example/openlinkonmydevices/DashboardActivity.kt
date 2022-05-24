package com.example.openlinkonmydevices

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.openlinkonmydevices.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private val deviceCollectionRef = Firebase.firestore.collection("devices")
    private var backPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_OpenLinkOnMyDevices)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        val logOutDialog = AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure want to logout?")
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton("Logout"){_, _ ->
                firebaseAuth.signOut()
                checkUser()
            }
            .setNegativeButton("No"){_, _ ->
                Toast.makeText(this@DashboardActivity, "Thank You!", Toast.LENGTH_SHORT).show()
            }.create()

        binding.ivLogOut.setOnClickListener {
            logOutDialog.show()
        }

    }

    override fun onBackPressed() {
        if (backPressTime + 2000 > System.currentTimeMillis()){
            super.onBackPressed()
        }else{
            Toast.makeText(this@DashboardActivity, "Press Again to exit", Toast.LENGTH_SHORT).show()
        }
        backPressTime = System.currentTimeMillis()
    }


    private fun saveDevice(device: Device) = CoroutineScope(Dispatchers.IO).launch {

    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }else{
            val name = "Hello, " + firebaseUser.displayName
            binding.tvDisplayName.text = name
        }
    }
}