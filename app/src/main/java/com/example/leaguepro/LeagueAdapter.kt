package com.example.leaguepro

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// LeagueAdapter è collegato al layout league_item
class LeagueAdapter(
    val context: Context,
    val leagueList: ArrayList<League>,
    val dbRef: DatabaseReference,
    val mAuth: FirebaseAuth,
    val fromAllLeague: Boolean
) : RecyclerView.Adapter<LeagueAdapter.LeagueViewHolder>() {

    class LeagueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.league_name)
        val textPrize: TextView = itemView.findViewById(R.id.league_prize)
        val floatRating: RatingBar = itemView.findViewById(R.id.league_level)
        val binButton: ImageView = itemView.findViewById(R.id.bin)
        val moreButton: ImageView = itemView.findViewById(R.id.more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.card_league_layout, parent, false)
        return LeagueViewHolder(view)
    }

    override fun getItemCount(): Int {
        return leagueList.size
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        val currentLeague = leagueList[position]
        holder.textName.text = currentLeague.name
        holder.textPrize.text = currentLeague.prize
        holder.floatRating.rating = currentLeague.level!!

        // Mostro la possibilità di cancellare se sono in MyLeague
        if (fromAllLeague) {
            holder.binButton.visibility=View.GONE
        }
        else {
                holder.binButton.setOnClickListener {
                    showConfirmationDialog(currentLeague)
                }
        }


        holder.moreButton.setOnClickListener {
            showLeagueInfoPopup(currentLeague)
        }
    }

    private fun showConfirmationDialog(league: League) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        // Leaugue manager cancella il torneo
        if (UserInfo.isLeagueManager) {
            builder.setTitle("Delete confirm")
            builder.setMessage("Are you sure to delete ${league.name} and all info connected?")

            builder.setPositiveButton("Delete") { dialog, _ ->
                removeLeagueFromDatabase(league)
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }
        // Team managaer si disiiscrive al torneo
        if (!UserInfo.isLeagueManager){
            builder.setTitle("Unsubscribe confirm")
            builder.setMessage("Are you sure to unsubscribe from ${league.name}?")

            builder.setPositiveButton("Unsubscribe") { dialog, _ ->
                UserInfo.team_id?.let { removeTeamFromLeague(league, it) }
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder.create().show()
    }
    private fun removeTeamFromLeague(league: League, teamUid: String) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = Calendar.getInstance().time

        try {
            val startDateString = league.playingPeriod?.split(" - ")?.get(0)
            val startDate = sdf.parse(startDateString)

            // Posso disiscrivermi fino al giorno prima
            if (startDate != null && startDate.after(today)) {
                val leaguesTeamRef = dbRef.child("leagues_team")
                leaguesTeamRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (dataSnapshot in snapshot.children) {
                            val leagueTeamData = dataSnapshot.value as Map<String, String>
                            val leagueId = leagueTeamData["league_id"]
                            val teamId = leagueTeamData["team_id"]

                            if (leagueId == league.uid && teamId == teamUid) {
                                dataSnapshot.ref.removeValue().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        leagueList.remove(league)
                                        // Notifica l'adapter della rimozione
                                        notifyDataSetChanged()
                                        Toast.makeText(context, "Team removed from league successfully!", Toast.LENGTH_SHORT).show()

                                    } else {
                                        Toast.makeText(context, "Failed to remove team from league", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                break
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to remove team from league: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(context, "Cannot unsubscribe because the league has already started!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error parsing date: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun removeLeagueFromDatabase(league: League) {
        // Usa leagueId per trovare e rimuovere la league dal database
        dbRef.child("leagues").child(league.uid!!).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Rimuovi l'elemento dalla lista locale
                leagueList.remove(league)
                // Notifica l'adapter della rimozione
                 notifyDataSetChanged()
                // Notifica l'adapter del cambiamento della dimensione della lista
                // notifyItemRangeChanged(position, leagueList.size)
                Toast.makeText(context, "League deleted!", Toast.LENGTH_SHORT).show()
                // Opzionalmente, naviga al MyLeagueFragment o esegui altre azioni
            } else {
                Toast.makeText(context, "Error in league deletion", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showLeagueInfoPopup(league: League) {
        val popupView = LayoutInflater.from(context).inflate(R.layout.league_more, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Nascondi la RecyclerView
        (context as Activity).findViewById<RecyclerView>(R.id.leagueRecyclerView).visibility = View.GONE
        popupWindow.setOnDismissListener {
            // Mostra di nuovo la RecyclerView quando la popup viene chiusa
            (context as Activity).findViewById<RecyclerView>(R.id.leagueRecyclerView).visibility = View.VISIBLE
        }
        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val play: Button = popupView.findViewById(R.id.join_button)
        // Il league manager non può iscriversi e se ho la league in myleague non posso riscrivermi
        // Verifica se l'utente è un team manager
        val isTeamManager = !UserInfo.isLeagueManager

// Verifica se la lega è già nel database leagues_team
        val leagueId = league.uid
        val leaguesTeamRef = dbRef.child("leagues_team")
        leaguesTeamRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isLeagueInTeamLeagues = false
                for (dataSnapshot in snapshot.children) {
                    val leagueTeamData = dataSnapshot.value as Map<String, String>
                    val fetchedLeagueId = leagueTeamData["league_id"]
                    val fetchedTeamId = leagueTeamData["team_id"]

                    if (fetchedLeagueId == leagueId) {
                        // Se c'è una corrispondenza leagues_team con questa lega
                        isLeagueInTeamLeagues = true
                        break
                    }
                }

                //Toast.makeText(context, "Failed to check leagues_team: ${UserInfo.team_id}", Toast.LENGTH_SHORT).show()

                // Determina la visibilità del pulsante "play" in base alle condizioni
                if (!UserInfo.team_id.equals("") && !isLeagueInTeamLeagues) {
                    // Se l'utente è team manager e non c'è una corrispondenza leagues_team, il pulsante è visibile
                    play.visibility = View.VISIBLE
                } else {
                    // Altrimenti, il pulsante non è visibile
                    play.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to check leagues_team: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })


        // Handle data visualization
        val description: TextView = popupView.findViewById(R.id.more_league_description)
        val address: TextView = popupView.findViewById(R.id.edt_more_address)
        val period: TextView = popupView.findViewById(R.id.edt_more_playing_period)
        val entry: TextView = popupView.findViewById(R.id.edt_more_euro)
        val restrictions: TextView = popupView.findViewById(R.id.edt_more_info
        )

        description.text = league.description
        address.text = league.address
        period.text = league.playingPeriod
        entry.text = league.entryfee
        restrictions.text = league.restrictions

        // Optional: If you want to add a close button in the popup
        val closeButton: ImageView = popupView.findViewById(R.id.btn_close)
        closeButton.setOnClickListener {
            popupWindow.dismiss()
        }

        play.setOnClickListener{
            UserInfo.team_id?.let { it1 -> addTeamToALeague(league, it1) }
            popupWindow.dismiss()
        }
    }
    private fun addTeamToALeague(league: League, teamUid: String) {
        // Verifica che la data di inizio del torneo sia maggiore di oggi
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = Calendar.getInstance().time

        try {
            val startDateString = league.playingPeriod?.split(" - ")?.get(0)
            val startDate = sdf.parse(startDateString)

            if (startDate != null && startDate.after(today)) {
                // Continua con l'aggiunta del team alla league
                val leagueUid: String? = league.uid
                // Otteniamo un riferimento al nodo leagues_team
                val leagueTeamRef = dbRef.child("leagues_team")

                // Creiamo un oggetto per il team con l'ID della league e l'ID del team
                val leagueTeamData = mapOf(
                    "league_id" to leagueUid,
                    "team_id" to teamUid
                )

                // Usiamo push() per generare una chiave univoca per ogni associazione league-team
                val leagueTeamKey = leagueTeamRef.push().key

                if (leagueTeamKey != null) {
                    leagueTeamRef.child(leagueTeamKey).setValue(leagueTeamData)
                        .addOnSuccessListener {
                            // Gestione successo
                            Toast.makeText(context, "Team joined league successfully!", Toast.LENGTH_SHORT).show()
                            // TODO: Spostarsi in MyLeague e aggiornare il menu

                        }
                        .addOnFailureListener { e ->
                            // Gestione fallimento
                            Toast.makeText(context, "Failed to join league: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Failed to generate a unique key for the league-team association", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Cannot join league because the league has already started!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ParseException) {
            Toast.makeText(context, "Error parsing date: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



}
