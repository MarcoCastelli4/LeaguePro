package com.example.leaguepro

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyTeamFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyTeamFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtplayer_name: EditText
    private lateinit var edtplayer_role: Spinner
    private lateinit var edtplayer_birthday: TextView
    private lateinit var edtteam_name: TextView
   // private var teamId: String? = null

    private lateinit var playerList: ArrayList<Player>
    private lateinit var adapter: PlayerAdapter
    private lateinit var playerRecyclerView: RecyclerView

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
        return inflater.inflate(R.layout.fragment_my_team, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         *         // TODO: Rename and change types and number of para
         * @return A new instance of fragment MyTeamFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyTeamFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView(view)
    }


    private fun setupView(view: View) {
        setupFirebase()
        val edtteam_name: TextView = view.findViewById(R.id.team_name)
        val addPlayerContainer: ConstraintLayout = view.findViewById(R.id.add_player_container)
        // Aggiorna il nome del team nella TextView
        // Chiamata a updateTeamName con un callback per ottenere teamId aggiornato
        mAuth.currentUser?.uid?.let {
            updateTeamName(it, edtteam_name, object : TeamIdCallback {
                override fun onTeamIdUpdated(teamId: String) {
                    UserInfo.team_id=teamId
                    //Toast.makeText(context, "Team id: ${this@MyTeamFragment.teamId}", Toast.LENGTH_SHORT).show()
                    // Aggiorna la visibilità di addPlayerContainer dopo aver ottenuto teamId
                    addPlayerContainer.visibility = if (!UserInfo.isLeagueManager and !UserInfo.team_id.equals("")) View.VISIBLE else View.GONE
                }
            })
        }

        setupLeagueRecyclerView(view)

        // load team  create by team manager
        if (!UserInfo.isLeagueManager){
            fetchTeamFromDatabase()
        }

        val addPlayerIcon: ImageView = view.findViewById(R.id.add_player_icon)
        addPlayerIcon.setOnClickListener {
            showAddPlayerPopup(view) }

        edtteam_name.setOnClickListener {
            // Crea un EditText per inserire il nuovo nome
            val editText = EditText(requireContext()).apply {
                setText(edtteam_name.text)
            }

            // Crea un AlertDialog
            val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogPositive)
                .setTitle("Change Team Name")
                .setView(editText)
                .setPositiveButton("Save", null)  // Pass null per gestire il click manualmente
                .setNegativeButton("Cancel", null)
                .create()

            // Mostra il dialogo
            dialog.show()

            // Gestione manuale del click sul pulsante "Save"
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // Controlla che l'input non sia vuoto
                val newName = editText.text.toString().trim()
                if (newName.isEmpty()) {
                    // Mostra un messaggio di errore nel campo di input
                    editText.error = "Team name cannot be empty"
                } else {
                    // Aggiorna il TextView con il nuovo nome e salva il team
                    edtteam_name.text = newName
                    addOrUpdateTeamToDatabase(newName, mAuth.currentUser?.uid, object : TeamIdCallback {
                        override fun onTeamIdUpdated(teamId: String) {
                            UserInfo.team_id = teamId
                            //Toast.makeText(context, "Team id: ${this@MyTeamFragment.teamId}", Toast.LENGTH_SHORT).show()
                            // Aggiorna la visibilità di addPlayerContainer dopo aver ottenuto teamId
                            addPlayerContainer.visibility = if (!UserInfo.isLeagueManager and !UserInfo.team_id.equals("")) View.VISIBLE else View.GONE
                        }
                    })
                    dialog.dismiss()  // Chiudi il dialogo
                }
            }
        }


    }

    private fun setupLeagueRecyclerView(view: View) {
        playerList = ArrayList()
        playerRecyclerView = view.findViewById(R.id.playersRecyclerView)
        playerRecyclerView.layoutManager = LinearLayoutManager(context)
        playerRecyclerView.setHasFixedSize(true)
        adapter = PlayerAdapter(requireContext(), playerList,mDbRef,mAuth)
        playerRecyclerView.adapter = adapter
    }
    private fun fetchTeamFromDatabase() {
        mDbRef.child("teams").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playerList.clear() // Assicurati che playerList sia una MutableMap

                for (postSnapshot in snapshot.children) {
                    // Estrai il team_manager id
                    val teamManagerId = postSnapshot.child("team_manager").getValue(String::class.java)

                    // Verifica se il team_manager id corrisponde all'utente corrente
                    if (teamManagerId == mAuth.currentUser?.uid) {
                        // Estrai la lista dei giocatori per questo team
                        val playersSnapshot = postSnapshot.child("players")
                        for (playerSnapshot in playersSnapshot.children) {
                            val player = playerSnapshot.getValue(Player::class.java)
                            player?.let {
                                playerList.add(player)
                            }
                        }
                    } else {
                        Log.d("FirebaseData", "Invalid team data for manager ID: $teamManagerId")
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }




    private fun updateTeamName(userId: String, teamNameTextView: TextView,callback: TeamIdCallback) {
        mDbRef.child("teams").orderByChild("team_manager").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Trova il team associato all'ID dell'utente corrente
                        for (teamSnapshot in snapshot.children) {
                            val id = teamSnapshot.child("uid").getValue().toString()
                            if (id != "") {
                                // Aggiorna la TextView con il nome del team
                                teamNameTextView.text = teamSnapshot.child("name").getValue().toString()
                                callback.onTeamIdUpdated(id)
                            }
                        }
                    }
                    else{
                        callback.onTeamIdUpdated("")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
    }

    private fun showAddPlayerPopup(view: View) {

        // Nascondi la RecyclerView
        (context as Activity).findViewById<RecyclerView>(R.id.playersRecyclerView).visibility = View.GONE

        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.add_player, null)
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setOnDismissListener {
            // Mostra di nuovo la RecyclerView quando la popup viene chiusa
            (context as Activity).findViewById<RecyclerView>(R.id.playersRecyclerView).visibility = View.VISIBLE
        }

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        initializePopupFields(popupView)
        setupPopupListeners(popupView, popupWindow)

    }
    private fun initializePopupFields(popupView: View) {
        edtplayer_name = popupView.findViewById(R.id.edt_player_name)
        edtplayer_role = popupView.findViewById(R.id.edt_player_role)
        edtplayer_birthday = popupView.findViewById(R.id.edt_player_birthday)

        mAuth = FirebaseAuth.getInstance()

        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.role_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            edtplayer_role.adapter = adapter
        }

        val btnBirthday: ImageView = popupView.findViewById(R.id.btn_birthday)
        btnBirthday.setOnClickListener { datePickerDialog(edtplayer_birthday) }
    }

    private fun setupPopupListeners(popupView: View, popupWindow: PopupWindow) {
        val btnClose: ImageView = popupView.findViewById(R.id.btn_close)
        btnClose.setOnClickListener { popupWindow.dismiss() }

        val btnSave: Button = popupView.findViewById(R.id.btn_save)
        btnSave.setOnClickListener { savePlayer(popupWindow) }
    }

    private fun savePlayer(popupWindow: PopupWindow) {
        val playername = edtplayer_name.text.toString()
        val playerole = edtplayer_role.selectedItem.toString()
        val playerbirthday = edtplayer_birthday.text.toString()

        if (!validateFields(
                playername,
                playerbirthday
            )
        ) {
            return
        }

            addPlayerToTeam(
                playername,
                playerole,
                playerbirthday,
                UserInfo.team_id
            )

        popupWindow.dismiss()
    }

    private fun validateFields(playername: String?, playerbirthday: String?): Boolean {
        var valid = true

        // Check each field and set an error message if it's empty
        if (playername!!.isEmpty()) {
            edtplayer_name.error = "Please enter player name"
            valid = false
        }
        if (!isValidDateRange(playerbirthday!!)) {
            edtplayer_birthday.error = "Please select a player birthday"
            valid = false
        }
        return valid
    }
    private fun isValidDateRange(dateRange: String): Boolean {
        // Define the regex pattern for the date range
        val dateRangePattern = Regex("""\b\d{2}/\d{2}/\d{4}\b""")

        // Check if the input string matches the pattern
        return dateRangePattern.matches(dateRange)
    }
    private fun datePickerDialog(edtPlayingPeriod: TextView) {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // Consenti solo date nel passato

        val builder = MaterialDatePicker.Builder.datePicker() // Usa il DatePicker singolo per la data di nascita
            .setTitleText("Select birthday")
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.CustomDatePicker) // Usa il tema personalizzato

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDateString = sdf.format(Date(selection ?: 0))
            edtPlayingPeriod.text = selectedDateString
        }
        datePicker.show(requireFragmentManager(), "DATE_PICKER")
    }

    private fun addPlayerToTeam(playername: String?, playerrole: String?, playerbirthday: String?, team: String?) {
        // Generate a unique key using push()
        val playerId = mDbRef.child("teams").child(team!!).push().key

        if (playerId != null) {
            val player = Player(playername, playerrole, playerbirthday,playerId)
            // Set the value for the new node
            mDbRef.child("teams").child(team).child("players").child(playerId).setValue(player)
                .addOnSuccessListener {
                    // Handle success
                    Toast.makeText(context, "Player added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Toast.makeText(context, "Failed to add player: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle error: Failed to generate unique key
            Toast.makeText(context, "Failed to generate unique key", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addOrUpdateTeamToDatabase(name: String?, teamManager: String?,callback: TeamIdCallback) {
        mDbRef.child("teams").orderByChild("team_manager").equalTo(teamManager)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Update existing team
                        for (teamSnapshot in snapshot.children) {
                            val teamId = teamSnapshot.key
                            if (teamId != null) {
                                val updatedTeam = Team(teamId, name, teamManager,playerList)
                                mDbRef.child("teams").child(teamId).setValue(updatedTeam)
                                    .addOnSuccessListener {
                                        if (name != null) {
                                            callback.onTeamIdUpdated(teamId)
                                        }
                                        Toast.makeText(context, "Team updated successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to update team: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    } else {
                        // Create new team
                        val teamId = mDbRef.child("teams").push().key
                        if (teamId != null) {
                            val newTeam = Team(teamId, name, teamManager,playerList)
                            mDbRef.child("teams").child(teamId).setValue(newTeam)
                                .addOnSuccessListener {
                                    if (name != null) {
                                        callback.onTeamIdUpdated(teamId)
                                    }
                                    Toast.makeText(context, "Team added successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to add team: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Failed to generate unique key", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    interface TeamIdCallback {
        fun onTeamIdUpdated(teamId: String)
    }

}