package com.mhmdawad.chatme.ui.main_Page

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.mhmdawad.chatme.MainChatAdapter
import com.mhmdawad.chatme.R
import com.mhmdawad.chatme.pojo.MainChatData
import com.mhmdawad.chatme.pojo.UserChatData
import com.mhmdawad.chatme.ui.FragmentTabsAdapter
import com.mhmdawad.chatme.ui.MainCallsFragment
import com.mhmdawad.chatme.ui.MainMessagesFragment
import com.mhmdawad.chatme.ui.MainStatusFragment
import com.mhmdawad.chatme.ui.contact.ContactsFragment
import com.mhmdawad.chatme.ui.conversation.ConversationActivity
import com.mhmdawad.chatme.ui.main.MainActivity
import com.mhmdawad.chatme.ui.profile_setting.SettingsFragment
import com.mhmdawad.chatme.utils.RecyclerViewClick


class MainPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)

        addViewPagerFragments()
        Toast.makeText(applicationContext, "HI",Toast.LENGTH_SHORT).show()
    }
    private fun addViewPagerFragments(){
        val viewPager = findViewById<ViewPager>(R.id.mainPageViewPager)
        val tabLayout = findViewById<TabLayout>(R.id.mainPageTabLayout)
        supportActionBar!!.elevation = 0F
        val fragmentAdapter = FragmentTabsAdapter(supportFragmentManager)
        fragmentAdapter.addFragment(MainCallsFragment(), "")
        fragmentAdapter.addFragment(MainMessagesFragment(), "Chat")
        fragmentAdapter.addFragment(MainStatusFragment(), "Status")
        fragmentAdapter.addFragment(MainCallsFragment(), "Calls")
        viewPager.adapter=  fragmentAdapter
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.getTabAt(0)!!.setIcon(R.drawable.main_page_camera)
        val layout = ( ( tabLayout.getChildAt(0) as LinearLayout).getChildAt(0) as LinearLayout)
        val layoutParams =  layout.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 0.4f
        layout.layoutParams = layoutParams
        tabLayout.getTabAt(1)!!.select()
        viewPager.offscreenPageLimit = 4
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_page_menu, menu)
        val searchItem = menu?.findItem(R.id.searchMenuItem)
        val searchView: SearchView = searchItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                MainMessagesFragment.chatAdapter.filter.filter(newText)
                return false
            }
        })
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.signOutMenuItem -> signOut()
            R.id.settingsMenuItem -> profileSettings()
        }
        return true
    }


    private fun signOut() {
        FirebaseDatabase.getInstance().reference.child("Users").child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("")
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun profileSettings() {
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    override fun onPause() {
        super.onPause()
        if(FirebaseAuth.getInstance().uid != null)
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("")
    }
    override fun onResume() {
        super.onResume()
        FirebaseDatabase.getInstance().reference.child("Users")
            .child(FirebaseAuth.getInstance().uid!!)
            .child("mood").setValue("Online")
    }

    override fun onBackPressed() {
        Toast.makeText(applicationContext, "HIr",Toast.LENGTH_SHORT).show()
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment != null) {
                if (fragment is SettingsFragment)
                    fragment.onBackPressed()
                else if (fragment is MainMessagesFragment)
                    fragment.onBackPressed()
                return
            }
        }
        super.onBackPressed()
    }

    interface OnBackButtonPressed {
        fun onBackPressed()
    }
}
