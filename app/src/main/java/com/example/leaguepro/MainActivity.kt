package com.example.leaguepro

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.leaguepro.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Imposta il frammento iniziale
        replaceFragment(MyLeagueFragment())
        binding.bottomNavigationView.post {
            val item = binding.bottomNavigationView.menu.findItem(R.id.myleague)
            showIndicator(item)
        }

        // Imposta il listener per la selezione degli elementi nel menu di navigazione in basso
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.myleague -> {
                    replaceFragment(MyLeagueFragment())
                    showIndicator(item)
                    true
                }
                R.id.allLeague -> {
                    replaceFragment(AllLeagueFragment())
                    showIndicator(item)
                    true
                }
                R.id.myteam -> {
                    replaceFragment(MyTeamFragment())
                    showIndicator(item)
                    true
                }
                R.id.profile -> {
                    replaceFragment(ProfileFragment())
                    showIndicator(item)
                    true
                }
                else -> false
            }
        }

        // Aggiorna l'indicatore iniziale
        binding.bottomNavigationView.selectedItemId = R.id.myleague
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    private fun showIndicator(item: MenuItem) {
        // Ottieni la View associata all'elemento di menu selezionato
        val itemView = binding.bottomNavigationView.findViewById<View>(item.itemId)
        itemView.post {
            // Calcola la larghezza e la posizione della View dell'elemento di menu selezionato
            val width = itemView.width
            val x = itemView.left

            // Aggiorna la larghezza e la posizione della barra di selezione
            binding.selectionIndicator.layoutParams.width = width
            binding.selectionIndicator.x = x.toFloat()
            binding.selectionIndicator.visibility = View.VISIBLE
            binding.selectionIndicator.requestLayout()
        }
    }
}
