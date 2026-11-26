package com.hmx.harness.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hmx.harness.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device test: an existing v27 database keeps its rows and gains safe defaults. */
@RunWith(AndroidJUnit4::class)
class UsageMigrationDeviceTest {
    @Test fun v27UsageRowsGainChatKindAndAttemptZero() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "usage-migration-${System.nanoTime()}"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                object : SupportSQLiteOpenHelper.Callback(27) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE `usage_events` (" +
                                "`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`sessionId` TEXT NOT NULL, `providerName` TEXT NOT NULL, `model` TEXT NOT NULL, " +
                                "`inputTokens` INTEGER NOT NULL, `outputTokens` INTEGER NOT NULL, " +
                                "`cachedTokens` INTEGER NOT NULL DEFAULT 0, `cacheWriteTokens` INTEGER NOT NULL DEFAULT 0, " +
                                "`createdAt` INTEGER NOT NULL, `turnId` TEXT, " +
                                "`cacheReported` INTEGER NOT NULL DEFAULT 0, " +
                                "`inputPrice` REAL, `cacheReadPrice` REAL, `cacheWritePrice` REAL)"
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                },
            ).build(),
        )
        val legacy = helper.writableDatabase
        legacy.execSQL(
            "INSERT INTO usage_events (sessionId, providerName, model, inputTokens, outputTokens, " +
                "cachedTokens, cacheWriteTokens, createdAt, cacheReported) " +
                "VALUES ('old-session', 'Legacy', 'old-model', 100, 10, 20, 0, 1000, 0)",
        )
        assertEquals(27, legacy.version)
        AppDatabase.MIGRATION_27_28.migrate(legacy)
        legacy.version = 28
        legacy.query("SELECT kind, attempt, inputTokens FROM usage_events").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(UsageKind.CHAT, cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(100L, cursor.getLong(2))
        }
        legacy.close()
        helper.close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val repository = SessionRepository(db)
        val migrated = repository.usageEventsOnce("old-session").single()
        assertEquals(UsageKind.CHAT, migrated.kind)
        assertEquals(0, migrated.attempt)
        assertEquals(100L, migrated.inputTokens)

        // New rows carry their own kind/attempt; the old row remains readable
        // and the totals query still sees both.
        repository.recordUsage("old-session", "Legacy", "old-model", 5, 1, 0, 0,
            kind = UsageKind.COMPACTION, attempt = 2)
        val rows = repository.usageEventsOnce("old-session")
        assertEquals(2, rows.size)
        assertTrue(rows.any { it.inputTokens == 100L && it.kind == UsageKind.CHAT && it.attempt == 0 })
        assertTrue(rows.any { it.inputTokens == 5L && it.kind == UsageKind.COMPACTION && it.attempt == 2 })
        val stats = repository.statsUsageBetween(0, Long.MAX_VALUE).first().filter { it.sessionId == "old-session" }
        assertEquals(105L, stats.sumOf { it.inputTokens })
        assertEquals(2L, stats.sumOf { it.requests })
        assertEquals(1L, stats.sumOf { it.retriedRequests })
        assertEquals(UsageKind.COMPACTION, stats.single { it.inputTokens == 5L }.kind)
        db.close()
    }
}
