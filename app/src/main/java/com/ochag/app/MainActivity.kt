package com.ochag.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.ochag.app.data.DataStoreManager
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerRealtime
import com.ochag.app.data.StickerStore
import com.ochag.app.ui.navigation.AppNavigation
import com.ochag.app.ui.theme.OchagTheme
import com.ochag.app.ui.theme.OchagThemeController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "ON_CREATE")

        OchagAppState.init(this)
        OchagThemeController.init(this)
        StickerStore.init(this)

        lifecycleScope.launch {
            val dataStore = DataStoreManager(this@MainActivity)

            dataStore.userFlow.collect { user ->
                if (user != null) {
                    Log.d(
                        TAG,
                        "DataStore user: login=${user.first}, name=${user.second}, owner=${user.third}"
                    )

                    OchagAppState.restoreUser(
                        login = user.first,
                        name = user.second,
                        isOwner = user.third
                    )

                    Log.d(TAG, "CONNECT_FROM_DATASTORE: ${user.second}")

                    ServerRealtime.connect(
                        context = applicationContext,
                        userName = user.second
                    )
                } else {
                    Log.w(TAG, "DataStore user = null")
                    ServerRealtime.disconnect()
                }
            }
        }

        setContent {
            OchagTheme {
                AppNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "ON_START: Подключить к WebSocket")
        ServerRealtime.ensureConnected(applicationContext)
    }

    override fun onStop() {
        Log.d(TAG, "ON_STOP: WebSocket не отключаем")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "ON_DESTROY: isFinishing=$isFinishing")

        if (isFinishing) {
            ServerRealtime.disconnect()
        }

        super.onDestroy()
    }
}
