package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.local.ShopRepository
import com.example.data.local.StorePreferencesRepository
import com.example.ui.ShopViewModel
import com.example.ui.StockLedgerApp
import com.example.ui.theme.StockLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val prefsRepository = StorePreferencesRepository(applicationContext)
        val shopRepository = ShopRepository(database, prefsRepository)

        setContent {
            StockLedgerTheme {
                val shopViewModel: ShopViewModel = viewModel(
                    factory = ShopViewModel.Factory(shopRepository)
                )
                StockLedgerApp(viewModel = shopViewModel)
            }
        }
    }
}
