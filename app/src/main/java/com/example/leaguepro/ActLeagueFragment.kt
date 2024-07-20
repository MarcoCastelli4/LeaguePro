package com.example.leaguepro

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ActLeagueFragment : Fragment() {

    private var leagueId: String? = null
    private lateinit var layout_calendar: LinearLayout
    private lateinit var btn_createCalendar: ImageView
    private lateinit var mDbRef: DatabaseReference
    private lateinit var calendar: List<Match>

    companion object {
        @JvmStatic
        fun newInstance(league: League): ActLeagueFragment {
            val fragment = ActLeagueFragment()
            val args = Bundle().apply {
                putString("league_id", league.uid) // Assuming uid is a String; adjust accordingly
                // Add other data you need to pass to the fragment
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            leagueId = it.getString("league_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_act_league, container, false)
        return view
    }

    // TODO check if user is logged
    // if user is league manager can create calendar
    // update the bar to match

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDbRef = FirebaseDatabase.getInstance().getReference()
        setupView(view)
    }

    private fun setupView(view: View) {

        layout_calendar=view.findViewById(R.id.create_calendar)
        btn_createCalendar=view.findViewById(R.id.add_calendar)

        // aggiungere che il calendario non sia già stato creato e che siamo al giorno di inizio del torneo
        /*
        if(UserInfo.userType==(getString(R.string.LeagueManager))){
            layout_calendar.visibility=view.visibility
        }*/
        layout_calendar.visibility=view.visibility

        btn_createCalendar.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result = fetchLeagueAndTeams(leagueId!!, requireContext())
                    if (result == null) return@launch // Early return if no teams are found or an error occurred

                    val (league, teams) = result
                        // Print league and teams details in the log
                        league?.let { Log.d("ActLeagueFragment", "League Name: ${it.name}") }
                        teams!!.forEach { team ->
                            Log.d(
                                "ActLeagueFragment",
                                "Team Name: ${team.name}"
                            )
                        }

                        // Create a test league and teams for testing
                        val pl: ArrayList<Player> = ArrayList()
                        val leagueProva = League(
                            "1",
                            "A",
                            "",
                            1.0f,
                            "",
                            "",
                            "",
                            "",
                            "15/07/2024 - 30/07/2024",
                            "1",
                            8.0f
                        )
                        val teamA = Team("1", "A", "A", pl)
                        val teamB = Team("2", "B", "A", pl)
                        val teamC = Team("3", "C", "A", pl)
                        val teamD = Team("4", "D", "A", pl)
                        val teamE = Team("5", "E", "A", pl)
                        val teamF = Team("6", "F", "A", pl)
                        val teamG = Team("7", "G", "A", pl)
                        val teamH = Team("8", "H", "A", pl)

                        val teamListProva =
                            arrayListOf(teamA, teamB, teamC, teamD, teamE, teamF, teamG, teamH)

                        calendar = createCalendar(leagueProva, teamListProva)

                        // Print the calendar matches in the log
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        for (match in calendar) {
                            Log.d(
                                "Calendar",
                                "Match: ${match.team1!!.name} vs ${match.team2!!.name} on ${
                                    dateFormat.format(match.date)
                                }"
                            )
                        }

                } catch (e: Exception) {
                    // Gestire eventuali errori
                    Log.e("ActLeagueFragment", "Error getting league and teams", e)
                }
            }
        }

    }

    private fun createCalendar(league: League, teams: List<Team>): List<Match> {
        val matches = ArrayList<Match>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val (startDate, endDate) = splitPlayingPeriod(league.playingPeriod!!)
            ?: throw IllegalArgumentException("Invalid playing period format")

        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.set(Calendar.HOUR_OF_DAY, 18) // Start matches from 18:00 each day

        val teamSchedule = mutableMapOf<String, MutableSet<String>>() // To track matches for each team

        // Initialize team schedule map
        for (team in teams) {
            teamSchedule[team.uid!!] = mutableSetOf()
        }

        val matchDays = mutableSetOf<String>() // To track the days when matches are scheduled

        // Generate matches
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                val team1 = teams[i]
                val team2 = teams[j]

                // Ensure each team plays exactly once against each other
                if (team1.uid in teamSchedule && team2.uid in teamSchedule &&
                    team2.uid !in teamSchedule[team1.uid]!! && team1.uid !in teamSchedule[team2.uid]!!
                ) {
                    // Find the next available date for scheduling
                    while (true) {
                        val matchDate = dateFormat.format(calendar.time)
                        if (matchDate !in matchDays) {
                            // Schedule the match
                            val matchTime = timeFormat.format(calendar.time)
                            val match = Match(
                                team1 = team1,
                                team2 = team2,
                                date = matchDate,
                                time = matchTime,
                                result = null
                            )

                            matches.add(match)
                            teamSchedule[team1.uid]!!.add(team2.uid!!)
                            teamSchedule[team2.uid]!!.add(team1.uid!!)

                            matchDays.add(matchDate)
                            break
                        }
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 18) // Reset time for the next day
                    }

                    calendar.add(Calendar.HOUR_OF_DAY, 1)

                    // Check if the match time exceeds 21:00, move to next day starting from 18:00
                    if (calendar.get(Calendar.HOUR_OF_DAY) >= 21) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 18)
                    }

                    // Reset the calendar if it exceeds the end date
                    if (calendar.time.after(endDate)) {
                        calendar.time = startDate
                        calendar.set(Calendar.HOUR_OF_DAY, 18)
                    }
                }
            }
        }

        return matches
    }

    private fun splitPlayingPeriod(playingPeriod: String): Pair<Date, Date>? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dates = playingPeriod.split(" - ")

        return if (dates.size == 2) {
            val startDate = dateFormat.parse(dates[0])
            val endDate = dateFormat.parse(dates[1])
            if (startDate != null && endDate != null) {
                Pair(startDate, endDate)
            } else {
                null
            }
        } else {
            null
        }
    }
    private suspend fun fetchLeagueAndTeams(leagueId: String, context: Context): Pair<League?, List<Team>?>? {
        return withContext(Dispatchers.IO) {
            var league: League? = null
            val teamsList = mutableListOf<Team>()

            try {
                // Fetch league details
                val leagueSnapshot = mDbRef.child("leagues").child(leagueId).get().await()
                league = leagueSnapshot.getValue(League::class.java)

                // Fetch teams associated with the league
                val teamsSnapshot = mDbRef.child("leagues_team")
                    .orderByChild("league_id")
                    .equalTo(leagueId)
                    .get()
                    .await()
                for (teamLeagueSnapshot in teamsSnapshot.children) {
                    val teamId = teamLeagueSnapshot.child("team_id").getValue(String::class.java)
                    if (teamId != null) {
                        val teamSnapshot = mDbRef.child("teams").child(teamId).get().await()
                        // Handle the team snapshot data
                        val teamMap = teamSnapshot.getValue<Map<String, Any>>() ?: continue

                        // Extract and convert the players map
                        val playersMap = teamSnapshot.child("players").children.map { playerSnapshot ->
                            playerSnapshot.getValue(Player::class.java)
                        }.filterNotNull()

                        val team = Team(
                            uid = teamMap["uid"] as String?,
                            name = teamMap["name"] as String?,
                            team_manager = teamMap["team_manager"] as String?,
                            players = ArrayList(playersMap)
                        )
                        teamsList.add(team)
                    }
                }

                if (league != null) {
                    // aggiungere con or or (teamsList.size<league.maxNumberTeam!!)
                    if (teamsList.isEmpty() ) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Impossible create a league not enough team!", Toast.LENGTH_SHORT).show()
                        }
                        return@withContext null
                    }
                }

                return@withContext Pair(league, teamsList)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error fetching league and teams: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ActLeagueFragment", "Error getting league and teams", e)
                }
                return@withContext null
            }
        }
    }




}
