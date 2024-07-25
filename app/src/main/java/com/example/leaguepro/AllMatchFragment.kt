package com.example.leaguepro

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class AllMatchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var matchAdapter: MatchAdapter
    private val matchList = mutableListOf<Match>()
    private lateinit var leagueOwnerId: String

    companion object {
        private const val LEAGUE_ID_KEY = "league_id"

        fun newInstance(leagueId: String): AllMatchFragment {
            return AllMatchFragment().apply {
                arguments = Bundle().apply {
                    putString(LEAGUE_ID_KEY, leagueId)
                }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_match, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val leagueId = arguments?.getString(LEAGUE_ID_KEY) ?: return
        loadLeagueOwnerId(leagueId)
    }

    private fun loadLeagueOwnerId(leagueId: String) {
        val database = FirebaseDatabase.getInstance()
        val leagueRef = database.getReference("leagues").child(leagueId)

        leagueRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val league = dataSnapshot.getValue(League::class.java)
                leagueOwnerId = league?.leagueManager ?: return
                setupRecyclerView()
                loadMatchesFromDatabase(leagueId)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("RealtimeDB", "loadLeagueOwnerId:onCancelled", databaseError.toException())
            }
        })
    }

    private fun setupRecyclerView() {
        val leagueId = arguments?.getString(LEAGUE_ID_KEY) ?: return
        matchAdapter = MatchAdapter(matchList, leagueOwnerId, leagueId)
        recyclerView.adapter = matchAdapter
    }
    private fun loadMatchesFromDatabase(leagueId: String) {
        val database = FirebaseDatabase.getInstance()
        val matchRef = database.getReference("matches").child(leagueId)

        matchRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                matchList.clear()
                for (matchSnapshot in dataSnapshot.children) {
                    val match = matchSnapshot.getValue(Match::class.java)
                    val matchId = matchSnapshot.key // Get the ID of the match
                    if (match != null) {
                        match.id = matchId // Set the ID in the match object
                        matchList.add(match)
                    }
                }
                matchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("RealtimeDB", "loadMatches:onCancelled", databaseError.toException())
            }
        })
    }
}
