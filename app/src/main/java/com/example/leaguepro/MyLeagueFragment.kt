package com.example.leaguepro

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MyLeagueFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var leagueList: ArrayList<League>
    private lateinit var adapter: LeagueAdapter
    private lateinit var leagueRecyclerView: RecyclerView

    private lateinit var edtleague_name: EditText
    private lateinit var edtleague_address: EditText
    private lateinit var edtleague_level: RatingBar
    private lateinit var edtleague_description: EditText
    private lateinit var edtleague_entryfee: EditText
    private lateinit var edtleague_prize: EditText
    private lateinit var edtleague_restrictions: EditText
    private lateinit var edtleague_playingPeriod: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_league, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MyLeagueFragment().apply {
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
        val addLeagueContainer: ConstraintLayout = view.findViewById(R.id.add_league_container)
        addLeagueContainer.visibility = if (UserType.isLeagueManager) View.VISIBLE else View.GONE

        setupFirebase()
        setupLeagueRecyclerView(view)

        // load league create by league manager
        if (UserType.isLeagueManager){
            fetchLeaguesFromDatabase()
        }

        // TODO load league that team has subscribe
        if (UserType.isLeagueManager==false){
            fetchTeamLeaguesFromDatabase()
        }
        val addLeagueIcon: ImageView = view.findViewById(R.id.add_league_icon)
        addLeagueIcon.setOnClickListener {
            showAddLeaguePopup(view) }

    }

    private fun setupLeagueRecyclerView(view: View) {
        leagueList = ArrayList()
        leagueRecyclerView = view.findViewById(R.id.leagueRecyclerView)
        leagueRecyclerView.layoutManager = LinearLayoutManager(context)
        leagueRecyclerView.setHasFixedSize(true)
        adapter = LeagueAdapter(requireContext(), leagueList,mDbRef,mAuth,false)
        leagueRecyclerView.adapter = adapter
    }

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
    }

    private fun fetchLeaguesFromDatabase() {
        mDbRef.child("leagues").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                leagueList.clear()
                for (postSnapshot in snapshot.children) {
                    val league = postSnapshot.getValue(League::class.java)
                    league?.let {
                        if (it.leagueManager == mAuth.currentUser?.uid) {
                            leagueList.add(it)
                        } else {
                            Log.d("FirebaseData", "Invalid league data: $it")
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun fetchTeamLeaguesFromDatabase() {
        val currentUserId = mAuth.currentUser?.uid

        // First, fetch all leagues associated with the current user's team
        mDbRef.child("leagues_team").orderByChild("team_id").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(teamSnapshot: DataSnapshot) {
                    // Clear the current leagueList
                    leagueList.clear()

                    // Iterate through each league-team association
                    teamSnapshot.children.forEach { teamLeagueSnapshot ->
                        val leagueUid = teamLeagueSnapshot.child("league_id").getValue(String::class.java)

                        // Fetch details of the league from leagues table using leagueUid
                        if (!leagueUid.isNullOrEmpty()) {
                            mDbRef.child("leagues").child(leagueUid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(leagueSnapshot: DataSnapshot) {
                                        val league = leagueSnapshot.getValue(League::class.java)
                                        league?.let {
                                            leagueList.add(it)
                                        }
                                        adapter.notifyDataSetChanged()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(
                                            context,
                                            "Failed to load league data: ${error.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        context,
                        "Failed to load team leagues data: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun showAddLeaguePopup(view: View) {

        // Nascondi la RecyclerView
        (context as Activity).findViewById<RecyclerView>(R.id.leagueRecyclerView).visibility = View.GONE



        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.add_league, null)
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setOnDismissListener {
            // Mostra di nuovo la RecyclerView quando la popup viene chiusa
            (context as Activity).findViewById<RecyclerView>(R.id.leagueRecyclerView).visibility = View.VISIBLE
        }

        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        initializePopupFields(popupView)
        setupPopupListeners(popupView, popupWindow)

    }

    private fun initializePopupFields(popupView: View) {
        edtleague_name = popupView.findViewById(R.id.edt_league_name)
        edtleague_address = popupView.findViewById(R.id.edt_address)
        edtleague_level = popupView.findViewById(R.id.edt_league_level)
        edtleague_description = popupView.findViewById(R.id.edt_league_description)
        edtleague_entryfee = popupView.findViewById(R.id.edt_entryfee)
        edtleague_prize = popupView.findViewById(R.id.edt_league_prize)
        edtleague_restrictions = popupView.findViewById(R.id.edt_league_restrictions)
        edtleague_playingPeriod = popupView.findViewById(R.id.edt_playing_period)
        mAuth = FirebaseAuth.getInstance()

        val btnPlayingPeriod: ImageView = popupView.findViewById(R.id.btn_playing_period)
        btnPlayingPeriod.setOnClickListener { datePickerDialog(edtleague_playingPeriod) }
    }

    private fun setupPopupListeners(popupView: View, popupWindow: PopupWindow) {
        val btnClose: ImageView = popupView.findViewById(R.id.btn_close)
        btnClose.setOnClickListener { popupWindow.dismiss() }

        val btnSave: Button = popupView.findViewById(R.id.btn_save)
        btnSave.setOnClickListener { saveLeague(popupWindow) }
    }

    private fun datePickerDialog(edtPlayingPeriod: TextView) {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())

        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select a date range")
            .setCalendarConstraints(constraintsBuilder.build())
            .setTheme(R.style.CustomDatePicker) // Usa il tema personalizzato

        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val startDateString = sdf.format(Date(selection.first ?: 0))
            val endDateString = sdf.format(Date(selection.second ?: 0))
            edtPlayingPeriod.text = "$startDateString - $endDateString"
        }
        datePicker.show(requireFragmentManager(), "DATE_PICKER")
    }


    private fun saveLeague(popupWindow: PopupWindow) {
        val leaguename = edtleague_name.text.toString()
        val leagueaddress = edtleague_address.text.toString()
        val leaguelevel = edtleague_level.rating
        val leaguedescription = edtleague_description.text.toString()
        val leagueentryfee = edtleague_entryfee.text.toString()
        val leagueprize = edtleague_prize.text.toString()
        val leaguerestrictions = edtleague_restrictions.text.toString()
        val leagueplayingPeriod = edtleague_playingPeriod.text.toString()

        if (!validateFields(
                leaguename,
                leagueaddress,
                leaguedescription,
                leagueentryfee,
                leagueprize,
                leaguerestrictions,
                leagueplayingPeriod
            )
        ) {
            return
        }

        addLeagueToDatabase(
            leaguename,
            leagueaddress,
            leaguelevel,
            leaguedescription,
            leagueentryfee,
            leagueprize,
            leaguerestrictions,
            leagueplayingPeriod,
            mAuth.currentUser?.uid!!
        )
        popupWindow.dismiss()
    }

    // Function to check if the date range is in the correct format
    private fun isValidDateRange(dateRange: String): Boolean {
        // Define the regex pattern for the date range
        val dateRangePattern = Regex("""\b\d{2}/\d{2}/\d{4} - \d{2}/\d{2}/\d{4}\b""")

        // Check if the input string matches the pattern
        return dateRangePattern.matches(dateRange)
    }

    // Function to validate the fields
    private fun validateFields(leaguename: String?,
                               leagueaddress: String?,
                               leaguedescription: String?,
                               leagueentryfee: String?,
                               leagueprize: String?,
                               leaguerestrictions: String?,
                               leagueplayingPeriod: String?): Boolean {
        var valid = true

        // Check each field and set an error message if it's empty
        if (leaguename!!.isEmpty()) {
            edtleague_name.error = "Please enter League name"
            valid = false
        }
        if (leagueaddress!!.isEmpty()) {
            edtleague_address.error = "Please enter League address"
            valid = false
        }
        if (leaguedescription!!.isEmpty()) {
            edtleague_description.error = "Please enter League description"
            valid = false
        }
        if (leagueentryfee!!.isEmpty()) {
            edtleague_entryfee.error = "Please enter League entry fee"
            valid = false
        }
        if (leagueprize!!.isEmpty()) {
            edtleague_prize.error = "Please enter League prize"
            valid = false
        }
        if (leaguerestrictions!!.isEmpty()) {
            edtleague_restrictions.error = "Please enter League restrictions"
            valid = false
        }

        // Specific validation for the playing period field
        if (!isValidDateRange(leagueplayingPeriod!!)) {
            edtleague_playingPeriod.error = "Please enter a valid date range (dd/MM/yyyy - dd/MM/yyyy)"
            valid = false
        }

        return valid
    }



    private fun addLeagueToDatabase(
        name: String?,
        place: String?,
        level: Float?,
        description: String?,
        entry: String?,
        prize: String?,
        restrictions: String?,
        playingPeriod: String?,
        leagueManager: String?
    ) {
        // Generate a unique key using push()
        val leagueId = mDbRef.child("leagues").push().key

        if (leagueId != null) {
            val league = League(leagueId, name, place, level, description, entry, prize, restrictions, playingPeriod, leagueManager)
            // Set the value for the new node
            mDbRef.child("leagues").child(leagueId).setValue(league)
                .addOnSuccessListener {
                    // Handle success
                    Toast.makeText(context, "League added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Toast.makeText(context, "Failed to add league: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle error: Failed to generate unique key
            Toast.makeText(context, "Failed to generate unique key", Toast.LENGTH_SHORT).show()
        }
    }
}