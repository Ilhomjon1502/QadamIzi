package com.ilhomjon.serviceworkmanager.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MyTimeDao {
    @Query("select * from MyLocation")
    fun getAllTime():List<MyLocation>

    @Insert
    fun addTime(myTime: MyLocation)
}