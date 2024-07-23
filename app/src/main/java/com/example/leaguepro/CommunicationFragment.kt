package com.example.leaguepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.leaguepro.databinding.FragmentCommunicationsBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
class CommunicationFragment: Fragment() {
    private var _binding: FragmentCommunicationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var adapter: CommunicationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCommunicationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        val leagueId = arguments?.getString("league_id")

        if (leagueId == null) {
            Toast.makeText(context, "League ID not available", Toast.LENGTH_LONG).show()
            return
        }

        database = FirebaseDatabase.getInstance().reference.child("communications")
        val communications = mutableListOf<Communication>()
        adapter = CommunicationAdapter(communications)
        binding.recyclerView.adapter = adapter

        database.orderByChild("leagueId").equalTo(leagueId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                communications.clear()
                for (dataSnapshot in snapshot.children) {
                    val communication = dataSnapshot.getValue(Communication::class.java)
                    if (communication != null) {
                        communications.add(communication)
                    }
                }
                if (communications.isEmpty()) {
                    // Mostra il messaggio di errore se non ci sono comunicazioni
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = "No communications available"
                    binding.recyclerView.visibility = View.GONE
                } else {
                    // Nascondi il messaggio di errore e mostra la lista
                    binding.tvErrorMessage.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load communications", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}