package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.StoreProfile
import com.example.domain.GstCalculatorEngine
import com.example.domain.SupplyType
import com.example.ui.BillingState
import com.example.ui.components.LedgerCard
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.MonoNumberLarge
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.MonoNumberSmall
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber

@Composable
fun QuickBillScreen(
    products: List<ProductEntity>,
    billingState: BillingState,
    storeProfile: StoreProfile,
    onSearchChange: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    onUpdateCartQty: (Long, Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onSupplyTypeChange: (SupplyType) -> Unit,
    onBillDiscountChange: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onDismissReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val c = storeProfile.currencySymbol
    val filteredCatalog = remember(products, billingState.searchQuery) {
        products.filter { p ->
            billingState.searchQuery.isBlank() ||
                p.name.contains(billingState.searchQuery, ignoreCase = true) ||
                p.sku.contains(billingState.searchQuery, ignoreCase = true)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CatalogPane(
                    filteredCatalog = filteredCatalog,
                    searchQuery = billingState.searchQuery,
                    currencySymbol = c,
                    onSearchChange = onSearchChange,
                    onAddToCart = onAddToCart,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                CartCheckoutPane(
                    billingState = billingState,
                    currencySymbol = c,
                    onUpdateCartQty = onUpdateCartQty,
                    onCustomerNameChange = onCustomerNameChange,
                    onSupplyTypeChange = onSupplyTypeChange,
                    onBillDiscountChange = onBillDiscountChange,
                    onClearCart = onClearCart,
                    onCheckout = onCheckout,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = billingState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Tap items to add to bill...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (billingState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("billing_search_input")
                )

                Text("Quick Tap Catalog", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredCatalog, key = { "pos_${it.id}" }) { product ->
                        CatalogTile(
                            product = product,
                            currencySymbol = c,
                            onAddToCart = { onAddToCart(product) },
                            modifier = Modifier.width(196.dp)
                        )
                    }
                }

                CartCheckoutContent(
                    billingState = billingState,
                    currencySymbol = c,
                    onUpdateCartQty = onUpdateCartQty,
                    onCustomerNameChange = onCustomerNameChange,
                    onSupplyTypeChange = onSupplyTypeChange,
                    onBillDiscountChange = onBillDiscountChange,
                    onClearCart = onClearCart,
                    onCheckout = onCheckout
                )
            }
        }
    }

    billingState.lastCompletedInvoice?.let { completed ->
        ReceiptPreviewDialog(
            context = context,
            profile = storeProfile,
            invoice = completed,
            items = billingState.lastCompletedItems,
            onDismiss = onDismissReceipt
        )
    }
}

@Composable
private fun CatalogPane(
    filteredCatalog: List<ProductEntity>,
    searchQuery: String,
    currencySymbol: String,
    onSearchChange: (String) -> Unit,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search catalog...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCatalog, key = { "wide_pos_${it.id}" }) { product ->
                CatalogTile(
                    product = product,
                    currencySymbol = currencySymbol,
                    onAddToCart = { onAddToCart(product) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CatalogTile(
    product: ProductEntity,
    currencySymbol: String,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerCard(
        modifier = modifier,
        onClick = if (product.stockQty > 0) onAddToCart else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                text = "${product.stockQty} ${product.unit}",
                containerColor = if (product.stockQty > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (product.stockQty > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
            )
            Text(
                text = "${GstCalculatorEngine.formatCompactRate(product.gstRate)} GST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = GstCalculatorEngine.formatMoney(product.sellingPrice, currencySymbol),
                style = MonoNumberMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.Add,
                contentDescription = "Add ${product.name}",
                tint = if (product.stockQty > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun CartCheckoutPane(
    billingState: BillingState,
    currencySymbol: String,
    onUpdateCartQty: (Long, Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onSupplyTypeChange: (SupplyType) -> Unit,
    onBillDiscountChange: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CartCheckoutContent(
            billingState = billingState,
            currencySymbol = currencySymbol,
            onUpdateCartQty = onUpdateCartQty,
            onCustomerNameChange = onCustomerNameChange,
            onSupplyTypeChange = onSupplyTypeChange,
            onBillDiscountChange = onBillDiscountChange,
            onClearCart = onClearCart,
            onCheckout = onCheckout
        )
    }
}

@Composable
private fun CartCheckoutContent(
    billingState: BillingState,
    currencySymbol: String,
    onUpdateCartQty: (Long, Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onSupplyTypeChange: (SupplyType) -> Unit,
    onBillDiscountChange: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    val summary = billingState.summary

    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Current Bill (${summary.totalItems} items)", style = MaterialTheme.typography.titleMedium)
            }
            if (billingState.cart.isNotEmpty()) {
                TextButton(onClick = onClearCart, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        if (billingState.cart.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap any item above to start billing. Stock auto-deducts and CGST/SGST or IGST splits automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            billingState.cart.forEach { cartItem ->
                val line = GstCalculatorEngine.calculateLineItem(
                    costPriceExclGst = cartItem.product.costPrice,
                    sellingPrice = cartItem.product.sellingPrice,
                    isSellingPriceTaxInclusive = cartItem.product.isSellingPriceTaxInclusive,
                    gstRate = cartItem.product.gstRate,
                    quantity = cartItem.quantity,
                    discountPercent = 0.0,
                    supplyType = billingState.supplyType
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cartItem.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${GstCalculatorEngine.formatMoney(line.unitPriceInclGst, currencySymbol)} • ${GstCalculatorEngine.formatCompactRate(cartItem.product.gstRate)} GST",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = { onUpdateCartQty(cartItem.product.id, -1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${cartItem.quantity}",
                            style = MonoNumberMedium,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        FilledTonalIconButton(
                            onClick = { onUpdateCartQty(cartItem.product.id, 1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = GstCalculatorEngine.formatMoney(line.lineTotal, currencySymbol),
                        style = MonoNumberMedium
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = billingState.customerName,
                    onValueChange = onCustomerNameChange,
                    label = { Text("Customer (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = billingState.billDiscountText,
                    onValueChange = { onBillDiscountChange(it.filter { ch -> ch.isDigit() || ch == '.' }) },
                    label = { Text("Disc %") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("bill_discount_input")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SupplyType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = billingState.supplyType == type,
                        onClick = { onSupplyTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = SupplyType.entries.size)
                    ) {
                        Text(if (type == SupplyType.INTRA_STATE) "Intra (CGST+SGST)" else "Inter (IGST)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tax & Grand Total Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taxable Value", style = MaterialTheme.typography.bodyMedium)
                    Text(GstCalculatorEngine.formatMoney(summary.taxableSubtotal, currencySymbol), style = MonoNumberSmall)
                }
                if (billingState.supplyType == SupplyType.INTRA_STATE) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(summary.cgstTotal, currencySymbol), style = MonoNumberSmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(summary.sgstTotal, currencySymbol), style = MonoNumberSmall)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(summary.igstTotal, currencySymbol), style = MonoNumberSmall)
                    }
                }
                if (summary.discountSavings > 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Customer Discount", style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                        Text("-${GstCalculatorEngine.formatMoney(summary.discountSavings, currencySymbol)}", style = MonoNumberSmall, color = WarningAmber)
                    }
                }
                if (summary.roundOff != 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Round Off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(GstCalculatorEngine.formatMoney(summary.roundOff, currencySymbol), style = MonoNumberSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Profit Guard Banner (Unique Shopkeeper Feature!)
            val isLoss = summary.netProfit < 0.0
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLoss) MaterialTheme.colorScheme.errorContainer else ProfitGreenContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLoss) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isLoss) MaterialTheme.colorScheme.error else ProfitGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoss) "Loss Alert! Discount exceeds cost" else "Live Bill Profit",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLoss) MaterialTheme.colorScheme.error else ProfitGreen
                        )
                    }
                    Text(
                        text = "${if (summary.netProfit >= 0) "+" else ""}${GstCalculatorEngine.formatMoney(summary.netProfit, currencySymbol)}",
                        style = MonoNumberMedium,
                        color = if (isLoss) MaterialTheme.colorScheme.error else ProfitGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Grand Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GstCalculatorEngine.formatMoney(summary.grandTotal, currencySymbol),
                        style = MonoNumberLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .heightIn(min = 52.dp)
                        .testTag("checkout_bill_button")
                ) {
                    Text("Complete & Print Bill")
                }
            }
        }
    }
}
