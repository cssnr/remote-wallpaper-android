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
import androidx.room.migration.Migration
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

    // FIX AI: Update ONLY the cache validator columns. Do NOT use addOrUpdate() for this:
    // @Upsert overwrites ALL columns, which would reset active to false on rows
    // that already exist (only one remote may be active at a time).
    // Rows whose url is not in the table are silently skipped (0 rows updated).
    @Query("UPDATE Remote SET etag = :etag, lastModified = :lastModified WHERE url = :url")
    fun updateCacheHeaders(url: String, etag: String?, lastModified: String?)

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
    val etag: String? = null,
    val lastModified: String? = null,
)


@Database(entities = [Remote::class], version = 2, exportSchema = false)
abstract class RemoteDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao

    companion object {
        @Volatile
        private var instance: RemoteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Remote ADD COLUMN etag TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE Remote ADD COLUMN lastModified TEXT DEFAULT NULL")
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
