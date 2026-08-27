package com.ochag.app

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.lifecycle.lifecycleScope

import com.ochag.app.data.DataStoreManager
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerRealtime

import com.ochag.app.ui.navigation.AppNavigation
import com.ochag.app.ui.theme.OchagTheme

import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        OchagAppState.init(
            this
        )


        // =====================================================
        // ВОССТАНАВЛИВАЕМ ПОЛЬЗОВАТЕЛЯ
        // И ПОДКЛЮЧАЕМ WEBSOCKET
        // =====================================================

        lifecycleScope.launch {

            val dataStore =
                DataStoreManager(
                    this@MainActivity
                )


            dataStore
                .userFlow
                .collect { user ->


                    if (
                        user != null
                    ) {


                        OchagAppState.restoreUser(

                            login =
                                user.first,

                            name =
                                user.second,

                            isOwner =
                                user.third
                        )


                        ServerRealtime.connect(

                            context =
                                applicationContext,

                            userName =
                                user.second
                        )


                    } else {


                        ServerRealtime.disconnect()
                    }
                }
        }


        // =====================================================
        // UI
        // =====================================================

        setContent {


            OchagTheme {


                AppNavigation()
            }
        }
    }


    override fun onStart() {

        super.onStart()


        ServerRealtime.ensureConnected(
            applicationContext
        )
    }


    override fun onDestroy() {

        if (isFinishing) {

            ServerRealtime.disconnect()
        }


        super.onDestroy()
    }
}