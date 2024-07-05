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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
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
         * @return A new instance of fragment MyTeamFragment.
         */
        // TODO: Rename and change types and number of parameters
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
        val addPlayerContainer: ConstraintLayout = view.findViewById(R.id.add_player_container)
        addPlayerContainer.visibility = if (!UserType.isLeagueManager) View.VISIBLE else View.GONE

        setupFirebase()
        //setupLeagueRecyclerView(view)

        // load team  create by team manager
        if (!UserType.isLeagueManager){
           // fetchTeamFromDatabase()
        }

        val addPlayerIcon: ImageView = view.findViewById(R.id.add_player_icon)
        addPlayerIcon.setOnClickListener {
            showAddPlayerPopup(view) }

        val nameTeam: TextView = view.findViewById(R.id.team_name)
        nameTeam.setOnClickListener {
            // Create an EditText to enter the new name
            val editText = EditText(requireContext())
            editText.setText(nameTeam.text)

            // Create a dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Change Team Name")
                .setView(editText)
                .setPositiveButton("Save") { dialog, which ->
                    // Update the TextView with the new name
                    val newName = editText.text.toString()
                    nameTeam.text = newName
                    addTeamToDatabse(nameTeam.text)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

    }

    private fun setupFirebase() {
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()
    }

    private fun showAddPlayerPopup(view: View) {

        // Nascondi la RecyclerView
        (context as Activity).findViewById<RecyclerView>(R.id.teamRecyclerView).visibility = View.GONE

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
            (context as Activity).findViewById<RecyclerView>(R.id.teamRecyclerView).visibility = View.VISIBLE
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
            mAuth.currentUser?.uid!!
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

    private fun addPlayerToTeam(
        playername: String?,
        playerrole: String?,
        playerbirthday: String?,
        team: String?
    ) {
        // Generate a unique key using push()
        val playerId = mDbRef.child(team!!).push().key

        if (playerId != null) {
            val player = Player(playername, playerrole, playerbirthday,playerId)
            // Set the value for the new node
            mDbRef.child(team).child(playerId).setValue(player)
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
}