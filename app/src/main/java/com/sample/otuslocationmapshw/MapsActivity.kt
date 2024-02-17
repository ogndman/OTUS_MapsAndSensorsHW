package com.sample.otuslocationmapshw


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sample.otuslocationmapshw.camera.CameraActivity
import com.sample.otuslocationmapshw.data.utils.LocationDataUtils
import com.sample.otuslocationmapshw.databinding.ActivityMapsBinding
import java.io.File

private const val TAG = "MapsActivity"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val locationDataUtils = LocationDataUtils()
    private val cameraForResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CameraActivity.SUCCESS_RESULT_CODE) {
            // TODO("Обновить точки на карте при получении результата от камеры")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{it->
            Log.i(TAG, "Maps is ready")
            onMapReady(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                cameraForResultLauncher.launch(Intent(this, CameraActivity::class.java))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        showPreviewsOnMap()
    }

    private fun showPreviewsOnMap() {
        map.clear()
        val folder = File("${filesDir.absolutePath}/photos/")
        folder.listFiles()?.forEach {
            // Создается объект ExifInterface для текущего файла. ExifInterface используется для чтения метаданных изображения (EXIF).
            val exifInterface = ExifInterface(it)
            // Вызывается метод getLocationFromExif для получения местоположения из метаданных EXIF изображения.
            val location = locationDataUtils.getLocationFromExif(exifInterface)
            // Создается объект LatLng, представляющий координаты местоположения изображения (широту и долготу).
            val point = LatLng(location.latitude, location.longitude)
            // Загружается изображение из файла и масштабируется до требуемого размера (64x64 пикселя).
            val pinBitmap = Bitmap.createScaledBitmap(
                BitmapFactory.decodeFile(
                    it.path,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }), 64, 64, false
            )
            // Создается объект BitmapDescriptor с помощью метода fromBitmap() фабрики
            val icon = BitmapDescriptorFactory.fromBitmap(pinBitmap)
            map.addMarker(
                MarkerOptions()
                    .position(point)
                    .icon(icon)
            )
            // Передвигаем камеру к позиции маркера
            val cameraPosition = CameraPosition.Builder()
                .target(point) // Устанавливаем цель камеры в координаты местоположения фото
                .zoom(15f) // Устанавливаем уровень масштабирования
                .build()
            // Перемещаем камеру
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }
}