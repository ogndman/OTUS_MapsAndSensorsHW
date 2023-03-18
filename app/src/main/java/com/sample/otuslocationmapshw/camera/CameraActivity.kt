package com.sample.otuslocationmapshw.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_LOW_POWER
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.common.util.concurrent.ListenableFuture
import com.sample.otuslocationmapshw.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs


class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var sensorManager: SensorManager
    private lateinit var sensorEventListener: SensorEventListener
    private var tiltSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // TODO("Получить экземпляр SensorManager")
        // TODO("Добавить проверку на наличие датчика акселерометра и присвоить значение tiltSensor")
        tiltSensor = if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Акселеромент не обнаружен!
            null
        }

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))

        sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val tilt = event.values[2]
                binding.errorTextView.visibility = if (abs(tilt) > 2) View.VISIBLE else View.GONE
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                //nothing to do
            }
        }

        binding.takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onStart() {
        super.onStart()
        sensorManager.registerListener(sensorEventListener,tiltSensor,1)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)

    }

    // TODO("Остановить получение событий от датчика")

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun takePhoto() {
        getLastLocation { location ->
            Log.d("LOCATION", location.toString())

            val folderPath = "${filesDir.absolutePath}/photos/"
            val folder = File(folderPath)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val filePath =
                folderPath + SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(Date())

            val meta = ImageCapture.Metadata()
            getLastLocation { location ->
                meta.location = location
            }
            // TODO("4. Добавить установку местоположения в метаданные фото")
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(filePath))
                .setMetadata(meta)
                .build()


            // TODO("Добавить вызов CameraX для фото")
            // TODO("Вывести Toast о том, что фото успешно сохранено и закрыть текущее активити c указанием кода результата SUCCESS_RESULT_CODE")
            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Toast.makeText(this@CameraActivity, "Фото сохранено!", Toast.LENGTH_SHORT)
                            .show()
                        setResult(SUCCESS_RESULT_CODE)
                        finish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    }

                })
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(callback: (location: Location?) -> Unit) {
        val cts = CancellationTokenSource()

        val loc = fusedLocationClient.getCurrentLocation(PRIORITY_LOW_POWER, cts.token)
        callback.invoke(loc.result)

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {

        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10

        // TODO("Указать набор требуемых разрешений")
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION

        ).toTypedArray()

        const val SUCCESS_RESULT_CODE = 15
    }
}