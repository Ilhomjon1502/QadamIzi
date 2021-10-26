package com.ilhomjon.serviceworkmanager.Room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng

@Entity
class MyLocation {

    @PrimaryKey(autoGenerate = true)
    var id:Int? = null

    var latitude:Double? = null
    var longitude:Double? = null
    var time:String? = null
}