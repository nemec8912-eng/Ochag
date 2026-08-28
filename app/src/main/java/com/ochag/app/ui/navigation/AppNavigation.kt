package com.ochag.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ochag.app.data.ChatRepository
import com.ochag.app.data.OchagAppState
import com.ochag.app.data.ServerRealtime
import com.ochag.app.ui.screens.home.HomeScreen
import com.ochag.app.ui.screens.profile.ProfileScreen
import com.ochag.app.ui.screens.login.LoginScreen
import com.ochag.app.ui.screens.registration.InviteRegistrationScreen
import com.ochag.app.ui.screens.files.FilesMediaScreen
import com.ochag.app.ui.screens.chats.ChatListScreen
import com.ochag.app.ui.screens.chats.ChatRoomWithMessageActions
import com.ochag.app.ui.screens.chats.NewChatScreen
import com.ochag.app.ui.screens.family.FamilyManagementScreen
import com.ochag.app.ui.screens.family.MemberProfileScreen
import com.ochag.app.ui.screens.stickers.StickerPacksScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chats : Screen("chats")
    object NewChat : Screen("new_chat")
    object ChatRoom : Screen("chat/{chatId}")
    object Family : Screen("family")
    object MemberProfile : Screen("profile/{name}")
    object MyProfile : Screen("my_profile")
    object Files : Screen("files")
    object Stickers : Screen("stickers")
    object Login : Screen("login")
    object InviteRegistration : Screen("invite_registration")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ChatRepository.initialize(context)
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onChatClick = { chatId -> navController.navigate("chat/$chatId") },
                onChatsClick = { navController.navigate(Screen.Chats.route) },
                onNewChatClick = { navController.navigate(Screen.NewChat.route) },
                onFamilyClick = { navController.navigate(Screen.Family.route) },
                onProfileClick = { navController.navigate(Screen.MyProfile.route) }
            )
        }

        composable(Screen.MyProfile.route) {
            ProfileScreen(
                onChatsClick = {
                    navController.navigate(Screen.Chats.route) { launchSingleTop = true }
                },
                onFamilyClick = { navController.navigate(Screen.Family.route) },
                onFilesClick = {
                    navController.navigate(Screen.Files.route) { launchSingleTop = true }
                },
                onInviteClick = { navController.navigate(Screen.InviteRegistration.route) },
                onStickersClick = { navController.navigate(Screen.Stickers.route) },
                onLogoutClick = {
                    OchagAppState.logout()
                    ServerRealtime.disconnect()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Files.route) {
            FilesMediaScreen(
                onBackClick = { navController.popBackStack() },
                onChatsClick = {
                    navController.navigate(Screen.Chats.route) { launchSingleTop = true }
                },
                onProfileClick = {
                    navController.navigate(Screen.MyProfile.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Stickers.route) {
            StickerPacksScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    val user = OchagAppState.currentUser
                    if (user != null) {
                        ServerRealtime.connect(context = context, userName = user.name)
                    }
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onInviteRegistrationClick = {
                    navController.navigate(Screen.InviteRegistration.route)
                }
            )
        }

        composable(Screen.InviteRegistration.route) {
            InviteRegistrationScreen(
                onBackClick = { navController.popBackStack() },
                onRequestSent = { }
            )
        }

        composable(Screen.NewChat.route) {
            NewChatScreen(
                onBackClick = { navController.popBackStack() },
                onChatCreated = { chat ->
                    navController.navigate("chat/${chat.id}") {
                        popUpTo(Screen.NewChat.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chats.route) {
            ChatListScreen(
                onFilesClick = {
                    navController.navigate(Screen.Files.route) { launchSingleTop = true }
                },
                onProfileClick = {
                    navController.navigate(Screen.MyProfile.route) { launchSingleTop = true }
                },
                onChatClick = { id, _ -> navController.navigate("chat/$id") }
            )
        }

        composable(Screen.ChatRoom.route) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: 0L
            val chat = ChatRepository.findChat(chatId)
            val title = chat?.title ?: "Чат"

            LaunchedEffect(chatId) {
                if (chatId > 0L) {
                    ChatRepository.markChatOpened(
                        context = context,
                        chatId = chatId
                    )
                }
            }

            DisposableEffect(chatId) {
                onDispose {
                    if (chatId > 0L) {
                        ChatRepository.markChatClosed(chatId)
                    }
                }
            }

            ChatRoomWithMessageActions(
                chatId = chatId,
                chatTitle = title,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Family.route) {
            FamilyManagementScreen(
                onMemberClick = { name -> navController.navigate("profile/$name") }
            )
        }

        composable(Screen.MemberProfile.route) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""

            MemberProfileScreen(
                name = name,
                status = "Участник семьи",
                onBackClick = { navController.popBackStack() },
                onMessageClick = {
                    coroutineScope.launch {
                        val chat = ChatRepository.createPrivateChat(
                            context = context,
                            memberName = name
                        )
                        navController.navigate("chat/${chat.id}")
                    }
                }
            )
        }
    }
}
