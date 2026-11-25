package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.domain.GstCalculatorEngine
import com.example.ui.InventoryFilter
import com.example.ui.components.LedgerCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.MonoNumberSmall
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TaxBlue
import com.example.ui.theme.TaxBlueContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun InventoryScreen(
    products: List<ProductEntity>,
    searchQuery: String,
    selectedCategory: String,
    inventoryFilter: InventoryFilter,
    currencySymbol: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onFilterChange: (InventoryFilter) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (ProductEntity) -> Unit,
    onRestockClick: (ProductEntity) -> Unit,
    onAdjustStock: (ProductEntity, Int) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onAddToBill: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct().sorted()
    }
    val now = System.currentTimeMillis()
    val thirtyDaysMs = 30L * 86_400_000L

    val filteredProducts = remember(products, searchQuery, selectedCategory, inventoryFilter) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true) ||
                p.hsnCode.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || p.category == selectedCategory
            val matchesFilter = when (inventoryFilter) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> p.stockQty <= p.reorderThreshold
                InventoryFilter.EXPIRING_SOON -> {
                    val exp = p.expiryEpochMillis
                    exp != null && exp <= now + thirtyDaysMs
                }
            }
            matchesQuery && matchesCategory && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_inventory")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add new product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search product, SKU, or HSN...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = inventoryFilter == InventoryFilter.ALL,
                    onClick = { onFilterChange(InventoryFilter.ALL) },
                    label = { Text("All (${products.size})") }
                )
                FilterChip(
                    selected = inventoryFilter == InventoryFilter.LOW_STOCK,
                    onClick = { onFilterChange(InventoryFilter.LOW_STOCK) },
                    label = { Text("Low Stock (${products.count { it.stockQty <= it.reorderThreshold }})") }
                )
                FilterChip(
                    selected = inventoryFilter == InventoryFilter.EXPIRING_SOON,
                    onClick = { onFilterChange(InventoryFilter.EXPIRING_SOON) },
                    label = { Text("Expiring <30d") }
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_ledger),
                        contentDescription = "Empty inventory shelves illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_inventory_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.empty_inventory_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddClick,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Product")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val margin = GstCalculatorEngine.calculateProductMarginPercent(
                            costPriceExclGst = product.costPrice,
                            sellingPrice = product.sellingPrice,
                            isSellingPriceTaxInclusive = product.isSellingPriceTaxInclusive,
                            gstRate = product.gstRate
                        )
                        val isLow = product.stockQty <= product.reorderThreshold

                        LedgerCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_card_${product.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append("${product.sku} • ${product.category}")
                                            if (product.hsnCode.isNotBlank()) append(" • HSN ${product.hsnCode}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = "${GstCalculatorEngine.formatCompactRate(product.gstRate)} GST",
                                    containerColor = TaxBlueContainer,
                                    contentColor = TaxBlue
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Sell: ${GstCalculatorEngine.formatMoney(product.sellingPrice, currencySymbol)} ${if (product.isSellingPriceTaxInclusive) "(Incl.)" else "(+GST)"}",
                                        style = MonoNumberMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Cost: ${GstCalculatorEngine.formatMoney(product.costPrice, currencySymbol)}",
                                        style = MonoNumberSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = "Margin ${if (margin >= 0) "+" else ""}$margin%",
                                    containerColor = if (margin >= 0) ProfitGreenContainer else MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (margin >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stock Stepper
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalIconButton(
                                        onClick = { onAdjustStock(product, -1) },
                                        enabled = product.stockQty > 0,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease stock")
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "${product.stockQty} ${product.unit}",
                                            style = MonoNumberMedium,
                                            color = if (isLow) WarningAmber else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (product.stockQty == 0) "OUT" else if (isLow) "LOW" else "IN STOCK",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (product.stockQty == 0) MaterialTheme.colorScheme.error else if (isLow) WarningAmber else ProfitGreen
                                        )
                                    }
                                    FilledTonalIconButton(
                                        onClick = { onAdjustStock(product, 1) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase stock")
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onEditClick(product) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit product")
                                    }
                                    IconButton(
                                        onClick = { pendingDelete = product },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete product")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onRestockClick(product) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restock + ITC")
                                }
                                FilledTonalButton(
                                    onClick = { onAddToBill(product) },
                                    enabled = product.stockQty > 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Bill")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${target.name}?") },
            text = { Text("This removes the product from your local inventory catalog. Past sales invoices remain untouched.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(target)
                        pendingDelete = null
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }
}
