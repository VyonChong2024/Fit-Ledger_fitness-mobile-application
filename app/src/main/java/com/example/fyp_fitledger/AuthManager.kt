package com.example.fyp_fitledger

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModelProvider
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun signInWithGoogle(context: Context, callback: (Boolean, String?) -> Unit) {
        val credentialManager = CredentialManager.create(context)  //manage sign-in credentials (Google, passwords, etc.)

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  //'false' -> Allows users to sign in with any Google account.
            .setServerClientId(context.getString(R.string.web_client_id))
            .setAutoSelectEnabled(false)
            //.setNonce()
            .build()

        val request = GetCredentialRequest.Builder()  //specified which credentials we want to retrieve.
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("SignIn", "...............Starting Google Sign-In...............")
                //Fetches the user’s credential by the request created earlier
                val result = CredentialManager.create(context).getCredential(
                    request = request,
                    context = context
                )
                Log.d("SignIn", "...............Google Sign-In successful...............")
                handleSignIn(result, context, callback)
            } catch (e: GetCredentialCancellationException) {
                Log.e("GoogleSignIn", "User canceled sign-in")
                Toast.makeText(context, "Sign-in was canceled. Please try again.", Toast.LENGTH_SHORT).show() //popup
                callback(false, null)
            } catch (e: Exception) {
                Log.e("SignIn", "Error during sign-in", e)
                callback(false, null)
            }
        }
    }

    fun handleSignIn(result: GetCredentialResponse, context: Context, callback: (Boolean, String?) -> Unit) {
        // Handle the successfully returned credential.
        val credential = result.credential

        // GoogleIdToken credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                if (idToken != null) {
                    // Send ID token to Firebase for authentication
                    firebaseAuthWithGoogle(idToken, context, callback)
                } else {
                    Log.e("Credential Error", "Google ID token is null")
                    callback(false, null)
                }
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("Credential Error", "Received an invalid google id token response", e)
                callback(false, null)
            }
        } else {
            Log.e("Credential Error", "Unexpected type of credential")
            callback(false, null)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, context: Context, callback: (Boolean, String?) -> Unit) {
        //Converts the Google ID Token into a Firebase Authentication credential.
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)  //Sends the credential to Firebase Authentication.
            .addOnCompleteListener { task ->  //Runs code when sign-in completes.
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    Log.d("SignIn", "Firebase sign-in success: ${user?.displayName}")
                    Log.d("SignIn, User ID", "User ID: $uid")
                    Toast.makeText(context, "Sign-in Successful.", Toast.LENGTH_SHORT).show()
                    //Update UI or proceed as needed
                    //checkIfUserExists(user)
                    callback(true, uid)
                } else {
                    Log.w("SignIn", "signInWithCredential:failure", task.exception)
                    callback(false, null)
                }
            }
    }

    private fun checkIfUserExists(user: FirebaseUser?) {
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("users")

        user?.let {
            usersCollection.document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d("Firestore", "User exists: ${user.displayName}")
                        // Proceed to main activity or home screen
                    } else {
                        Log.d("Firestore", "User does not exist, collecting extra info")
                        // Show UI to collect extra demographic info
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error checking user existence", exception)
                }
        }
    }
}

