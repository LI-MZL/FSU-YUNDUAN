package com.fsu.native.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.Delete
import com.fsu.native.model.AlarmRecord
import com.fsu.native.model.Converters
import com.fsu.native.model.ProtoLog
import android.content.Context
import kotlinx.coroutines.CoroutineScope

/**
 * Room 数据库 - FSU数据持久化
 * 包含告警记录和协议日志两张表
 */
@Database(
    entities = [AlarmRecord::class, ProtoLog::class],
    version = 1,
    autoMigrations = []
)
@androidx.room.TypeConverters(Converters::class)
abstract class FSUDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun protoLogDao(): ProtoLogDao

    companion object {
        @Volatile
        private var INSTANCE: FSUDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): FSUDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FSUDatabase::class.java,
                    "fsu_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================================================================
// 告警记录 DAO
// ============================================================================

@Dao
interface AlarmDao {

    @Insert
    suspend fun insert(alarm: AlarmRecord): Long

    @Query("SELECT * FROM alarm_records ORDER BY alarmTime DESC")
    suspend fun getAll(): List<AlarmRecord>

    @Query("SELECT * FROM alarm_records WHERE alarmFlag = 'START' ORDER BY alarmTime DESC")
    suspend fun getActive(): List<AlarmRecord>

    @Query("DELETE FROM alarm_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE alarm_records SET alarmFlag = 'END' WHERE itemId = :itemId AND alarmFlag = 'START'")
    suspend fun clearActive(itemId: String)

    @Query("UPDATE alarm_records SET alarmFlag = 'END' WHERE alarmFlag = 'START'")
    suspend fun clearAllActive()

    @Query("SELECT COUNT(*) FROM alarm_records WHERE alarmFlag = 'START'")
    suspend fun getActiveCount(): Int

    @Query("SELECT * FROM alarm_records WHERE devId = :devId ORDER BY alarmTime DESC")
    suspend fun getByDevId(devId: String): List<AlarmRecord>

    @Query("DELETE FROM alarm_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM alarm_records")
    suspend fun getTotalCount(): Int
}

// ============================================================================
// 协议日志 DAO
// ============================================================================

@Dao
interface ProtoLogDao {

    @Insert
    suspend fun insert(log: ProtoLog): Long

    @Insert
    suspend fun insertAll(logs: List<ProtoLog>): List<Long>

    @Query("SELECT * FROM proto_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<ProtoLog>

    @Query("SELECT * FROM proto_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ProtoLog>

    @Query("DELETE FROM proto_logs WHERE id IN (SELECT id FROM proto_logs ORDER BY timestamp DESC LIMIT -1 OFFSET :count)")
    suspend fun deleteOld(count: Int)

    @Query("DELETE FROM proto_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM proto_logs")
    suspend fun getTotalCount(): Int

    @Query("DELETE FROM proto_logs WHERE id NOT IN (SELECT id FROM proto_logs ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trimToMax(count: Int = 500)
}
