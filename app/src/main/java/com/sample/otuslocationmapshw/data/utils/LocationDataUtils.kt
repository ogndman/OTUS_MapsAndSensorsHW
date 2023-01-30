package com.sample.otuslocationmapshw.data.utils

import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.LATITUDE_NORTH
import androidx.exifinterface.media.ExifInterface.LATITUDE_SOUTH
import androidx.exifinterface.media.ExifInterface.LONGITUDE_EAST
import androidx.exifinterface.media.ExifInterface.LONGITUDE_WEST

private const val MINUTES = 60
private const val SECONDS = 3600

class LocationDataUtils {

    fun getLocationFromExif(exif: ExifInterface): LocationPosition {
        val rationalLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val rationalLng = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
        val latitude = convert(rationalLat, latRef)
        val longitude = convert(rationalLng, lngRef)

        return LocationPosition(latitude, longitude)
    }

    private fun convert(rationalValues: String?, directionTag: String?): Double {
        return rationalValues?.let { attribute ->
            val signMultiplier = when (directionTag) {
                LATITUDE_NORTH, LONGITUDE_EAST -> 1
                LATITUDE_SOUTH, LONGITUDE_WEST -> -1
                else -> 0
            }
            val coordinate = attribute.split(',').mapIndexed { index: Int, item: String ->
                val ratio = item.split('/')
                val divider = when (index) {
                    1 -> MINUTES
                    2 -> SECONDS
                    else -> 1
                }
                ratio[0].toDouble() / ratio[1].toInt() / divider
            }.sum() * signMultiplier

            return coordinate
        } ?: 0.0
    }

    data class LocationPosition(
        val latitude: Double,
        val longitude: Double
    )
}