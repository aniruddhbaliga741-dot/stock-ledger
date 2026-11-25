package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ProductEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleLineItemEntity
import com.example.data.local.StoreProfile
import com.example.domain.GstCalculatorEngine
import com.example.domain.ReceiptFormatter
import com.example.domain.SupplyType
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.MonoNumberSmall
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.WarningAmber

fun sharePlainText(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorDialog(
    existingProduct: ProductEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var sku by remember { mutableStateOf(existingProduct?.sku ?: "SKU-${(100..999).random()}") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "Groceries") }
    var hsnCode by remember { mutableStateOf(existingProduct?.hsnCode ?: "") }
    var unit by remember { mutableStateOf(existingProduct?.unit ?: "pcs") }
    var stockQtyText by remember { mutableStateOf(existingProduct?.stockQty?.toString() ?: "10") }
    var reorderText by remember { mutableStateOf(existingProduct?.reorderThreshold?.toString() ?: "5") }
    var costText by remember { mutableStateOf(existingProduct?.costPrice?.toString() ?: "") }
    var sellingText by remember { mutableStateOf(existingProduct?.sellingPrice?.toString() ?: "") }
    var isTaxInclusive by remember { mutableStateOf(existingProduct?.isSellingPriceTaxInclusive ?: true) }
    var gstRate by remember { mutableDoubleStateOf(existingProduct?.gstRate ?: 18.0) }
    var expiryDaysOption by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf(existingProduct?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val costVal = costText.toDoubleOrNull() ?: 0.0
    val sellVal = sellingText.toDoubleOrNull() ?: 0.0
    val marginPercent = GstCalculatorEngine.calculateProductMarginPercent(
        costPriceExclGst = costVal,
        sellingPrice = sellVal,
        isSellingPriceTaxInclusive = isTaxInclusive,
        gstRate = gstRate
    )

    val categories = listOf("Groceries", "Electronics", "Personal Care", "Beverages", "Stationery", "Apparel", "Hardware")
    val units = listOf("pcs", "kg", "L", "box", "pack", "bag", "btl")
    val gstSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingProduct == null) "Add Inventory Item" else "Edit Item",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_product_editor")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hsnCode,
                        onValueChange = { hsnCode = it },
                        label = { Text("HSN Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Unit", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    units.forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stockQtyText,
                        onValueChange = { stockQtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stock Qty *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                    OutlinedTextField(
                        value = reorderText,
                        onValueChange = { reorderText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Low Stock Alert") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it; errorMessage = null },
                        label = { Text("Cost Excl. GST ($currencySymbol) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_cost_input")
                    )
                    OutlinedTextField(
                        value = sellingText,
                        onValueChange = { sellingText = it; errorMessage = null },
                        label = { Text("Selling Price ($currencySymbol) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selling Price Includes GST", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isTaxInclusive) "MRP mode (tax extracted from price)" else "Base price mode (tax added at checkout)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isTaxInclusive,
                        onCheckedChange = { isTaxInclusive = it },
                        modifier = Modifier.testTag("product_tax_inclusive_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("GST Rate Slab", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    gstSlabs.forEach { slab ->
                        FilterChip(
                            selected = gstRate == slab,
                            onClick = { gstRate = slab },
                            label = { Text("${slab.toInt()}% GST") }
                        )
                    }
                }

                if (costVal > 0 && sellVal > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (marginPercent >= 0) ProfitGreenContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated Net Margin",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (marginPercent >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${if (marginPercent >= 0) "+" else ""}$marginPercent%",
                                style = MonoNumberMedium,
                                color = if (marginPercent >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Expiry Watcher (Optional)", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val expiryOptions = listOf(null to "Keep / None", 15 to "15 Days", 30 to "30 Days", 90 to "90 Days", 180 to "6 Months", 365 to "1 Year")
                    expiryOptions.forEach { (days, label) ->
                        FilterChip(
                            selected = expiryDaysOption == days,
                            onClick = { expiryDaysOption = days },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Shelf / Batch Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsedCost = costText.toDoubleOrNull()
                            val parsedSell = sellingText.toDoubleOrNull()
                            val parsedQty = stockQtyText.toIntOrNull() ?: 0
                            val parsedReorder = reorderText.toIntOrNull() ?: 5
                            if (name.isBlank() || parsedCost == null || parsedSell == null) {
                                errorMessage = "Please enter product name, cost price, and selling price."
                                return@Button
                            }
                            val resolvedExpiry = when {
                                expiryDaysOption != null -> System.currentTimeMillis() + expiryDaysOption!! * 86_400_000L
                                else -> existingProduct?.expiryEpochMillis
                            }
                            onSave(
                                ProductEntity(
                                    id = existingProduct?.id ?: 0L,
                                    name = name.trim(),
                                    sku = sku.trim().ifEmpty { "SKU-${(100..999).random()}" },
                                    category = category,
                                    hsnCode = hsnCode.trim(),
                                    unit = unit,
                                    stockQty = parsedQty,
                                    reorderThreshold = parsedReorder,
                                    costPrice = parsedCost,
                                    sellingPrice = parsedSell,
                                    isSellingPriceTaxInclusive = isTaxInclusive,
                                    gstRate = gstRate,
                                    expiryEpochMillis = resolvedExpiry,
                                    notes = notes.trim()
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_product_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Item")
                    }
                }
            }
        }
    }
}

@Composable
fun RestockDialog(
    product: ProductEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmRestock: (qty: Int, supplier: String, unitCostExcl: Double, gstRate: Double) -> Unit
) {
    var qtyText by remember { mutableStateOf("10") }
    var supplierText by remember { mutableStateOf("Wholesale Distributor") }
    var unitCostText by remember { mutableStateOf(product.costPrice.toString()) }
    var gstRate by remember { mutableDoubleStateOf(product.gstRate) }

    val qty = qtyText.toIntOrNull() ?: 0
    val unitCost = unitCostText.toDoubleOrNull() ?: 0.0
    val base = GstCalculatorEngine.round2(qty * unitCost)
    val itc = GstCalculatorEngine.round2(base * (gstRate / 100.0))
    val total = GstCalculatorEngine.round2(base + itc)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restock ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Logs Input Tax Credit (ITC) automatically so your net GST payable stays accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity to Add (${product.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitCostText,
                    onValueChange = { unitCostText = it },
                    label = { Text("Purchase Cost / Unit Excl. GST ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = supplierText,
                    onValueChange = { supplierText = it },
                    label = { Text("Supplier Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxable Purchase", style = MaterialTheme.typography.bodySmall)
                            Text(GstCalculatorEngine.formatMoney(base, currencySymbol), style = MonoNumberSmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Claimable ITC (${gstRate.toInt()}%)", style = MaterialTheme.typography.bodySmall)
                            Text(GstCalculatorEngine.formatMoney(itc, currencySymbol), style = MonoNumberSmall, color = ProfitGreen)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Supplier Bill", style = MaterialTheme.typography.labelLarge)
                            Text(GstCalculatorEngine.formatMoney(total, currencySymbol), style = MonoNumberMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (qty > 0) {
                        onConfirmRestock(qty, supplierText, unitCost, gstRate)
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Confirm Restock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StoreProfileDialog(
    currentProfile: StoreProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, gstin: String, phone: String, address: String, currency: String, supplyType: SupplyType) -> Unit
) {
    var name by remember { mutableStateOf(currentProfile.shopName) }
    var gstin by remember { mutableStateOf(currentProfile.gstin) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var address by remember { mutableStateOf(currentProfile.address) }
    var currency by remember { mutableStateOf(currentProfile.currencySymbol) }
    var supplyType by remember { mutableStateOf(currentProfile.defaultSupplyType) }

    val currencies = listOf("₹", "$", "€", "£")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Store Profile (Local Only)", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Text(
                    text = "Appears on shared GST invoices. Stored strictly offline on your device — no account required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shop / Business Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("store_name_input")
                )
                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("GSTIN (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Shop Phone (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Shop Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Currency Symbol", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencies.forEach { sym ->
                        FilterChip(
                            selected = currency == sym,
                            onClick = { currency = sym },
                            label = { Text(sym, style = MonoNumberMedium) }
                        )
                    }
                }
                Text("Default Billing Tax Mode", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SupplyType.entries.forEachIndexed { idx, type ->
                        SegmentedButton(
                            selected = supplyType == type,
                            onClick = { supplyType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = SupplyType.entries.size)
                        ) {
                            Text(if (type == SupplyType.INTRA_STATE) "CGST + SGST" else "IGST")
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(name, gstin, phone, address, currency, supplyType) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_store_profile_button")
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptPreviewDialog(
    context: Context,
    profile: StoreProfile,
    invoice: SaleInvoiceEntity,
    items: List<SaleLineItemEntity>,
    onDismiss: () -> Unit
) {
    val c = profile.currencySymbol
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleMedium)
                            Text(invoice.customerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close receipt")
                    }
                }

                HorizontalDivider()

                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.quantity} × ${GstCalculatorEngine.formatMoney(item.unitPriceInclGst, c)} • GST ${GstCalculatorEngine.formatCompactRate(item.gstRate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(GstCalculatorEngine.formatMoney(item.lineTotal, c), style = MonoNumberMedium)
                    }
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taxable Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(GstCalculatorEngine.formatMoney(invoice.subtotalExclGst, c), style = MonoNumberSmall)
                }
                if (invoice.cgstTotal > 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(invoice.cgstTotal, c), style = MonoNumberSmall)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(invoice.sgstTotal, c), style = MonoNumberSmall)
                    }
                }
                if (invoice.igstTotal > 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IGST", style = MaterialTheme.typography.bodyMedium)
                        Text(GstCalculatorEngine.formatMoney(invoice.igstTotal, c), style = MonoNumberSmall)
                    }
                }
                if (invoice.discountAmount > 0.0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Discount Saved", style = MaterialTheme.typography.bodyMedium, color = WarningAmber)
                        Text("-${GstCalculatorEngine.formatMoney(invoice.discountAmount, c)}", style = MonoNumberSmall, color = WarningAmber)
                    }
                }

                HorizontalDivider()

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Grand Total", style = MaterialTheme.typography.titleMedium)
                    Text(
                        GstCalculatorEngine.formatMoney(invoice.grandTotal, c),
                        style = MonoNumberMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Shopkeeper Net Profit", style = MaterialTheme.typography.labelMedium, color = ProfitGreen)
                    Text(
                        "+${GstCalculatorEngine.formatMoney(invoice.totalProfit, c)}",
                        style = MonoNumberSmall,
                        color = ProfitGreen
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("Done")
                    }
                    Button(
                        onClick = {
                            val text = ReceiptFormatter.formatInvoiceReceipt(profile, invoice, items)
                            sharePlainText(context, "Tax Invoice ${invoice.invoiceNumber}", text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("share_receipt_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Bill")
                    }
                }
            }
        }
    }
}
