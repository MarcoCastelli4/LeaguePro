package com.example.leaguepro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.leaguepro.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigationView: BottomNavigationView = binding.bottomNavigationView

        when (UserInfo.userType) {
            getString(R.string.LeagueManager) -> {
                bottomNavigationView.inflateMenu(R.menu.league_nav_menu)
            }
            getString(R.string.TeamManager) -> {
                bottomNavigationView.inflateMenu(R.menu.team_nav_menu)
            }
            else -> {
                bottomNavigationView.inflateMenu(R.menu.visitor_nav_menu) // Fallback menu
            }
        }

        // Load first page
        if (UserInfo.logged) {
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

                R.id.goBack ->{
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Set the initial selected item
        bottomNavigationView.selectedItemId = R.id.myleague
    }
}
