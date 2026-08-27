package com.ochag.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StickerPack(
    val id: String,
    val title: String,
    val emojiStickers: List<String> = emptyList(),
    val imageUris: List<String> = emptyList(),
    val builtIn: Boolean = false
)

object StickerStore {

    private const val PREFS_NAME = "ochag_stickers"
    private const val KEY_PACKS = "custom_packs"

    val packs = mutableStateListOf<StickerPack>()

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        reload()
    }

    private fun builtInPacks(): List<StickerPack> = listOf(
        StickerPack(
            id = "family",
            title = "Семья",
            emojiStickers = listOf("❤️", "🤗", "🥰", "😘", "🫶", "🏡", "🔥", "☕", "🍰", "🌿", "✨", "🎉"),
            builtIn = true
        ),
        StickerPack(
            id = "mood",
            title = "Настроение",
            emojiStickers = listOf("😂", "🤣", "😎", "🥳", "😴", "🤔", "😇", "🙈", "🤭", "😋", "🤩", "🫠"),
            builtIn = true
        )
    )

    fun reload() {
        val result = builtInPacks().toMutableList()
        val json = appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_PACKS, null)

        if (!json.isNullOrBlank()) {
            runCatching {
                val array = JSONArray(json)
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val images = item.optJSONArray("images") ?: JSONArray()
                    result += StickerPack(
                        id = item.getString("id"),
                        title = item.optString("title", "Мой стикерпак"),
                        imageUris = buildList {
                            for (imageIndex in 0 until images.length()) {
                                add(images.getString(imageIndex))
                            }
                        }
                    )
                }
            }
        }

        packs.clear()
        packs.addAll(result)
    }

    fun createPack(title: String): String {
        val cleanTitle = title.trim().ifBlank { "Мой стикерпак" }
        val id = UUID.randomUUID().toString()
        packs += StickerPack(id = id, title = cleanTitle)
        saveCustomPacks()
        return id
    }

    fun renamePack(packId: String, title: String) {
        val index = packs.indexOfFirst { it.id == packId && !it.builtIn }
        if (index < 0) return
        packs[index] = packs[index].copy(title = title.trim().ifBlank { packs[index].title })
        saveCustomPacks()
    }

    fun addImages(packId: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = appContext ?: return
        val index = packs.indexOfFirst { it.id == packId && !it.builtIn }
        if (index < 0) return

        val strings = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            uri.toString()
        }

        packs[index] = packs[index].copy(
            imageUris = (packs[index].imageUris + strings).distinct()
        )
        saveCustomPacks()
    }

    fun removeSticker(packId: String, uri: String) {
        val index = packs.indexOfFirst { it.id == packId && !it.builtIn }
        if (index < 0) return
        packs[index] = packs[index].copy(
            imageUris = packs[index].imageUris.filterNot { it == uri }
        )
        saveCustomPacks()
    }

    fun deletePack(packId: String) {
        packs.removeAll { it.id == packId && !it.builtIn }
        saveCustomPacks()
    }

    private fun saveCustomPacks() {
        val array = JSONArray()
        packs.filterNot { it.builtIn }.forEach { pack ->
            array.put(
                JSONObject().apply {
                    put("id", pack.id)
                    put("title", pack.title)
                    put("images", JSONArray(pack.imageUris))
                }
            )
        }

        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_PACKS, array.toString())
            ?.apply()
    }
}
