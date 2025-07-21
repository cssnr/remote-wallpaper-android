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
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.Executors

@Dao
interface RemoteDao {
    @Query("SELECT * FROM remote")
    fun getAll(): List<Remote>

    @Query("SELECT * FROM remote WHERE active = 1 LIMIT 1")
    fun getActive(): Remote?

    @Query("SELECT * FROM remote WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): Remote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(remotes: List<Remote>)

    //@Insert
    //fun add(remote: Remote)

    @Upsert
    fun addOrUpdate(remote: Remote)

    @Query("UPDATE Remote SET active = 1 WHERE ROWID = (SELECT ROWID FROM Remote LIMIT 1)")
    fun activateFirst()

    @Query("UPDATE remote SET active = 0 WHERE active = 1")
    fun deactivateAll()

    @Query("UPDATE remote SET active = 1 WHERE url = :url")
    fun activateByUrl(url: String)

    @Transaction
    fun activate(remote: Remote?): Boolean {
        if (remote != null) {
            deactivateAll()
            activateByUrl(remote.url)
            return true
        }
        return false
    }

    @Delete
    fun delete(remote: Remote)
}


//@Entity(
//    indices = [Index(value = ["url"], unique = true)]
//)
@Entity
data class Remote(
    //@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @PrimaryKey val url: String,
    val active: Boolean = false,
)


@Database(entities = [Remote::class], version = 1)
abstract class RemoteDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao

    companion object {
        @Volatile
        private var instance: RemoteDatabase? = null

        private val defaultData: List<Remote> = listOf(
            Remote("https://picsum.photos/4800/2400", active = true),
            Remote("https://picsum.photos/4800/2400?blur=10", active = false),
            Remote("https://picsum.photos/4800/2400?grayscale", active = false),
            Remote("https://images.cssnr.com/aviation", active = false),
        )

        fun getInstance(context: Context): RemoteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RemoteDatabase::class.java,
                    "remote-database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Executors.newSingleThreadExecutor().execute {
                                getInstance(context).remoteDao().apply {
                                    insertAll(defaultData)
                                }
                            }
                        }
                    })
                    .build().also { instance = it }
            }
    }
}
