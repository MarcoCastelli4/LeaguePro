package com.example.leaguepro

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MyLeagueFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MyLeagueFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var mDbRef: DatabaseReference
    private lateinit var edtleague_name: EditText
    private lateinit var edtleague_address:EditText
    private lateinit var edtleague_level:EditText
    private lateinit var edtleague_description:EditText
    private lateinit var edtleague_entryfee:EditText
    private lateinit var edtleague_prize:EditText
    private lateinit var edtleague_restrictions:EditText
    private lateinit var edtleague_playingPeriod:TextView
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
        return inflater.inflate(R.layout.fragment_my_league, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MyLeagueFragment.
         */
        // TODO: Rename and change types and number of parameters
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
        // Handle button to add a league
        val addLeagueContainer: ConstraintLayout = view.findViewById(R.id.add_league_container)
        if (UserType.isLeagueManager) {
            addLeagueContainer.visibility = View.VISIBLE
        } else {
            addLeagueContainer.visibility = View.GONE
        }

        // Find the add_league_icon and set a click listener
        val addLeagueIcon: ImageView = view.findViewById(R.id.add_league_icon)
        addLeagueIcon.setOnClickListener {

            // step 1
            val inflater = LayoutInflater.from(context)
            val popupView = inflater.inflate(R.layout.add_league, null)

            // step 2
            val wid = LinearLayout.LayoutParams.WRAP_CONTENT
            val high = LinearLayout.LayoutParams.WRAP_CONTENT
            val focus= true
            val popupWindow = PopupWindow(popupView, wid, high, focus)

            // step 3
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

            edtleague_name = popupView.findViewById(R.id.edt_league_name)
            edtleague_address = popupView.findViewById(R.id.edt_address)
            edtleague_level = popupView.findViewById(R.id.edt_league_level)
            edtleague_description = popupView.findViewById(R.id.edt_league_description)
            edtleague_entryfee = popupView.findViewById(R.id.edt_entryfee)
            edtleague_prize = popupView.findViewById(R.id.edt_league_prize)
            edtleague_restrictions = popupView.findViewById(R.id.edt_league_restrictions)
            edtleague_playingPeriod = popupView.findViewById(R.id.edt_playing_period)
            mAuth = FirebaseAuth.getInstance()

            // Initialize date pickers
            val btnPlayingPeriod: ImageView = popupView.findViewById(R.id.btn_playing_period)
            val edtPlayingPeriod: TextView = popupView.findViewById(R.id.edt_playing_period)
            btnPlayingPeriod.setOnClickListener{datePickerDialog(edtPlayingPeriod)}

            // Handle close button click
            val btnClose: ImageView = popupView.findViewById(R.id.btn_close)
            btnClose.setOnClickListener {
                popupWindow.dismiss()
            }

            // Handle save button click
            val btnSave: Button = popupView.findViewById(R.id.btn_save)
            btnSave.setOnClickListener {

                val leaguename = edtleague_name.text.toString()
                val leagueaddress = edtleague_address.text.toString()
                val leaguelevel = edtleague_level.text.toString().toInt()
                val leaguedescription = edtleague_description.text.toString()
                val leagueentryfee = edtleague_entryfee.text.toString()
                val leagueprize = edtleague_prize.text.toString()
                val leaguerestrictions = edtleague_restrictions.text.toString()
                val leagueplayingPeriod = edtleague_playingPeriod.text.toString()

                addLeagueToDatabase(leaguename,leagueaddress,leaguelevel,leaguedescription,leagueentryfee,leagueprize,leaguerestrictions,leagueplayingPeriod,mAuth.currentUser?.uid!!)
                popupWindow.dismiss()
            }
        }

    }

    private fun datePickerDialog(edtPlayingPeriod: TextView) {
        // Creating a MaterialDatePicker builder for selecting a date range
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")

        // Building the date picker dialog
        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            // Retrieving the selected start and end dates
            val startDate = selection.first
            val endDate = selection.second

            // Formatting the selected dates as strings
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDateString = sdf.format(Date(startDate ?: 0))
            val endDateString = sdf.format(Date(endDate ?: 0))

            // Creating the date range string
            val selectedDateRange = "$startDateString - $endDateString"

            // Displaying the selected date range in the TextView called edt_playing_period
            edtPlayingPeriod.text = selectedDateRange

        }

        // Showing the date picker dialog

        datePicker.show(requireFragmentManager(), "DATE_PICKER")
    }

    private fun addLeagueToDatabase(name: String?, place: String?, level: Number?,description: String?,entry: String?,prize: String?,restrictions: String?,playingPeriod: String?,leagueManager: String?) {
        // recupero il riferimento del db
        mDbRef = FirebaseDatabase.getInstance().getReference()
        // tramite il riferiemnto aggiungo un elemento
        mDbRef.child("leagues").push().setValue(League(name,place,level,description,entry,prize,restrictions,playingPeriod,leagueManager))
    }
}