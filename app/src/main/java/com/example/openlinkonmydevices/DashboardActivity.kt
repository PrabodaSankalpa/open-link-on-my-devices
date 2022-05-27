package com.example.openlinkonmydevices

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.openlinkonmydevices.databinding.ActivityDashboardBinding
import com.example.openlinkonmydevices.databinding.EditDeviceNameBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private var backPressTime = 0L
    private var nameOfTheCollection: String = ""
    private var userDeviceId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_OpenLinkOnMyDevices)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        nameOfTheCollection = getCollectionName()
        userDeviceId = getDeviceId(this)

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

        //On load function to get device information
        //retrieveDevice()
        realtimeGetDeviceName()

        //Update device name
        binding.ivUpdateDeviceName.setOnClickListener {
            val mBuilder = AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_edit)
                .setTitle("Update Device Name")

            val li = LayoutInflater.from(this)
            val editDeviceNameBinding = EditDeviceNameBinding.inflate(li)
            mBuilder.setView(editDeviceNameBinding.root)

            mBuilder.setPositiveButton("Update"){_, _ ->
                val deviceNewName = editDeviceNameBinding.etDeviceName.text.toString().trim()
                updateDeviceName(deviceNewName)
            }
            mBuilder.create()
            mBuilder.show()
        }

    }

    private fun getDeviceId(context: Context): String{
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun getCollectionName(): String {
        val firebaseUser = firebaseAuth.currentUser
        var userCustomCollection = ""
           if (firebaseUser != null) {
                userCustomCollection = firebaseUser.uid + "_devices"
            }
        return userCustomCollection
    }

    override fun onBackPressed() {
        if (backPressTime + 2000 > System.currentTimeMillis()){
            super.onBackPressed()
        }else{
            Toast.makeText(this@DashboardActivity, "Press Again to exit", Toast.LENGTH_SHORT).show()
        }
        backPressTime = System.currentTimeMillis()
    }

    private fun updateDeviceName(deviceNewName: String) = CoroutineScope(Dispatchers.IO).launch {
        val deviceQuery = Firebase.firestore.collection(nameOfTheCollection)
            .whereEqualTo("deviceId", userDeviceId)
            .get()
            .await()
        if (deviceQuery.documents.isNotEmpty()) {
            for (document in deviceQuery) {
                try {

                    val map = mutableMapOf<String, Any>()
                    if (deviceNewName != "") {
                        map["deviceName"] = deviceNewName

                        Firebase.firestore.collection(nameOfTheCollection).document(document.id)
                            .set(
                                map,
                                SetOptions.merge()
                            ).await()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun realtimeGetDeviceName(){
        Firebase.firestore.collection(nameOfTheCollection).addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                Toast.makeText(this@DashboardActivity, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            if(querySnapshot == null){
                val device = Device(userDeviceId, "My Mobile")
                saveDevice(device)
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it){
                    val device = document.toObject<Device>()
                        if (device.deviceId == userDeviceId){
                            sb.append(device.deviceName)
                        }
                }
                if(sb.isEmpty()){
                    val device = Device(userDeviceId, "My Mobile")
                    saveDevice(device)

                    for (document in it){
                        val device = document.toObject<Device>()
                            if (device.deviceId == userDeviceId){
                                sb.append(device.deviceName)
                            }
                    }
                }
                binding.tvDeviceName.text = sb.toString()
            }
        }
    }


//    private fun retrieveDevice() = CoroutineScope(Dispatchers.IO).launch {
//        try{
//            var querySnapshot = Firebase.firestore.collection(nameOfTheCollection).get().await()
//            val sb = StringBuilder()
//            if(querySnapshot.isEmpty){
//                val device = Device(userDeviceId, "My Mobile")
//                saveDevice(device)
//                querySnapshot = Firebase.firestore.collection(nameOfTheCollection).get().await()
//            }
//            for (document in querySnapshot.documents){
//                val device = document.toObject<Device>()
//                if (device != null) {
//                    if (device.deviceId == userDeviceId){
//                        sb.append(device.deviceName)
//                    }
//                }
//
//            }
//            if(sb.isEmpty()){
//                val device = Device(userDeviceId, "My Mobile")
//                saveDevice(device)
//                querySnapshot = Firebase.firestore.collection(nameOfTheCollection).get().await()
//
//                for (document in querySnapshot.documents){
//                    val device = document.toObject<Device>()
//                    if (device != null) {
//                        if (device.deviceId == userDeviceId){
//                            sb.append(device.deviceName)
//                        }
//                    }
//
//                }
//
//            }
//
//            withContext(Dispatchers.Main){
//                binding.tvDeviceName.text = sb.toString()
//            }
//
//        }catch (e: Exception){
//            withContext(Dispatchers.Main){
//                Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private fun saveDevice(device: Device) = CoroutineScope(Dispatchers.IO).launch {
            try {
                Firebase.firestore.collection(nameOfTheCollection).add(device).await()
                withContext(Dispatchers.Main){
                    Toast.makeText(this@DashboardActivity, "Device Added", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    Toast.makeText(this@DashboardActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
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