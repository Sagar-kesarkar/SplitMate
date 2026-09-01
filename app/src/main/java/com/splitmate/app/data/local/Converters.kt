package com.splitmate.app.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        if (list == null || list.isEmpty()) return "[]"
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        return try {
            val jsonArray = JSONArray(data)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromDoubleMap(map: Map<String, Double>?): String {
        if (map == null || map.isEmpty()) return "{}"
        val jsonObj = JSONObject()
        map.forEach { (k, v) -> jsonObj.put(k, v) }
        return jsonObj.toString()
    }

    @TypeConverter
    fun toDoubleMap(data: String?): Map<String, Double> {
        if (data.isNullOrEmpty()) return emptyMap()
        return try {
            val jsonObj = JSONObject(data)
            val map = mutableMapOf<String, Double>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObj.getDouble(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
