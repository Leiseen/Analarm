package com.analarm // 你的包名

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao // 告诉Room，这是我们的工作手册
interface AlarmDao {

    // 获取所有的闹钟，并且当数据变化时，能自动通知我们
    @Query("SELECT * FROM alarms ORDER BY hour, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    // 插入一个新的闹钟
    @Insert
    suspend fun insert(alarm: Alarm): Long // 把这里的返回值改成 Long

    // 更新一个已有的闹钟
    @Update
    suspend fun update(alarm: Alarm)

    @Delete // 告诉Room，这是销毁操作
    suspend fun delete(alarm: Alarm)

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): Alarm?

}