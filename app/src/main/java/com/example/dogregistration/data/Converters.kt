package com.example.dogregistration.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

class Converters {
    private val gson: Gson by lazy { Gson() }

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): ByteArray? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in array) buffer.putFloat(f)
        return buffer.array()
    }

    @TypeConverter
    fun toFloatArray(byteArray: ByteArray?): FloatArray? {
        if (byteArray == null) return null
        return try {
            val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)
            val result = FloatArray(byteArray.size / 4)
            for (i in result.indices) result[i] = buffer.getFloat()
            result
        } catch (ex: Exception) {
            Log.e("Converters", "toFloatArray failed", ex)
            null
        }
    }

    // Always write JSON (never null). Empty list => "[]"
    @TypeConverter
    fun fromVaccinationList(list: List<VaccinationRecord>?): String {
        return try {
            gson.toJson(list ?: emptyList<VaccinationRecord>())
        } catch (ex: Exception) {
            Log.e("Converters", "fromVaccinationList failed", ex)
            "[]"
        }
    }

    @TypeConverter
    fun toVaccinationList(json: String?): List<VaccinationRecord> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            // Create the Type object (keeps Java Type info)
            val type = object : TypeToken<List<VaccinationRecord>>() {}.type
            // Read into a raw object, then cast safely
            val raw: Any? = gson.fromJson<Any>(json, type)
            val list = (raw as? List<*>) ?: return emptyList()
            // Map elements to VaccinationRecord where possible
            list.mapNotNull { element ->
                try {
                    // If element already a VaccinationRecord, return it
                    if (element is VaccinationRecord) return@mapNotNull element
                    // Otherwise convert element -> JSON string -> VaccinationRecord
                    val elementJson = gson.toJson(element)
                    gson.fromJson(elementJson, VaccinationRecord::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (ex: Exception) {
            Log.e("Converters", "toVaccinationList failed", ex)
            emptyList()
        }
    }

    @TypeConverter
    fun fromDogGender(gender: DogGender?): String? = gender?.name

    @TypeConverter
    fun toDogGender(name: String?): DogGender =
        name?.let { runCatching { DogGender.valueOf(it) }.getOrDefault(DogGender.None) } ?: DogGender.None

    @TypeConverter
    fun fromDogColor(color: DogColor?): String? = color?.name

    @TypeConverter
    fun toDogColor(name: String?): DogColor =
        name?.let { runCatching { DogColor.valueOf(it) }.getOrDefault(DogColor.NONE) } ?: DogColor.NONE

    @TypeConverter
    fun fromDogType(type: DogType?): String? = type?.name

    @TypeConverter
    fun toDogType(name: String?): DogType =
        name?.let { runCatching { DogType.valueOf(it) }.getOrDefault(DogType.PET) } ?: DogType.PET
}
