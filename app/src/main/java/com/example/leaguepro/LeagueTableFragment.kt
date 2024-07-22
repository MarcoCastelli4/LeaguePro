package com.example.leaguepro

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LeagueTableFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var leagueTableLayout: TableLayout
    private var leagueUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inizializzazione di Firebase Database
        database = FirebaseDatabase.getInstance().reference
        leagueUid = arguments?.getString("league_id")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_table_league, container, false)
        leagueTableLayout = view.findViewById(R.id.leagueTableLayout)

        // Verifica se il contesto è disponibile
        if (context == null) {
            return view
        }

        leagueUid?.let { id ->
            // Imposta il listener per i dati della classifica
            val leagueTableRef = database.child("league_table").child(id)
            leagueTableRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Assicurati che il contesto sia disponibile
                    if (isAdded) {
                        leagueTableLayout.removeAllViews() // Rimuove tutte le righe esistenti

                        val leagueTable = snapshot.getValue(LeagueTable::class.java)
                        if (leagueTable != null) {
                            // Aggiungi l'intestazione della tabella
                            addTableHeader()

                            // Aggiungi i dati delle squadre
                            leagueTable.getSortedTeams().forEach { team ->
                                addTeamRow(team)
                            }
                        } else {
                            // Mostra un messaggio se la classifica non è disponibile
                            addNoDataRow()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Assicurati che il contesto sia disponibile
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Failed to retrieve data: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } ?: run {
            // Assicurati che il contesto sia disponibile
            if (isAdded) {
                Toast.makeText(requireContext(), "League ID not available", Toast.LENGTH_LONG).show()
            }
        }

        return view
    }

    private fun addTableHeader() {
        // Assicurati che il contesto sia non null
        val context = requireContext()
        val headerRow = TableRow(context)

        val headers = listOf("Team", "PT", "V", "N", "P", "GF", "GS")
        headers.forEach { headerText ->
            val textView = TextView(context).apply {
                text = headerText
                setPadding(8, 8, 8, 8)
                textSize = 16f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
            }
            headerRow.addView(textView)
        }
        leagueTableLayout.addView(headerRow)
    }

    private fun addTeamRow(team: Team) {
        // Assicurati che il contesto sia non null
        val context = requireContext()
        val row = TableRow(context)

        val values = listOf(
            team.name ?: "N/A",
            team.points.toString(),
            team.wins.toString(),
            team.draws.toString(),
            team.losses.toString(),
            team.goalsFor.toString(),
            team.goalsAgainst.toString()
        )

        values.forEach { value ->
            val textView = TextView(context).apply {
                text = value
                setPadding(8, 8, 8, 8)
                textSize = 14f
                gravity = Gravity.CENTER
            }
            row.addView(textView)
        }

        leagueTableLayout.addView(row)
    }

    private fun addNoDataRow() {
        // Assicurati che il contesto sia non null
        val context = requireContext()
        val noDataRow = TableRow(context)
        val noDataTextView = TextView(context).apply {
            text = "No team registered"
            setPadding(16, 16, 16, 16)
            textSize = 16f
            gravity = Gravity.CENTER
        }
        noDataRow.addView(noDataTextView)
        leagueTableLayout.addView(noDataRow)
    }
}
