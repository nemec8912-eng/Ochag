package com.ochag.app.data

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

import androidx.sqlite.db.SupportSQLiteDatabase


@Database(
    entities = [
        Message::class,
        ChatEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class OchagDatabase : RoomDatabase() {


    abstract fun messageDao(): MessageDao


    abstract fun chatDao(): ChatDao


    companion object {


        @Volatile
        private var INSTANCE: OchagDatabase? = null


        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS chats (
                            id INTEGER NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            type TEXT NOT NULL,
                            lastMessage TEXT NOT NULL,
                            lastTime TEXT NOT NULL,
                            unreadCount INTEGER NOT NULL,
                            members TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE messages
                        ADD COLUMN messageType TEXT NOT NULL
                        DEFAULT 'TEXT'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE messages
                        ADD COLUMN attachmentUri TEXT NOT NULL
                        DEFAULT ''
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE messages
                        ADD COLUMN fileName TEXT NOT NULL
                        DEFAULT ''
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_3_4 =
            object : Migration(3, 4) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE messages
                        ADD COLUMN status TEXT NOT NULL
                        DEFAULT 'SENT'
                        """.trimIndent()
                    )
                }
            }


        private val MIGRATION_4_5 =
            object : Migration(4, 5) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE messages
                        ADD COLUMN createdAt INTEGER NOT NULL
                        DEFAULT 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        UPDATE messages
                        SET createdAt = id / 1000
                        WHERE createdAt = 0
                        """
                    )
                }
            }


        private val MIGRATION_5_6 =
            object : Migration(5, 6) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        UPDATE messages
                        SET createdAt = createdAt / 1000
                        WHERE createdAt > 10000000000000
                        """
                    )

                    database.execSQL(
                        """
                        UPDATE messages
                        SET createdAt = id / 1000
                        WHERE createdAt = 0
                        """
                    )
                }
            }


        private val MIGRATION_6_7 =
            object : Migration(6, 7) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE chats
                        ADD COLUMN lastActivityAt INTEGER NOT NULL
                        DEFAULT 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        UPDATE chats
                        SET lastActivityAt = COALESCE(
                            (
                                SELECT MAX(messages.createdAt)
                                FROM messages
                                WHERE messages.chatId = chats.id
                            ),
                            0
                        )
                        """.trimIndent()
                    )
                }
            }


        fun getInstance(
            context: Context
        ): OchagDatabase {


            return INSTANCE
                ?: synchronized(this) {


                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            OchagDatabase::class.java,
                            "ochag_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7
                            )
                            .build()


                    INSTANCE =
                        instance


                    instance
                }
        }
    }
}
