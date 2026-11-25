package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.ProductEntity
import com.example.ui.components.ProductEditorDialog
import com.example.ui.components.RestockDialog
import com.example.ui.components.StoreProfileDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GstLabScreen
import com.example.ui.screens.GstLabTab
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.QuickBillScreen

object AppRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_INVENTORY = "inventory"
    const val ROUTE_BILLING = "quick_bill"
    const val ROUTE_GST_LAB = "gst_lab"
    const val ROUTE_LEDGER = "ledger"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navDestinations = listOf(
    NavDestinationItem(
        route = AppRoutes.ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    NavDestinationItem(
        route = AppRoutes.ROUTE_INVENTORY,
        labelRes = R.string.nav_inventory,
        selectedIcon = Icons.Filled.Inventory2,
        unselectedIcon = Icons.Outlined.Inventory2,
        testTag = "nav_inventory"
    ),
    NavDestinationItem(
        route = AppRoutes.ROUTE_BILLING,
        labelRes = R.string.nav_billing,
        selectedIcon = Icons.Filled.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
        testTag = "nav_billing"
    ),
    NavDestinationItem(
        route = AppRoutes.ROUTE_GST_LAB,
        labelRes = R.string.nav_tax_lab,
        selectedIcon = Icons.Filled.Calculate,
        unselectedIcon = Icons.Outlined.Calculate,
        testTag = "nav_gst_lab"
    ),
    NavDestinationItem(
        route = AppRoutes.ROUTE_LEDGER,
        labelRes = R.string.nav_ledger,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        testTag = "nav_ledger"
    )
)

@Composable
fun StockLedgerApp(viewModel: ShopViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.ROUTE_DASHBOARD

    val storeProfile by viewModel.storeProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val searchQuery by viewModel.inventorySearch.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val inventoryFilter by viewModel.inventoryFilter.collectAsStateWithLifecycle()
    val billingState by viewModel.billingState.collectAsStateWithLifecycle()
    val gstLabState by viewModel.gstLabState.collectAsStateWithLifecycle()
    val selectedInvoiceItems by viewModel.selectedInvoiceItems.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddEditor by remember { mutableStateOf(false) }
    var restockingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showStoreProfileDialog by remember { mutableStateOf(false) }
    var selectedGstTab by remember { mutableStateOf(GstLabTab.CALCULATOR) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest.route) {
                                AppRoutes.ROUTE_BILLING -> billingState.summary.totalItems
                                AppRoutes.ROUTE_INVENTORY -> metrics.lowStockCount
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text("$badgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest.route) {
                                AppRoutes.ROUTE_BILLING -> billingState.summary.totalItems
                                AppRoutes.ROUTE_INVENTORY -> metrics.lowStockCount
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text("$badgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.ROUTE_DASHBOARD
                    ) {
                        composable(AppRoutes.ROUTE_DASHBOARD) {
                            DashboardScreen(
                                storeProfile = storeProfile,
                                metrics = metrics,
                                products = products,
                                invoices = invoices,
                                purchases = purchases,
                                onEditStoreProfile = { showStoreProfileDialog = true },
                                onAddProductClick = { showAddEditor = true },
                                onRestockProductClick = { restockingProduct = it },
                                onNavigateToQuickBill = { navigateTo(AppRoutes.ROUTE_BILLING) },
                                onNavigateToGstLab = {
                                    selectedGstTab = GstLabTab.CALCULATOR
                                    navigateTo(AppRoutes.ROUTE_GST_LAB)
                                },
                                onNavigateToLedger = { navigateTo(AppRoutes.ROUTE_LEDGER) },
                                onSimulateClearanceDiscount = { product ->
                                    viewModel.prefillDiscountSimulatorFromProduct(product, 15.0)
                                    selectedGstTab = GstLabTab.DISCOUNT_GUARD
                                    navigateTo(AppRoutes.ROUTE_GST_LAB)
                                }
                            )
                        }

                        composable(AppRoutes.ROUTE_INVENTORY) {
                            InventoryScreen(
                                products = products,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                inventoryFilter = inventoryFilter,
                                currencySymbol = storeProfile.currencySymbol,
                                onSearchChange = viewModel::setInventorySearch,
                                onCategoryChange = viewModel::setSelectedCategory,
                                onFilterChange = viewModel::setInventoryFilter,
                                onAddClick = { showAddEditor = true },
                                onEditClick = { editingProduct = it },
                                onRestockClick = { restockingProduct = it },
                                onAdjustStock = viewModel::adjustStock,
                                onDeleteProduct = viewModel::deleteProduct,
                                onAddToBill = { product ->
                                    viewModel.addToCart(product)
                                    navigateTo(AppRoutes.ROUTE_BILLING)
                                }
                            )
                        }

                        composable(AppRoutes.ROUTE_BILLING) {
                            QuickBillScreen(
                                products = products,
                                billingState = billingState,
                                storeProfile = storeProfile,
                                onSearchChange = viewModel::setBillingSearch,
                                onAddToCart = viewModel::addToCart,
                                onUpdateCartQty = viewModel::updateCartItemQuantity,
                                onCustomerNameChange = viewModel::setCustomerName,
                                onSupplyTypeChange = viewModel::setBillingSupplyType,
                                onBillDiscountChange = viewModel::setBillDiscountText,
                                onClearCart = viewModel::clearCart,
                                onCheckout = viewModel::checkoutCart,
                                onDismissReceipt = viewModel::dismissCompletedReceipt
                            )
                        }

                        composable(AppRoutes.ROUTE_GST_LAB) {
                            GstLabScreen(
                                selectedTab = selectedGstTab,
                                onTabChange = { selectedGstTab = it },
                                state = gstLabState,
                                onUpdateState = viewModel::updateGstLab,
                                storeProfile = storeProfile,
                                metrics = metrics,
                                invoices = invoices,
                                purchases = purchases,
                                products = products
                            )
                        }

                        composable(AppRoutes.ROUTE_LEDGER) {
                            LedgerScreen(
                                storeProfile = storeProfile,
                                invoices = invoices,
                                purchases = purchases,
                                products = products,
                                selectedInvoiceItems = selectedInvoiceItems,
                                onLoadInvoiceItems = viewModel::loadInvoiceItems
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddEditor || editingProduct != null) {
        ProductEditorDialog(
            existingProduct = editingProduct,
            currencySymbol = storeProfile.currencySymbol,
            onDismiss = {
                showAddEditor = false
                editingProduct = null
            },
            onSave = { product ->
                viewModel.upsertProduct(product)
                showAddEditor = false
                editingProduct = null
            }
        )
    }

    restockingProduct?.let { target ->
        RestockDialog(
            product = target,
            currencySymbol = storeProfile.currencySymbol,
            onDismiss = { restockingProduct = null },
            onConfirmRestock = { qty, supplier, unitCost, gstRate ->
                viewModel.recordRestock(target, qty, supplier, unitCost, gstRate)
                restockingProduct = null
            }
        )
    }

    if (showStoreProfileDialog) {
        StoreProfileDialog(
            currentProfile = storeProfile,
            onDismiss = { showStoreProfileDialog = false },
            onSave = { name, gstin, phone, address, currency, supplyType ->
                viewModel.updateStoreProfile(name, gstin, phone, address, currency, supplyType)
                showStoreProfileDialog = false
            }
        )
    }
}
