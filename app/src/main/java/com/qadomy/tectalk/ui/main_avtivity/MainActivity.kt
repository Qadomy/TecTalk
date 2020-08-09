package com.qadomy.tectalk.ui.main_avtivity

import android.os.Bundle
import android.util.Log.d
import android.util.Log.w
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentReference
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.ActivityMainBinding
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.FireStoreUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
        FireStoreUtil.firestoreInstance.collection("users").document(it)
    }

    //    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        try {
            // firestore database
            val db = FireStoreUtil.firestoreInstance
            db.collection("users").document(AuthUtil.getAuthId()).update("status", true)

            // check database, store time in database and update status to online

            checkDatabaseFirebase()
        } catch (e: Exception) {
            d(TAG, "onCreate: ${e.message}")
        }

        // set data binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)


        // todo: maybe error here
//        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)


        // setSupportActionBar
        setSupportActionBar(binding.toolbar)


        // hide toolbar on sign's up, login fragments
        hideToolbar()

        //setup toolbar with navigation
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

    } // end onCreate


    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.chatFragment -> {
                    d(TAG, "chat fragment clicked")
                    val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    if (isValidDestination(R.id.chatFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.chatFragment, null, navOptions)
                    }
                }
                R.id.groupFragment -> {
                    d(TAG, "group fragment clicked")
                    if (isValidDestination(R.id.groupFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.groupFragment)
                    }
                }
                R.id.ARselfieFragment -> {
                    if (isValidDestination(R.id.ARselfieFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.ARselfieFragment)
                    }
                }
                R.id.searchFragment -> {
                    if (isValidDestination(R.id.searchFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.searchFragment)
                    }
                }
                R.id.profileFragment -> {
                    if (isValidDestination(R.id.profileFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.profileFragment)
                    }
                }
            }
            it.isChecked = true
            false
        }


    private fun isValidDestination(destination: Int): Boolean {
        return destination != Navigation.findNavController(this, R.id.nav_host_fragment)
            .currentDestination!!.id
    }


    /** hide toolbar on sign's up, login fragments */
    private fun hideToolbar() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.label == "SignupFragment" || destination.label == "LoginFragment") {
                binding.toolbar.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.VISIBLE
            }
        }
    }

    private fun checkDatabaseFirebase() {
        // firebase database
        val database = FirebaseDatabase.getInstance()

        // Stores the timestamp of my last disconnect (the last time I was seen online)
        val lastOnlineRef = database.getReference("/users/${AuthUtil.getAuthId()}/lastOnline")
        val status = database.getReference("/users/${AuthUtil.getAuthId()}/status")

        // save connected reference to firebase database
        val connectedRef = database.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                w(TAG, "Listener was cancelled at .info/connected: ERROR: ${error.message}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val connected: Boolean = (snapshot.value ?: false) as Boolean
                if (connected) {
                    status.setValue("online")

                    // When I disconnect, update the last time I was seen online
                    lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP)
                    status.onDisconnect().setValue("offline")
                }
            }
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

