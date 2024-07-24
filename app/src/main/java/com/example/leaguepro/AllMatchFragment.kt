package com.example.leaguepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.leaguepro.R

class AllMatchFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var matchList: List<Match>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_match, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize match list
        matchList = listOf(
            Match("1", Team("United Darfo"), Team("US SanPancrazio"), "Tue 12/06", "20:30", Pair(7, 4)),
            Match("2", Team("Partizan Cogno"), Team("AC Castenedolo"), "Tue 12/06", "21:30", Pair(6, 0)),
            Match("3", Team("Sassuolo Bassa"), Team("Metalcam spa"), "Tue 24/06", "20:30", Pair(0, 0)),
            Match("4", Team("Padova Juniores"), Team("City Rome"), "Tue 12/06", "21:30", Pair(0, 0))
        )

        matchAdapter = MatchAdapter(matchList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = matchAdapter
    }
}
