package com.serkodesign.tepera.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.deviceIdDataStore by preferencesDataStore(name = "device_id")
private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

/**
 * NFR-7.3: анонімний локальний ідентифікатор пристрою (DataStore), без прив'язки до особи
 * і без надсилання кудись за межі пристрою в MVP.
 */
class DeviceIdProvider(private val context: Context) {

    suspend fun getOrCreate(): String {
        val existing = context.deviceIdDataStore.data.first()[DEVICE_ID_KEY]
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        context.deviceIdDataStore.edit { it[DEVICE_ID_KEY] = newId }
        return newId
    }
}
