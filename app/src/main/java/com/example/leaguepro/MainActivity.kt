package com.example.leaguepro

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.leaguepro.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load the appropriate menu based on user type
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userType = sharedPreferences.getString("user_type", "default_type")

        val bottomNavigationView: BottomNavigationView = binding.bottomNavigationView

        when (userType) {
            "League Manager" -> {
                bottomNavigationView.inflateMenu(R.menu.league_nav_menu)
            }
            "Team Manager" -> {
                bottomNavigationView.inflateMenu(R.menu.team_nav_menu)
            }
            else -> {
                bottomNavigationView.inflateMenu(R.menu.visitor_nav_menu) // Fallback menu
            }
        }

        if (userType.equals("League Manager") or userType.equals("Team Manager")) {
            // Set the initial fragment
            NavigationManager.replaceFragment(this, MyLeagueFragment())
            bottomNavigationView.post {
                val item = bottomNavigationView.menu.findItem(R.id.myleague)
                NavigationManager.showIndicator(this, binding, item)
            }
        }
        else{
            NavigationManager.replaceFragment(this, AllLeagueFragment())
            bottomNavigationView.post {
                val item = bottomNavigationView.menu.findItem(R.id.allLeague)
                NavigationManager.showIndicator(this, binding, item)
            }
        }

        // Set listener for bottom navigation item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.myleague -> {
                    NavigationManager.replaceFragment(this, MyLeagueFragment())
                    NavigationManager.showIndicator(this, binding, item)
                    true
                }
                R.id.allLeague -> {
                    NavigationManager.replaceFragment(this, AllLeagueFragment())
                    NavigationManager.showIndicator(this, binding, item)
                    true
                }
                R.id.myteam -> {
                    NavigationManager.replaceFragment(this, MyTeamFragment())
                    NavigationManager.showIndicator(this, binding, item)
                    true
                }
                R.id.profile -> {
                    NavigationManager.replaceFragment(this, ProfileFragment())
                    NavigationManager.showIndicator(this, binding, item)
                    true
                }

                else -> false
            }
        }

        // Set the initial selected item
        bottomNavigationView.selectedItemId = R.id.myleague
    }
}
