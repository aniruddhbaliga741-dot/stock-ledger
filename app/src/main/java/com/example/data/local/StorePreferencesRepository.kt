package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.SupplyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storeDataStore: DataStore<Preferences> by preferencesDataStore(name = "stockledger_prefs")

data class StoreProfile(
    val shopName: String = "Baliga General & Mart",
    val gstin: String = "",
    val phone: String = "",
    val address: String = "",
    val currencySymbol: String = "₹",
    val defaultSupplyType: SupplyType = SupplyType.INTRA_STATE,
    val hasSeededCatalog: Boolean = false
)

class StorePreferencesRepository(private val context: Context) {

    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val GSTIN = stringPreferencesKey("gstin")
        val PHONE = stringPreferencesKey("phone")
        val ADDRESS = stringPreferencesKey("address")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val SUPPLY_TYPE = stringPreferencesKey("default_supply_type")
        val HAS_SEEDED = booleanPreferencesKey("has_seeded_catalog")
    }

    val storeProfile: Flow<StoreProfile> = context.storeDataStore.data.map { prefs ->
        val supplyRaw = prefs[Keys.SUPPLY_TYPE] ?: SupplyType.INTRA_STATE.name
        val supplyType = runCatching { SupplyType.valueOf(supplyRaw) }
            .getOrDefault(SupplyType.INTRA_STATE)
        StoreProfile(
            shopName = prefs[Keys.SHOP_NAME] ?: "My Local Store",
            gstin = prefs[Keys.GSTIN] ?: "",
            phone = prefs[Keys.PHONE] ?: "",
            address = prefs[Keys.ADDRESS] ?: "",
            currencySymbol = prefs[Keys.CURRENCY] ?: "₹",
            defaultSupplyType = supplyType,
            hasSeededCatalog = prefs[Keys.HAS_SEEDED] ?: false
        )
    }

    suspend fun updateStoreProfile(
        shopName: String,
        gstin: String,
        phone: String,
        address: String,
        currencySymbol: String,
        defaultSupplyType: SupplyType
    ) {
        context.storeDataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = shopName.trim().ifEmpty { "My Local Store" }
            prefs[Keys.GSTIN] = gstin.trim().uppercase()
            prefs[Keys.PHONE] = phone.trim()
            prefs[Keys.ADDRESS] = address.trim()
            prefs[Keys.CURRENCY] = currencySymbol.ifEmpty { "₹" }
            prefs[Keys.SUPPLY_TYPE] = defaultSupplyType.name
        }
    }

    suspend fun markCatalogSeeded() {
        context.storeDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED] = true
        }
    }
}
