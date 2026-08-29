package com.ochag.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


data class LocalUserAccount(
    val name: String,
    val login: String,
    val password: String,
    val isOwner: Boolean = false,
    val online: Boolean = true
)


data class PendingJoinRequest(
    val name: String,
    val login: String,
    val inviteCode: String
)


object OchagAppState {


    private var dataStoreManager: DataStoreManager? = null


    private val users =
        mutableStateListOf(

            LocalUserAccount(
                name = "Михаил",
                login = "mikhail",
                password = "123456",
                isOwner = true
            ),

            LocalUserAccount(
                name = "Анна",
                login = "anna",
                password = "123456"
            ),

            LocalUserAccount(
                name = "Дочь",
                login = "daughter",
                password = "123456"
            ),

            LocalUserAccount(
                name = "Сын",
                login = "son",
                password = "123456"
            ),

            LocalUserAccount(
                name = "Бабушка",
                login = "grandma",
                password = "123456"
            )
        )


    var currentUser by mutableStateOf<LocalUserAccount?>(null)
        private set



    val approvedMembers
        get() = users



    val pendingJoinRequests =
        mutableStateListOf<PendingJoinRequest>()



    fun init(context: Context){

        dataStoreManager =
            DataStoreManager(context)

    }



    fun login(
        login: String,
        password: String
    ): Boolean {


        val user =
            users.find {

                it.login.equals(
                    login,
                    ignoreCase = true
                )
                        &&
                        it.password == password

            }



        return if(user != null){

            currentUser = user

            saveUser()

            true

        } else {

            false

        }

    }



    private fun saveUser(){

        val user =
            currentUser ?: return


        val manager =
            dataStoreManager ?: return


        CoroutineScope(
            Dispatchers.IO
        ).launch {


            manager.saveUser(

                login = user.login,

                name = user.name,

                isOwner = user.isOwner

            )

        }

    }




    fun restoreUser(
        login:String,
        name:String,
        isOwner:Boolean
    ){

        currentUser =
            LocalUserAccount(

                name = name,

                login = login,

                password = "",

                isOwner = isOwner

            )

        saveUser()
    }




    fun logout(){

        currentUser = null


        dataStoreManager?.let { manager ->


            CoroutineScope(
                Dispatchers.IO
            ).launch {

                manager.clearUser()

            }

        }

    }




    fun submitJoinRequest(
        name:String,
        login:String,
        inviteCode:String,
        password:String
    ){

        pendingJoinRequests.add(

            PendingJoinRequest(
                name,
                login,
                inviteCode
            )

        )

    }




    fun approveRequest(
        request: PendingJoinRequest
    ){

        users.add(

            LocalUserAccount(

                name = request.name,

                login = request.login,

                password = "123456"

            )

        )


        pendingJoinRequests.remove(request)

    }




    fun declineRequest(
        request: PendingJoinRequest
    ){

        pendingJoinRequests.remove(request)

    }




    fun getFamilyMembers():
            List<LocalUserAccount>{

        return users

    }

}