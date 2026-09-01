package com.splitmate.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        FriendEntity::class,
        GroupEntity::class,
        ExpenseEntity::class,
        SettlementEntity::class,
        AppPreferenceEntity::class,
        PersonalExpenseEntity::class,
        ChatMessageEntity::class,
        BalanceHistoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun personalExpenseDao(): PersonalExpenseDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun balanceHistoryDao(): BalanceHistoryDao

    companion object {
        @Volatile
        private var LIVE_INSTANCE: AppDatabase? = null

        @Volatile
        private var DEMO_INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `personal_expenses` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `categoryName` TEXT NOT NULL, `notes` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `senderId` TEXT NOT NULL, `message` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `budget` REAL")
                db.execSQL("CREATE TABLE IF NOT EXISTS `balance_history` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `title` TEXT NOT NULL, `eventTypeName` TEXT NOT NULL, `paidByUserId` TEXT NOT NULL, `otherUserId` TEXT NOT NULL, `fullAmount` REAL NOT NULL, `currentUserShare` REAL NOT NULL, `signedChange` REAL NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `ownerUserId` TEXT")
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `mutedUntilMillis` INTEGER")
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `pendingDeletionAtMillis` INTEGER")
                db.execSQL("ALTER TABLE `groups` ADD COLUMN `deletionDeadlineMillis` INTEGER")
                db.execSQL("UPDATE `groups` SET `ownerUserId` = 'u1' WHERE `id` = 'demo_group_goa'")
                db.execSQL("UPDATE `groups` SET `ownerUserId` = 'u2' WHERE `id` = 'demo_group_apartment'")
                db.execSQL("UPDATE `groups` SET `ownerUserId` = 'u3' WHERE `id` = 'demo_group_foodies'")
            }
        }

        fun getLiveDatabase(context: Context): AppDatabase {
            return LIVE_INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "splitmate_live.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                LIVE_INSTANCE = instance
                instance
            }
        }

        fun getDemoDatabase(context: Context): AppDatabase {
            return DEMO_INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "splitmate_demo.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                DEMO_INSTANCE = instance
                instance
            }
        }
    }
}
