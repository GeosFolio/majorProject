package com.example.saferhike.viewModels

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
import com.example.saferhike.api.ApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.security.KeyPairGenerator
import java.security.PublicKey

class AuthViewModel : ViewModel() {
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    var currentUser: FirebaseUser? = null
    var userData: UserReq = UserReq("0", null, "", "", emptyList())

    fun login(email : String, password : String, apiService: ApiService){

        if(email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or Password missing")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    currentUser = auth.currentUser
                    viewModelScope.launch {
                        try {
                            val response = apiService.apiService.getUser(currentUser?.uid?:"")
                            if (response.isSuccessful) {
                                val fetchedUser = response.body()
                                if (fetchedUser != null) {
                                    userData = fetchedUser
                                    _authState.value = AuthState.Authenticated
                                } else {
                                    _authState.value = AuthState.Error("User data not found")
                                }
                            } else {
                                _authState.value =
                                    AuthState.Error("Failed to fetch user:" +
                                            "${response.code()} : ${response.message()}")
                            }
                        } catch (e: Exception) {
                            _authState.value = AuthState.Error("Error fetching user: ${e.message}")
                        }
                    }
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }
    fun signup(email : String, password : String, fName: String, lName: String,
               contactsList: List<EmergencyContact>, apiService: ApiService) {

        if(email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or Password missing")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    val keyGen = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA,
                        "AndroidKeyStore"
                    )
                    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                        "SaferHikeKeyPair",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .build()
                    keyGen.initialize(keyGenParameterSpec)
                    val keyPair = keyGen.genKeyPair()
                    userData = UserReq(currentUser?.uid ?: "0", keyPair.public, fName, lName, contactsList)
                    viewModelScope.launch {
                        try {
                            val response = apiService.apiService.createUser(userData)
                            if (!response.isSuccessful) {
                                _authState.value =
                                    AuthState.Error("Failed to post user: ${response.code()} : ${response.message()}")
                            } else {
                                _authState.value = AuthState.Authenticated
                            }
                        } catch (e: Exception) {
                            _authState.value = AuthState.Error("Failed to post: ${e.message}")
                        }
                    }
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun updateUser(userReq: UserReq, apiService: ApiRoutes) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.updateUser(userReq)
                if (response.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value =
                        AuthState.Error("Unable to update account: ${response.code()} : ${response.message()}")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Something went wrong: ${e.message}")
            }
        }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        currentUser = null
    }
}

sealed class AuthState {
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserReq(
    val uid: String,
    val publicKey: PublicKey?,
    var fName: String,
    var lName: String,
    var emergencyContacts: List<EmergencyContact>
)

data class EmergencyContact(
    val phoneNumber: String,
    val email: String
)