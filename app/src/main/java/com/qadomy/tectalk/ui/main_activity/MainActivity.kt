package com.qadomy.tectalk.ui.main_activity

import android.content.Context
import android.os.Bundle
import android.util.Log.d
import android.util.Log.w
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentReference
import com.qadomy.tectalk.R
import com.qadomy.tectalk.databinding.ActivityMainBinding
import com.qadomy.tectalk.utils.AuthUtil
import com.qadomy.tectalk.utils.FireStoreUtil
import com.qadomy.tectalk.utils.event_buses.ConnectionChangeEvent
import com.qadomy.tectalk.utils.event_buses.KeyboardEvent
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    private var userDocRef: DocumentReference? = AuthUtil.getAuthId().let {
        FireStoreUtil.firestoreInstance.collection("users").document(it)
    }

    private var isActivityRecreated = false

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    // onDestroy, unregister EventBus
    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

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

        // init shared view model
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)


        // setSupportActionBar
        setSupportActionBar(binding.toolbar)

        //register to event bus to receive callbacks
        EventBus.getDefault().register(this)

        // hide toolbar on sign's up, login fragments
        hideToolbarInLoginAndSignup()

        //setup toolbar with navigation
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment, R.id.loginFragment))
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

    } // end onCreate

    // Show snackBar whenever the connection state changes
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onConnectionChangeEvent(event: ConnectionChangeEvent): Unit {
        if (!isActivityRecreated) {//to not show toast on configuration changes
            Snackbar.make(binding.coordinator, event.message, Snackbar.LENGTH_LONG).show()
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onKeyboardEvent(event: KeyboardEvent) {
        hideKeyboard()
    }

    // hide keyboard
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.toolbar.windowToken, 0)

    }

    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.chatFragment -> {
                    val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    if (isValidDestination(R.id.homeFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.homeFragment, null, navOptions)
                    }
                }
                R.id.groupFragment -> {
                    if (isValidDestination(R.id.groupFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.groupFragment)
                    }
                }
                R.id.ArSelfieHome -> {
                    if (isValidDestination(R.id.ArSelfieHome)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.ArSelfieHome)
                    }
                }
                R.id.findUserFragment -> {
                    if (isValidDestination(R.id.findUserFragment)) {
                        Navigation.findNavController(this, R.id.nav_host_fragment)
                            .navigate(R.id.findUserFragment)
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
    private fun hideToolbarInLoginAndSignup() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.label == getString(R.string.sign_up) || destination.label == getString(R.string.login)) {
                binding.toolbar.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.VISIBLE
            }
        }
    }

    // set last online, and current status to database firebase
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

