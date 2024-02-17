package com.sample.otuslocationmapshw.data.utils

import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.LATITUDE_NORTH
import androidx.exifinterface.media.ExifInterface.LATITUDE_SOUTH
import androidx.exifinterface.media.ExifInterface.LONGITUDE_EAST
import androidx.exifinterface.media.ExifInterface.LONGITUDE_WEST

// Константы для преобразования GPS-координат из градусов, минут, секунд в десятичные градусы
private const val MINUTES = 60
private const val SECONDS = 3600

// Класс утилиты для извлечения данных о местоположении из ExifInterface
class LocationDataUtils {

    // Функция для извлечения данных о местоположении из ExifInterface и возврата объекта LocationPosition
    fun getLocationFromExif(exif: ExifInterface): LocationPosition {
        // Получаем значения широты и долготы из ExifInterface
        val rationalLat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
        val latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
        val rationalLng = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
        val lngRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)

        // Преобразование рациональных значений широты и долготы в десятичные градусы
        val latitude = convert(rationalLat, latRef)
        val longitude = convert(rationalLng, lngRef)

        // Возвращаем объект LocationPosition со значениями широты и долготы
        return LocationPosition(latitude, longitude)
    }

    // Функция для преобразования рациональной широты или долготы в десятичные градусы
    private fun convert(rationalValues: String?, directionTag: String?): Double {
        return rationalValues?.let { attribute ->
            // Определить множитель знака на основе тега направления (Север, Юг, Восток, Запад)
            val signMultiplier = when (directionTag) {
                LATITUDE_NORTH, LONGITUDE_EAST -> 1
                LATITUDE_SOUTH, LONGITUDE_WEST -> -1
                else -> 0 // По умолчанию 0, если тег направления недействителен
            }

            // Разделяем рациональные значения, преобразуем их в десятичные градусы и суммируем
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
        } ?: 0.0 // Возвращаем 0,0, если рациональные значения равны нулю
    }

    // Класс данных, представляющий положение местоположения с широтой и долготой
    data class LocationPosition(
        val latitude: Double,
        val longitude: Double,
    )
}