package com.example.saferhike.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saferhike.api.ApiRoutes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    var currentUser: FirebaseUser? = null

    fun checkAuthStatus(){
        if(auth.currentUser==null){
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email : String, password : String){

        if(email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or Password missing")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                    currentUser = auth.currentUser
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }
    fun signup(email : String, password : String, fName: String, lName: String,
               contactsList: List<String>, apiService: ApiRoutes) {

        if(email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or Password missing")
            return
        }
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{task->
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    val userData = UserReq(currentUser?.uid ?: "0", fName, lName, contactsList)
                    viewModelScope.launch {
                        try {
                            val response = apiService.createUser(userData)
                            if (!response.isSuccessful) {
                                _authState.value =
                                    AuthState.Error("Failed to post hike: ${response.code()} : ${response.message()}")
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

    fun signout(){
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        currentUser = null
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserReq(
    val uid: String,
    val fName: String,
    val lName: String,
    val emergencyContacts: List<String>
)