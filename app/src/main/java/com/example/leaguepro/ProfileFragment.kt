package com.example.leaguepro

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var logout: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var user_type:TextView
    private lateinit var fullname:TextView
    private lateinit var email:TextView
    private lateinit var change_psw:Button
    private lateinit var editProfile:ImageView
    private lateinit var mDbRef: DatabaseReference

    private lateinit var editFullname: EditText
    private lateinit var editEmail: EditText
    private lateinit var save: Button
    private lateinit var cancel:Button
    private lateinit var txt_psw: TextView
    private lateinit var confirm_psw: EditText


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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()

        val userTypeImageView: ImageView = view.findViewById(R.id.profile_image)
        if (UserInfo.isLeagueManager) {
            userTypeImageView.setImageResource(R.drawable.league_manager) // Replace with your actual image resource
        }

        // Handle the logout button click
        logout = view.findViewById(R.id.logout_button)
        logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            // Navigate to the login activity or any other appropriate activity
            val intent = Intent(context, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity?.finish()
        }

        user_type = view.findViewById(R.id.text_user_type)
        fullname = view.findViewById(R.id.text_fullname)
        email = view.findViewById(R.id.text_email)
        change_psw = view.findViewById(R.id.reset_password_button)
        editProfile = view.findViewById(R.id.edt_button)
        save = view.findViewById(R.id.save_button)
        cancel=view.findViewById(R.id.cancel_button)
        editFullname = view.findViewById(R.id.edit_fullname)
        editEmail = view.findViewById(R.id.edit_email)

        txt_psw=view.findViewById(R.id.textView3)
        confirm_psw=view.findViewById(R.id.confirm_psw)

        // Fetch user data from Firebase and populate the UI
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            mDbRef.child("users").child(currentUser.uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        user_type.text = user.userType
                        fullname.text = user.fullname
                        email.text = user.email
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors.
                }
            })

            change_psw.setOnClickListener {
                val emailAddress = currentUser.email
                if (emailAddress != null) {
                    mAuth.sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Check your email to reset password reset", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            editProfile.setOnClickListener {
                toggleEditMode(true)
            }

            save.setOnClickListener {
                val newName = editFullname.text.toString()
                val newEmail = editEmail.text.toString()
                val psw=confirm_psw.text.toString()

                if(!validateFields(newName,newEmail,psw)){
                    return@setOnClickListener
                }

                reauthenticateAndUpdateProfile(newName, newEmail, psw)
            }

            cancel.setOnClickListener{
                toggleEditMode(false)
            }
        }
    }

    private fun validateFields(newName: String,newEmail: String, psw: String): Boolean
    {
        var valid=true
        // Check if any of the fields are empty
        if (newName.isEmpty()) {
            editFullname.error = "Fullname is required"
            valid=false
        }

        if (newEmail.isEmpty()) {
            editEmail.error = "Email is required"
            valid=false
        }

        if (psw.isEmpty()) {
            confirm_psw.error = "Confirm password is required"
            valid=false
        }
        return valid
    }
    private fun toggleEditMode(enable: Boolean) {
        if (enable) {
            fullname.visibility = View.GONE
            email.visibility = View.GONE
            editFullname.visibility = View.VISIBLE
            editEmail.visibility = View.VISIBLE
            editFullname.setText(fullname.text)
            editEmail.setText(email.text)

            cancel.visibility=View.VISIBLE
            save.visibility = View.VISIBLE
            editProfile.visibility = View.GONE
            change_psw.visibility = View.GONE

            txt_psw.visibility=View.VISIBLE
            confirm_psw.visibility=View.VISIBLE


        } else {
            fullname.visibility = View.VISIBLE
            email.visibility = View.VISIBLE
            editFullname.visibility = View.GONE
            editEmail.visibility = View.GONE

            save.visibility = View.GONE
            cancel.visibility=View.GONE

            editProfile.visibility = View.VISIBLE
            change_psw.visibility = View.VISIBLE

            txt_psw.visibility=View.GONE
            confirm_psw.visibility=View.GONE
        }
    }

    private fun reauthenticateAndUpdateProfile(newName: String, newEmail: String, password: String) {


        val currentUser = mAuth.currentUser
        if (currentUser != null && currentUser.email != null) {
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        currentUser.updateEmail(newEmail)
                            .addOnCompleteListener { updateEmailTask ->
                                if (updateEmailTask.isSuccessful) {
                                    val userUpdates = mapOf(
                                        "fullname" to newName,
                                        "email" to newEmail
                                    )
                                    mDbRef.child("users").child(currentUser.uid).updateChildren(userUpdates)
                                        .addOnCompleteListener { updateDbTask ->
                                            if (updateDbTask.isSuccessful) {
                                                fullname.text = newName
                                                email.text = newEmail
                                                toggleEditMode(false)
                                            } else {
                                                Toast.makeText(context, "Failed to update profile: ${updateDbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Failed to update email: ${updateEmailTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Reauthentication failed: ${reauthTask.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }




}
