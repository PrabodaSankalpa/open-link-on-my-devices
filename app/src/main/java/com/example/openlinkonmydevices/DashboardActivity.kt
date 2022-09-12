package com.example.openlinkonmydevices

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.ArrayAdapter
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
import android.text.Editable

import android.util.Patterns

import android.text.TextWatcher
import android.view.View
import android.webkit.URLUtil.isValidUrl
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import java.time.LocalDateTime
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback


class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    private var backPressTime = 0L
    private var nameOfTheCollection: String = ""
    private var nameOfTheLinkCollection: String = ""
    private var receiverDeviceName: String = ""
    private var userDeviceId: String = ""
    var deviceList = listOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setTheme(R.style.Theme_OpenLinkOnMyDevices)
        setContentView(binding.root)

        MobileAds.initialize(this@DashboardActivity)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        var interstitialAdRequest = AdRequest.Builder().build()
        InterstitialAd.load(this@DashboardActivity,"ca-app-pub-3940256099942544/1033173712", interstitialAdRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })

        RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })

        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        nameOfTheCollection = getCollectionName()
        nameOfTheLinkCollection = getLinksCollectionName()
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

        //Get All the devices list and append to the spinner
        getAllOtherDevices()

        //Get Receive Link
        var receiveLink = intent.getStringExtra("EXTRA_INCOME_LINK")
        if (receiveLink != null){
            binding.etUrl.setText(receiveLink)
        }
        binding.etUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            // whenever text size changes it will check
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // if text written matches the pattern then
                // it will show a toast of pattern matches
                if (Patterns.WEB_URL.matcher(binding.etUrl.getText().toString()).matches()) {
                    Toast.makeText(this@DashboardActivity, "Perfect URL", Toast.LENGTH_SHORT).show()
                } else {
                    // otherwise show error of invalid url
                    binding.etUrl.setError("Invalid URL")
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })

        binding.spinnerDevices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                receiverDeviceName = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        binding.btnSubmit.setOnClickListener {
            val userUrl = binding.etUrl.text.toString().trim()

            if (receiverDeviceName == "-No Other Devices-"){
                Toast.makeText(this@DashboardActivity, "Please configure the receiving device", Toast.LENGTH_SHORT).show()
            }else if (userUrl.isEmpty() || !isValidUrl(userUrl)){
                Toast.makeText(this@DashboardActivity, "Please enter URL", Toast.LENGTH_SHORT).show()
            }else{
                val link = Link(receiverDeviceName,userUrl, LocalDateTime.now())
                saveLink(link)
                binding.etUrl.getText().clear()
            }
        }

        binding.ivInfo.setOnClickListener{
            Intent(this@DashboardActivity, InfoActivity::class.java).also {
                startActivity(it)
            }
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
    private fun getLinksCollectionName(): String {
        val firebaseUser = firebaseAuth.currentUser
        var userCustomCollection = ""
        if (firebaseUser != null) {
            userCustomCollection = firebaseUser.uid + "_links"
        }
        return userCustomCollection
    }

    override fun onBackPressed() {
        if (backPressTime + 2000 > System.currentTimeMillis()){
            if (mRewardedAd != null) {
                mRewardedAd?.show(this@DashboardActivity, OnUserEarnedRewardListener() {
                    Toast.makeText(this@DashboardActivity, "Now you can close the add", Toast.LENGTH_SHORT).show()
                })
            }
            super.onBackPressed()
        }else{
            Toast.makeText(this@DashboardActivity, "Press Again to exit", Toast.LENGTH_SHORT).show()
        }
        backPressTime = System.currentTimeMillis()
    }

    private fun getAllOtherDevices() {
        Firebase.firestore.collection(nameOfTheCollection).whereNotEqualTo("deviceId", userDeviceId)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                firebaseFirestoreException?.let {
                    Toast.makeText(this@DashboardActivity, it.message, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                querySnapshot?.let {
                    if (querySnapshot.isEmpty){
                        deviceList = emptyList()
                        deviceList = deviceList + "-No Other Devices-"
                    }else {
                        deviceList = emptyList()
                        for (document in it) {
                            val device = document.toObject<Device>()
                            deviceList = deviceList + device.deviceName
                        }
                    }
                }
                val adapter = ArrayAdapter<String>(this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, deviceList)
                binding.spinnerDevices.adapter = adapter
            }
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
    private fun saveLink(link: Link) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.collection(nameOfTheLinkCollection).add(link).await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@DashboardActivity, "Successful", Toast.LENGTH_SHORT).show()
                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this@DashboardActivity)
                }
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
