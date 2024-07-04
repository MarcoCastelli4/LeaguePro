package com.example.leaguepro

import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.leaguepro.databinding.ActivityMainBinding

object NavigationManager {

    fun replaceFragment(activity: AppCompatActivity, fragment: Fragment) {
        val fragmentManager = activity.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }

    fun showIndicator(activity: AppCompatActivity, binding: ActivityMainBinding, item: MenuItem) {
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
