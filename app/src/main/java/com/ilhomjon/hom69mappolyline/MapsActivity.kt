package com.ilhomjon.hom69mappolyline

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.ilhomjon.hom69mappolyline.Service.UploadWork
import com.ilhomjon.hom69mappolyline.databinding.ActivityMapsBinding
import com.ilhomjon.serviceworkmanager.Room.AppDatabase
import com.ilhomjon.serviceworkmanager.Room.MyLocation
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    lateinit var appDatabase: AppDatabase
    lateinit var list: ArrayList<MyLocation>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.imageMap.setOnClickListener{
            if (mMap.mapType == GoogleMap.MAP_TYPE_NORMAL){
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            }else{
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }

        appDatabase = AppDatabase.getInstance(this)

        list = ArrayList()
        list.addAll(appDatabase.timeDao().getAllTime())

        if (list.isEmpty()) {
            askPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) {
                //all permissions already granted or just granted

                val workRequest: WorkRequest =
                    PeriodicWorkRequestBuilder<UploadWork>(15, TimeUnit.MINUTES)
//                .setInitialDelay(10, TimeUnit.SECONDS)
                        .build()
                WorkManager.getInstance(this)
                    .enqueue(workRequest)
                Toast.makeText(
                    this,
                    "Location aniqlash yoqildi GPS ni o'chirmang orqa fonda ham ishlaydi",
                    Toast.LENGTH_LONG
                ).show()
            }.onDeclined { e ->
                if (e.hasDenied()) {

                    AlertDialog.Builder(this)
                        .setMessage("Please accept our permissions")
                        .setPositiveButton("yes") { dialog, which ->
                            e.askAgain();
                        } //ask again
                        .setNegativeButton("no") { dialog, which ->
                            dialog.dismiss();
                        }
                        .show();
                }

                if (e.hasForeverDenied()) {
                    //the list of forever denied permissions, user has check 'never ask again'

                    // you need to open setting manually if you really need it
                    e.goToSettings();
                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

//        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        if (list.isNotEmpty()) {
            val latLngList = ArrayList<LatLng>()
            for (myLocation in list) {
                latLngList.add(LatLng(myLocation.latitude!!, myLocation.longitude!!))
            }

            // Add polylines to the map.
            // Polylines are useful to show a route or some other connection between points.
            val polyline1 = googleMap.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .addAll(latLngList)
            )

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.last(), 15.0f))
            mMap.addMarker(MarkerOptions().position(latLngList.last()).title("Oxirgi yozilgan location"))
            mMap.setOnMapClickListener {
                try {
                    val geocoder = Geocoder(this)
                    var addressList: List<Address> =
                        geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    val address = addressList.get(0)
                    Toast.makeText(this, "${address.getAddressLine(0)}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Addresni ola olmadik. Birozdan so'ng qayta urining...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }else{
       locationUpdates()
        }
    }

    override fun onPolylineClick(p0: Polyline) {
       
    }

    //location
    private lateinit var geocoder: Geocoder


    fun locationUpdates(){
        geocoder = Geocoder(this, Locale.getDefault())

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLastLocation()
        } else {
            //permission denied
            val sydney = LatLng(40.4644485, 71.6500236)
            mMap.addMarker(MarkerOptions().position(sydney).title("It's me"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        }
    }
    private val TAG = "MapsActivity"
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {

            val fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this)
            val locationTask: Task<Location> = fusedLocationProviderClient.lastLocation
            locationTask.addOnSuccessListener { it: Location ->
                if (it != null) {
                    //We have a location
                    Log.d(TAG, "getLastLocation: ${it.toString()}")
                    Log.d(TAG, "getLastLocation: ${it.latitude}")
                    Log.d(TAG, "getLastLocation: ${it.longitude}")
                    mMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).title("It's me"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
                    mMap.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude)).title("Siz turgan joy"))
                    Toast.makeText(this, "Siz turgan joy", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(
                        TAG,
                        "getLastLocation: location was null,,,,,,,,,,,,,,,,,,,..............."
                    )
                }
            }
            locationTask.addOnFailureListener {
                Log.d(TAG, "getLastLocation: ${it.message}")
            }
        }


}