package com.example.leaguepro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*



// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AllLeagueFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllLeagueFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var leagueRecyclerView: RecyclerView
    private lateinit var leagueList: ArrayList<League>
    private lateinit var adapter: LeagueAdapter
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_league, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AllLeagueFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AllLeagueFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leagueList= ArrayList()

        /*
        leagueList.add(League("prova","prova","prova","prova",
            "prova","prova","prova",null,null,null,"qwerty"))
        leagueList.add(League("prova2","prova","prova","prova",
            "prova","prova","prova",null,null,null,"qwerty"))
        leagueList.add(League("prova3","prova","prova","prova",
            "prova","prova","prova",null,null,null,"qwerty"))
        */

        leagueRecyclerView = view.findViewById(R.id.leagueRecyclerView)
        leagueRecyclerView.layoutManager = LinearLayoutManager(context)
        leagueRecyclerView.hasFixedSize()

        adapter = LeagueAdapter(requireContext(), leagueList)
        leagueRecyclerView.adapter = adapter

        mAuth = FirebaseAuth.getInstance()
        // creo collegamento con il database
        mDbRef = FirebaseDatabase.getInstance().getReference()

        // Aggiungi un listener per leggere i dati
        mDbRef.child("leagues").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Svuota la lista attuale dei tornei
                leagueList.clear()

                // Itera su tutti i figli della snapshot
                for (leagueSnapshot in snapshot.children) {
                    // Ottieni l'oggetto League
                    val league = leagueSnapshot.getValue(League::class.java)
                    // Aggiungi il torneo alla lista
                    if (league != null) {
                        leagueList.add(league)
                    }
                }

                // Notifica l'adapter che i dati sono cambiati
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore se necessario
            }
        })



    }




}