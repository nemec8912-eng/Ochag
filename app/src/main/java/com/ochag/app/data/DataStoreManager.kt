package com.ochag.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.dataStore by preferencesDataStore(
    name = "ochag_user_prefs"
)


class DataStoreManager(
    private val context: Context
){


    companion object{

        private val LOGIN =
            stringPreferencesKey("login")

        private val NAME =
            stringPreferencesKey("name")

        private val OWNER =
            booleanPreferencesKey("owner")

    }



    suspend fun saveUser(
        login:String,
        name:String,
        isOwner:Boolean
    ){

        context.dataStore.edit { prefs ->

            prefs[LOGIN] = login

            prefs[NAME] = name

            prefs[OWNER] = isOwner

        }

    }



    val userFlow: Flow<Triple<String,String,Boolean>?> =

        context.dataStore.data.map { prefs ->


            val login =
                prefs[LOGIN]


            val name =
                prefs[NAME]


            val owner =
                prefs[OWNER]


            if(login != null && name != null){

                Triple(
                    login,
                    name,
                    owner ?: false
                )

            } else {

                null

            }

        }




    suspend fun clearUser(){

        context.dataStore.edit {

            it.clear()

        }

    }

}