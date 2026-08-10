package com.qrscanner.qrscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ScanHistoryEntity>>

    @Query(
        "SELECT * FROM scan_history WHERE content LIKE '%' || :query || '%' ESCAPE '\\' " +
            "ORDER BY timestamp DESC"
    )
    fun searchHistory(query: String): Flow<List<ScanHistoryEntity>>

    @Insert
    suspend fun insert(history: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}
