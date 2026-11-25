package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseLogEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.StoreProfile
import com.example.domain.GstCalculatorEngine
import com.example.domain.ReceiptFormatter
import com.example.ui.DashboardMetrics
import com.example.ui.components.LedgerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.sharePlainText
import com.example.ui.theme.MonoNumberLarge
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TaxBlue
import com.example.ui.theme.TaxBlueContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun DashboardScreen(
    storeProfile: StoreProfile,
    metrics: DashboardMetrics,
    products: List<ProductEntity>,
    invoices: List<SaleInvoiceEntity>,
    purchases: List<PurchaseLogEntity>,
    onEditStoreProfile: () -> Unit,
    onAddProductClick: () -> Unit,
    onRestockProductClick: (ProductEntity) -> Unit,
    onNavigateToQuickBill: () -> Unit,
    onNavigateToGstLab: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onSimulateClearanceDiscount: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val c = storeProfile.currencySymbol
    val now = System.currentTimeMillis()
    val thirtyDaysMs = 30L * 86_400_000L

    val lowStockItems = products.filter { it.stockQty <= it.reorderThreshold }
    val expiringItems = products.filter { p ->
        val exp = p.expiryEpochMillis ?: return@filter false
        exp <= now + thirtyDaysMs && p.stockQty > 0
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Store Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_store_banner),
                    contentDescription = "Shop counter header illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF062E22).copy(alpha = 0.72f),
                                    Color(0xFF062E22).copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.offline_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE6FFF4)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = storeProfile.shopName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (storeProfile.gstin.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${storeProfile.gstin}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD1FAE5)
                                )
                            } else {
                                Text(
                                    text = "${products.size} SKUs tracked • Ready for instant billing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                        }
                        IconButton(
                            onClick = onEditStoreProfile,
                            modifier = Modifier.testTag("edit_store_profile_button")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit store profile",
                                tint = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToQuickBill,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF281600)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_quick_bill_btn")
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Bill")
                        }
                        FilledTonalButton(
                            onClick = onAddProductClick,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dashboard_add_item_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Item")
                        }
                    }
                }
            }
        }

        // 2x2 KPI Bento Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiBentoCard(
                        title = "Today's Sales",
                        value = GstCalculatorEngine.formatMoney(metrics.todaySales, c),
                        subtitle = "${invoices.size} total bills",
                        icon = Icons.Default.PointOfSale,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToQuickBill,
                        modifier = Modifier.weight(1f)
                    )
                    KpiBentoCard(
                        title = "Today's Net Profit",
                        value = GstCalculatorEngine.formatMoney(metrics.todayProfit, c),
                        subtitle = "After cost & GST",
                        icon = Icons.Default.TrendingUp,
                        accentColor = ProfitGreen,
                        onClick = onNavigateToLedger,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiBentoCard(
                        title = "Inventory Cost",
                        value = GstCalculatorEngine.formatMoney(metrics.totalInventoryCostValue, c),
                        subtitle = "Retail: ${GstCalculatorEngine.formatMoney(metrics.totalInventoryRetailValue, c)}",
                        icon = Icons.Default.Inventory2,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = null,
                        modifier = Modifier.weight(1f)
                    )
                    KpiBentoCard(
                        title = "Net GST Payable",
                        value = GstCalculatorEngine.formatMoney(metrics.netGstLiability, c),
                        subtitle = "ITC Saved: ${GstCalculatorEngine.formatMoney(metrics.inputItcTotal, c)}",
                        icon = Icons.Default.AccountBalance,
                        accentColor = TaxBlue,
                        onClick = onNavigateToGstLab,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Tools Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToGstLab,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_gst_calc_btn")
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GST & Margin Lab", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = {
                        val report = ReceiptFormatter.formatTaxSummaryReport(
                            profile = storeProfile,
                            invoices = invoices,
                            purchases = purchases,
                            products = products
                        )
                        sharePlainText(context, "StockLedger Business Summary", report)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_export_report_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Summary", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Smart Low-Stock Radar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Low-Stock Radar", style = MaterialTheme.typography.titleMedium)
                }
                StatusPill(
                    text = if (lowStockItems.isEmpty()) "All Healthy" else "${lowStockItems.size} Need Restock",
                    containerColor = if (lowStockItems.isEmpty()) ProfitGreenContainer else WarningAmberContainer,
                    contentColor = if (lowStockItems.isEmpty()) ProfitGreen else WarningAmber
                )
            }
        }

        if (lowStockItems.isEmpty()) {
            item {
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Every product is above its reorder threshold.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(lowStockItems, key = { "low_${it.id}" }) { product ->
                        LedgerCard(
                            modifier = Modifier.width(240.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = "${product.stockQty} ${product.unit} left",
                                    containerColor = if (product.stockQty == 0) MaterialTheme.colorScheme.errorContainer else WarningAmberContainer,
                                    contentColor = if (product.stockQty == 0) MaterialTheme.colorScheme.error else WarningAmber
                                )
                                Text(
                                    text = "Min ${product.reorderThreshold}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Cost ${GstCalculatorEngine.formatMoney(product.costPrice, c)} • GST ${GstCalculatorEngine.formatCompactRate(product.gstRate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onRestockProductClick(product) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("Restock & Log ITC")
                            }
                        }
                    }
                }
            }
        }

        // Expiry & Clearance Watcher (Unique Shopkeeper Feature)
        if (expiringItems.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = TaxBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clearance & Expiry Watcher", style = MaterialTheme.typography.titleMedium)
                    }
                    StatusPill(
                        text = "${expiringItems.size} Expiring <30d",
                        containerColor = TaxBlueContainer,
                        contentColor = TaxBlue
                    )
                }
            }

            items(expiringItems, key = { "exp_${it.id}" }) { product ->
                val daysLeft = (((product.expiryEpochMillis ?: now) - now) / 86_400_000L).coerceAtLeast(0L)
                LedgerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${product.stockQty} ${product.unit} in batch • Expires in $daysLeft days",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningAmber
                            )
                        }
                        OutlinedButton(
                            onClick = { onSimulateClearanceDiscount(product) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Check Safe % Off")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiBentoCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    LedgerCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MonoNumberLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
