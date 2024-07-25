package com.example.leaguepro

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.leaguepro.databinding.InfoLeagueBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random


class ActLeagueFragment : Fragment() {

    private var leagueId: String? = null
    private lateinit var layout_calendar: LinearLayout
    private lateinit var layout_chat: LinearLayout
    private lateinit var btn_createCalendar: ImageView
    private lateinit var layout_communication: LinearLayout
    private lateinit var btn_addCommunication: ImageView
    private lateinit var btn_addChat: ImageView
    private lateinit var mDbRef: DatabaseReference
    private lateinit var calendar: List<Match>
    private lateinit var binding: InfoLeagueBinding
    private var currentMenuItemId: Int = R.id.match
    private var leagueOwnerId: String? = null



    companion object {
        @JvmStatic
        fun newInstance(league: League): ActLeagueFragment {
            val fragment = ActLeagueFragment()
            val args = Bundle().apply {
                putString("league_id", league.uid) // Assuming uid is a String; adjust accordingly
                putString("league_name", league.name) // Assuming league.name is available
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
        // Initialize ViewBinding
        binding = InfoLeagueBinding.inflate(inflater, container, false)
        val view = binding.root

        // Imposta il nome e l'immagine della lega
        arguments?.let {
            val leagueName = it.getString("league_name")
            binding.leagueName.text = leagueName
        }

        // Configura il menu di navigazione
        binding.upperNavigationView.inflateMenu(R.menu.league_upper_nav_menu)
        binding.upperNavigationView.setOnItemSelectedListener { item ->
            currentMenuItemId = item.itemId // Aggiorna l'ID dell'elemento selezionato
            updateCreateCalendarButtonVisibility()
            updateCommunicationAndChatButtonVisibility()
            when (item.itemId) {
                R.id.match -> {
                    leagueId?.let { id ->
                        val fragment = AllMatchFragment.newInstance(id)
                        NavigationManager.replaceFragment(this, fragment)
                    } ?: run {
                        Toast.makeText(requireContext(), "League ID not available", Toast.LENGTH_LONG).show()
                    }
                    NavigationManager.showIndicator(binding, item)
                    true
                }
                R.id.leaguetable -> {
                    leagueId?.let { id ->
                        val fragment = LeagueTableFragment().apply {
                            arguments = Bundle().apply {
                                putString("league_id", id)
                            }
                        }
                        NavigationManager.replaceFragment(this, fragment)
                    } ?: run {
                        Toast.makeText(requireContext(), "League ID not available", Toast.LENGTH_LONG).show()
                    }
                    NavigationManager.showIndicator(binding, item)
                    true
                }
                R.id.statistics -> {
                    NavigationManager.showIndicator(binding, item)
                    true
                }
                R.id.comunications -> {
                    leagueId?.let { id ->
                        val fragment = CommunicationFragment().apply {
                            arguments = Bundle().apply {
                                putString("league_id", id)
                            }
                        }
                        NavigationManager.replaceFragment(this, fragment)
                    } ?: run {
                        Toast.makeText(requireContext(), "League ID not available", Toast.LENGTH_LONG).show()
                    }
                        NavigationManager.showIndicator(binding, item)
                        true
                    }
                else -> false
            }
        }
        binding.upperNavigationView.selectedItemId = R.id.match

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDbRef = FirebaseDatabase.getInstance().reference
        setupView(view)
        lifecycleScope.launch {
            try {
                // Recupera solo l'ID del proprietario della lega
                val leagueSnapshot = mDbRef.child("leagues").child(leagueId!!).get().await()
                val league = leagueSnapshot.getValue(League::class.java)
                leagueOwnerId = league?.leagueManager // Imposta l'ID del proprietario della lega

                // Aggiorna la visibilità dei pulsanti in base all'ID del proprietario e al menu corrente
                updateCreateCalendarButtonVisibility()
                updateCommunicationAndChatButtonVisibility()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching league: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ActLeagueFragment", "Error getting league", e)
            }
        }
    }

    private fun setupView(view: View) {
        layout_calendar=view.findViewById(R.id.create_calendar)
        btn_createCalendar=view.findViewById(R.id.add_calendar)
        // aggiungere che il calendario non sia già stato creato e che siamo al giorno di inizio del torneo

        layout_communication = view.findViewById(R.id.layout_add_communication)
        btn_addCommunication = view.findViewById(R.id.add_communication)
        layout_chat=view.findViewById(R.id.layout_chat)
        btn_addChat = view.findViewById(R.id.add_chat)

        // Configura la visibilità iniziale del layout di calendar e communication e chat
        updateCreateCalendarButtonVisibility()
        updateCommunicationAndChatButtonVisibility()
        //click listener per create calendar
        btn_createCalendar.setOnClickListener {
            checkIfCalendarExists(leagueId!!)
        }
        //click listener per il pulsante Add Communication
        layout_communication.setOnClickListener {
                showAddCommunicationDialog()
        }
        btn_addCommunication.setOnClickListener {
            showAddCommunicationDialog()
        }
        // Listener per il pulsante "Add Chat"
        layout_chat.setOnClickListener {
            openChat()
        }
        btn_addChat.setOnClickListener {
            openChat()
        }
    }

    private fun createAndSaveCalendar(leagueId: String){
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
                /*   // Create a test league and teams for testing
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
                */
                calendar = createCalendar(league!!, teams)

                // Print the calendar matches in the log
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                for (match in calendar) {
                    Log.d(
                        "Calendar",
                        "Match: ${match.toString()}"
                    )
                }

                saveMatchesToDatabase(calendar, leagueId!!)
            } catch (e: Exception) {
                // Gestire eventuali errori
                Log.e("ActLeagueFragment", "Error getting league and teams", e)
            }
        }
    }
    private fun checkIfCalendarExists(leagueId: String) {
        val database = FirebaseDatabase.getInstance()
        val matchRef = database.getReference("matches").child(leagueId)

        matchRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Il calendario esiste già
                    Toast.makeText(requireContext(), "The calendar has already been created for this league", Toast.LENGTH_LONG).show()
                } else {
                    // Il calendario non esiste, procedi con la creazione
                    createAndSaveCalendar(leagueId)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("Firebase", "checkIfCalendarExists:onCancelled", databaseError.toException())
            }
        })
    }
    fun saveMatchesToDatabase(matches: List<Match>, leagueId: String) {
        val database = FirebaseDatabase.getInstance()
        val matchRef = database.getReference("matches").child(leagueId)

        matches.forEach { match ->
            match.id = matchRef.push().key // Genera una chiave unica per ogni match
            matchRef.child(match.id!!).setValue(match)
                .addOnSuccessListener {
                    Log.d("RealtimeDB", "Match added with ID: ${match.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("RealtimeDB", "Error adding match", e)
                }
        }
    }
    private fun openChat() {
        // Qui devi inserire il codice per aprire la chat
        // Puoi usare un Intent per avviare una nuova attività o aprire un fragment
        //val intent = Intent(requireContext(), ChatActivity::class.java)
        //startActivity(intent)
    }
    private fun showAddCommunicationDialog() {
        // Inflazione del layout del dialogo
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_communication, null)
        val inputText = dialogView.findViewById<EditText>(R.id.input_communication_text)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        // Costruzione e visualizzazione del dialogo
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Listener per il pulsante "Save"
        btnSave.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isNotBlank()) {
                saveCommunication(text)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter communication text", Toast.LENGTH_SHORT).show()
            }
        }

        // Listener per il pulsante "Cancel"
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun saveCommunication(text: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        lifecycleScope.launch {
            try {
                val communicationId = mDbRef.child("communications").push().key
                if (communicationId != null) {
                    val communication = Communication(
                        communicationId = communicationId,
                        text = text,
                        date = currentDate,
                        leagueId = leagueId!!
                    )
                    mDbRef.child("communications").child(communicationId).setValue(communication)
                        .await()
                    Toast.makeText(requireContext(), "Communication added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error generating communication ID", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving communication: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ActLeagueFragment", "Error saving communication", e)
            }
        }
    }
    private fun updateCreateCalendarButtonVisibility() {
        // Assicurati che layout_calendar sia inizializzato prima di impostare la visibilità
        if (this::layout_calendar.isInitialized) {
            // Mostra layout_calendar solo se l'utente è un League Manager e la voce del menu selezionata è "match"
            val isLeagueOwner = UserInfo.userId == leagueOwnerId
            layout_calendar.visibility = if (isLeagueOwner && UserInfo.userType == getString(R.string.LeagueManager) &&
                currentMenuItemId == R.id.match
            ) View.VISIBLE else View.GONE
        }
    }
    private fun updateCommunicationAndChatButtonVisibility() {
        if (this::layout_communication.isInitialized) {
            // Mostra addCommunicationLayout solo se l'utente è un League Manager e la voce del menu selezionata è "communications"
            val isLeagueOwner = UserInfo.userId == leagueOwnerId
            layout_communication.visibility =
                if (isLeagueOwner && UserInfo.userType == getString(R.string.LeagueManager) &&
                    currentMenuItemId == R.id.comunications
                ) View.VISIBLE else View.GONE
        }
        if(this::layout_chat.isInitialized){
            layout_chat.visibility =
                if(UserInfo.userType != "" && currentMenuItemId == R.id.comunications) View.VISIBLE else View.GONE
        }
    }

    private fun createCalendar(league: League, teams: List<Team>): List<Match> {
        val matches = mutableListOf<Match>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val (startDate, endDate) = splitPlayingPeriod(league.playingPeriod!!)
            ?: throw IllegalArgumentException("Invalid playing period format")

        val calendar = Calendar.getInstance()
        calendar.time = startDate

        val allPossibleMatches = mutableListOf<Pair<Team, Team>>()
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                allPossibleMatches.add(Pair(teams[i], teams[j]))
            }
        }
        allPossibleMatches.shuffle()

        val matchDay = mutableMapOf<String, MutableList<String>>()
        val tempCalendar = Calendar.getInstance()
        tempCalendar.time = startDate
        while (!tempCalendar.time.after(endDate)) {
            val dayKey = dateFormat.format(tempCalendar.time)
            matchDay[dayKey] = mutableListOf()
            tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val matchTimes = listOf("20:30", "21:30")

        var matchId = 1
        val random = Random()

        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                val team1 = teams[i]
                val team2 = teams[j]

                var validDateFound = false
                while (!validDateFound) {
                    val randomDayOffset = random.nextInt((endDate.time - startDate.time).toInt() / (1000 * 60 * 60 * 24) + 1)
                    calendar.time = startDate
                    calendar.add(Calendar.DAY_OF_MONTH, randomDayOffset)

                    val currentDay = dateFormat.format(calendar.time)
                    val availableTimes = matchTimes - matchDay[currentDay]?.toSet().orEmpty()

                    if (availableTimes.isNotEmpty()) {
                        val currentTime = availableTimes.random()

                        if (matchDay[currentDay]?.size ?: 0 < 2 &&
                            (matchDay[currentDay]?.contains(currentTime) ?: false).not()
                        ) {
                            val match = Match(
                                id = matchId.toString(),
                                team1 = team1,
                                team2 = team2,
                                date = currentDay,
                                time = currentTime,
                                result1 = 0,
                                result2 = 0
                            )
                            matches.add(match)
                            matchId++

                            matchDay[currentDay]?.add(currentTime)

                            validDateFound = true
                        }
                    } else {
                        // Se tutti gli orari sono occupati, passare al giorno successivo
                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
        }

        // Ordinamento della lista di partite per data e ora
        matches.sortWith(Comparator { m1, m2 ->
            val dateComparison = dateFormat.parse(m1.date).compareTo(dateFormat.parse(m2.date))
            if (dateComparison == 0) {
                timeFormat.parse(m1.time).compareTo(timeFormat.parse(m2.time))
            } else {
                dateComparison
            }
        })

        return matches
    }


    private fun splitPlayingPeriod(playingPeriod: String): Pair<Date, Date>? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
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
                leagueOwnerId = league?.leagueManager  // Imposta l'ID del proprietario della lega

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
                            id = teamMap["uid"] as String?,
                            name = teamMap["name"] as String?,
                            team_manager = teamMap["team_manager"] as String?,
                            players = ArrayList(playersMap)
                        )
                        teamsList.add(team)
                    }
                }

                if (league != null) {
                    // aggiungere con or or (teamsList.size<league.maxNumberTeam!!)
                    if (teamsList.isEmpty() || teamsList.size < league.maxNumberTeam!!  ) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Impossible to create a calendar: there are not enough team!", Toast.LENGTH_SHORT).show()
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
