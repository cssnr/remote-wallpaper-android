package org.cssnr.remotewallpaper.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Dao
interface HistoryDao {
    @Query("SELECT * FROM historyitem ORDER BY timestamp DESC")
    fun getAll(): List<HistoryItem>

    @Query("SELECT * FROM historyitem ORDER BY timestamp DESC LIMIT :count")
    fun getLatest(count: Int = 50): List<HistoryItem>

    @Query("SELECT * FROM historyitem WHERE remote = :url ORDER BY timestamp DESC LIMIT :count")
    fun getForRemote(url: String, count: Int = 50): List<HistoryItem>

    @Query("SELECT * FROM historyitem ORDER BY timestamp DESC LIMIT 1")
    fun getLast(): HistoryItem?

    @Query("SELECT * FROM historyitem WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): HistoryItem?

    @Query("SELECT * FROM HistoryItem WHERE status = 200 ORDER BY timestamp DESC LIMIT 1")
    fun getLastSuccess(): HistoryItem?

    @Insert
    fun add(historyitem: HistoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(history: List<HistoryItem>)

    @Delete
    fun delete(historyitem: HistoryItem)

    @Query("DELETE FROM historyitem")
    fun deleteAll()
}


@Entity
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    var remote: String? = null,
    var url: String? = null,
    var status: Int = 0,
    var error: String? = null,
)

@Database(entities = [HistoryItem::class], version = 1)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "history-database"
                ).build().also { instance = it }
            }
    }
}


//val date = Date(historyItem.timestamp)
//// or using java.time:
//val instant = Instant.ofEpochMilli(historyItem.timestamp)
//val zonedDateTime = instant.atZone(ZoneId.systemDefault())
