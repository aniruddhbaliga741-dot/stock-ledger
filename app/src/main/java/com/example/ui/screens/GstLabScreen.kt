package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseLogEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.StoreProfile
import com.example.domain.GstCalculatorEngine
import com.example.domain.GstMode
import com.example.domain.ReceiptFormatter
import com.example.domain.SupplyType
import com.example.ui.DashboardMetrics
import com.example.ui.GstLabState
import com.example.ui.components.LedgerCard
import com.example.ui.components.StatusPill
import com.example.ui.components.sharePlainText
import com.example.ui.theme.MonoNumberLarge
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.MonoNumberSmall
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TaxBlue
import com.example.ui.theme.TaxBlueContainer
import com.example.ui.theme.WarningAmber

enum class GstLabTab(val title: String) {
    CALCULATOR("GST Calc"),
    MRP_ADVISOR("MRP & ITC"),
    DISCOUNT_GUARD("Discount Guard"),
    TAX_SUMMARY("Tax Return")
}

@Composable
fun GstLabScreen(
    selectedTab: GstLabTab,
    onTabChange: (GstLabTab) -> Unit,
    state: GstLabState,
    onUpdateState: ((GstLabState) -> GstLabState) -> Unit,
    storeProfile: StoreProfile,
    metrics: DashboardMetrics,
    invoices: List<SaleInvoiceEntity>,
    purchases: List<PurchaseLogEntity>,
    products: List<ProductEntity>,
    modifier: Modifier = Modifier
) {
    val c = storeProfile.currencySymbol
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GstLabTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        GstLabTab.CALCULATOR -> Icons.Default.Calculate
                        GstLabTab.MRP_ADVISOR -> Icons.Default.PriceCheck
                        GstLabTab.DISCOUNT_GUARD -> Icons.Default.LocalOffer
                        GstLabTab.TAX_SUMMARY -> Icons.Default.AccountBalance
                    }
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { onTabChange(tab) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("gst_tab_${tab.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                GstLabTab.CALCULATOR -> GstDualCalculatorSection(
                    state = state,
                    currencySymbol = c,
                    onUpdateState = onUpdateState,
                    onShare = { text -> sharePlainText(context, "GST Calculation", text) }
                )
                GstLabTab.MRP_ADVISOR -> MrpItcAdvisorSection(
                    state = state,
                    currencySymbol = c,
                    onUpdateState = onUpdateState
                )
                GstLabTab.DISCOUNT_GUARD -> DiscountBreakevenSection(
                    state = state,
                    currencySymbol = c,
                    onUpdateState = onUpdateState
                )
                GstLabTab.TAX_SUMMARY -> TaxReturnSummarySection(
                    storeProfile = storeProfile,
                    metrics = metrics,
                    invoices = invoices,
                    purchases = purchases,
                    onExportReport = {
                        val report = ReceiptFormatter.formatTaxSummaryReport(
                            profile = storeProfile,
                            invoices = invoices,
                            purchases = purchases,
                            products = products
                        )
                        sharePlainText(context, "StockLedger GST Return Summary", report)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GstDualCalculatorSection(
    state: GstLabState,
    currencySymbol: String,
    onUpdateState: ((GstLabState) -> GstLabState) -> Unit,
    onShare: (String) -> Unit
) {
    val breakdown = state.gstBreakdown

    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Text("Dual GST Calculator", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Instant Inclusive & Exclusive GST split with Cess and CGST/SGST/IGST breakdown.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GstMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = state.calcMode == mode,
                    onClick = { onUpdateState { it.copy(calcMode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = GstMode.entries.size),
                    modifier = Modifier.testTag("gst_mode_${mode.name.lowercase()}")
                ) {
                    Text(if (mode == GstMode.EXCLUSIVE) "Add GST (+)" else "Remove GST (-)")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.calcAmountText,
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() || it == '.' }
                onUpdateState { it.copy(calcAmountText = clean) }
            },
            label = {
                Text(
                    if (state.calcMode == GstMode.EXCLUSIVE) "Net Base Amount ($currencySymbol)"
                    else "MRP / Gross Amount ($currencySymbol)"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gst_amount_input")
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text("GST Slab", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GstCalculatorEngine.STANDARD_SLABS.forEach { slab ->
                val isSelected = state.calcCustomRateText.isEmpty() && state.calcGstRate == slab
                FilterChip(
                    selected = isSelected,
                    onClick = { onUpdateState { it.copy(calcGstRate = slab, calcCustomRateText = "") } },
                    label = { Text(GstCalculatorEngine.formatCompactRate(slab)) },
                    modifier = Modifier.testTag("gst_slab_${slab}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.calcCustomRateText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    onUpdateState { it.copy(calcCustomRateText = clean) }
                },
                label = { Text("Custom GST %") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.calcCessText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    onUpdateState { it.copy(calcCessText = clean) }
                },
                label = { Text("Cess % (Opt)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SupplyType.entries.forEachIndexed { idx, type ->
                SegmentedButton(
                    selected = state.calcSupplyType == type,
                    onClick = { onUpdateState { it.copy(calcSupplyType = type) } },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = SupplyType.entries.size)
                ) {
                    Text(if (type == SupplyType.INTRA_STATE) "Intra (CGST+SGST)" else "Inter (IGST)")
                }
            }
        }
    }

    // Breakdown Result Card
    LedgerCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Taxable Base Value", style = MaterialTheme.typography.bodyMedium)
            Text(
                GstCalculatorEngine.formatMoney(breakdown.baseAmount, currencySymbol),
                style = MonoNumberMedium,
                modifier = Modifier.testTag("gst_result_base")
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (breakdown.supplyType == SupplyType.INTRA_STATE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent / 2)})", style = MaterialTheme.typography.bodyMedium)
                Text(GstCalculatorEngine.formatMoney(breakdown.cgstAmount, currencySymbol), style = MonoNumberSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent / 2)})", style = MaterialTheme.typography.bodyMedium)
                Text(GstCalculatorEngine.formatMoney(breakdown.sgstAmount, currencySymbol), style = MonoNumberSmall)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent)})", style = MaterialTheme.typography.bodyMedium)
                Text(GstCalculatorEngine.formatMoney(breakdown.igstAmount, currencySymbol), style = MonoNumberSmall)
            }
        }
        if (breakdown.cessAmount > 0.0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cess (${GstCalculatorEngine.formatCompactRate(breakdown.cessPercent)})", style = MaterialTheme.typography.bodyMedium)
                Text(GstCalculatorEngine.formatMoney(breakdown.cessAmount, currencySymbol), style = MonoNumberSmall)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Tax", style = MaterialTheme.typography.labelLarge, color = TaxBlue)
            Text(
                GstCalculatorEngine.formatMoney(breakdown.totalTax, currencySymbol),
                style = MonoNumberMedium,
                color = TaxBlue,
                modifier = Modifier.testTag("gst_result_tax")
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Final Amount", style = MaterialTheme.typography.titleMedium)
            Text(
                GstCalculatorEngine.formatMoney(breakdown.grandTotal, currencySymbol),
                style = MonoNumberLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("gst_result_total")
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onShare(ReceiptFormatter.formatGstCalculation(breakdown, currencySymbol)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Tax Breakdown")
        }
    }
}

@Composable
private fun MrpItcAdvisorSection(
    state: GstLabState,
    currencySymbol: String,
    onUpdateState: ((GstLabState) -> GstLabState) -> Unit
) {
    val advice = state.pricingAdvice
    val slabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Text("Reverse Margin & ITC Pricing Advisor", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Know exactly what MRP to tag your goods after Input Tax Credit (ITC) and your target profit margin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.advisorPurchaseCostText,
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() || it == '.' }
                onUpdateState { it.copy(advisorPurchaseCostText = clean) }
            },
            label = { Text("Wholesale Purchase Price ($currencySymbol)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Supplier bill includes GST", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = state.advisorPurchaseIncludesGst,
                onCheckedChange = { checked -> onUpdateState { it.copy(advisorPurchaseIncludesGst = checked) } }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Purchase GST % (ITC)", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slabs.forEach { s ->
                FilterChip(
                    selected = state.advisorPurchaseGstRate == s,
                    onClick = { onUpdateState { it.copy(advisorPurchaseGstRate = s) } },
                    label = { Text("${s.toInt()}%") }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.advisorDesiredMarginText,
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() || it == '.' }
                onUpdateState { it.copy(advisorDesiredMarginText = clean) }
            },
            label = { Text("Desired Net Profit Margin %") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Selling GST Slab", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slabs.forEach { s ->
                FilterChip(
                    selected = state.advisorSellingGstRate == s,
                    onClick = { onUpdateState { it.copy(advisorSellingGstRate = s) } },
                    label = { Text("${s.toInt()}%") }
                )
            }
        }
    }

    LedgerCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Net Cost (Excl. ITC)", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(advice.netCostExclGst, currencySymbol), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Claimable Input Credit (ITC)", style = MaterialTheme.typography.bodyMedium, color = ProfitGreen)
            Text(GstCalculatorEngine.formatMoney(advice.inputTaxCredit, currencySymbol), style = MonoNumberSmall, color = ProfitGreen)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recommended Base Price", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(advice.recommendedSellingExclGst, currencySymbol), style = MonoNumberMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Output GST on Sale", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(advice.outputGst, currencySymbol), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Net GST Payable to Govt", style = MaterialTheme.typography.labelLarge, color = TaxBlue)
            Text(GstCalculatorEngine.formatMoney(advice.netGstPayableToGovt, currencySymbol), style = MonoNumberMedium, color = TaxBlue)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tag Customer MRP At", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Net Profit: +${GstCalculatorEngine.formatMoney(advice.expectedProfit, currencySymbol)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ProfitGreen
                )
            }
            Text(
                GstCalculatorEngine.formatMoney(advice.recommendedMrpInclGst, currencySymbol),
                style = MonoNumberLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DiscountBreakevenSection(
    state: GstLabState,
    currencySymbol: String,
    onUpdateState: ((GstLabState) -> GstLabState) -> Unit
) {
    val impact = state.discountImpact
    val slabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Text("Discount Breakeven Guard", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Before agreeing to a customer discount or running clearance, verify your exact remaining profit & max safe discount %.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.discountCostText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    onUpdateState { it.copy(discountCostText = clean) }
                },
                label = { Text("Cost Excl. GST ($currencySymbol)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.discountMrpText,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() || it == '.' }
                    onUpdateState { it.copy(discountMrpText = clean) }
                },
                label = { Text("MRP Incl. GST ($currencySymbol)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.discountPercentText,
            onValueChange = { raw ->
                val clean = raw.filter { it.isDigit() || it == '.' }
                onUpdateState { it.copy(discountPercentText = clean) }
            },
            label = { Text("Offered Discount %") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Item GST Slab", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slabs.forEach { s ->
                FilterChip(
                    selected = state.discountGstRate == s,
                    onClick = { onUpdateState { it.copy(discountGstRate = s) } },
                    label = { Text("${s.toInt()}%") }
                )
            }
        }
    }

    LedgerCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (impact.isLossMaking) MaterialTheme.colorScheme.errorContainer else ProfitGreenContainer
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (impact.isLossMaking) "LOSS ALERT!" else "SAFE DISCOUNT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (impact.isLossMaking) MaterialTheme.colorScheme.error else ProfitGreen
            )
            StatusPill(
                text = "Max Safe: ${impact.maxBreakevenDiscountPercent}%",
                containerColor = TaxBlueContainer,
                contentColor = TaxBlue
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Customer Pays", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(impact.discountedMrp, currencySymbol), style = MonoNumberMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Taxable Value (Excl. GST)", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(impact.taxableValue, currencySymbol), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Output GST", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(impact.outputGst, currencySymbol), style = MonoNumberSmall)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Profit Left After Discount", style = MaterialTheme.typography.titleSmall)
            Text(
                "${if (impact.profitLeft >= 0) "+" else ""}${GstCalculatorEngine.formatMoney(impact.profitLeft, currencySymbol)} (${impact.newMarginPercent}%)",
                style = MonoNumberMedium,
                color = if (impact.isLossMaking) MaterialTheme.colorScheme.error else ProfitGreen
            )
        }
    }
}

@Composable
private fun TaxReturnSummarySection(
    storeProfile: StoreProfile,
    metrics: DashboardMetrics,
    invoices: List<SaleInvoiceEntity>,
    purchases: List<PurchaseLogEntity>,
    onExportReport: () -> Unit
) {
    val c = storeProfile.currencySymbol
    val cgst = invoices.sumOf { it.cgstTotal }
    val sgst = invoices.sumOf { it.sgstTotal }
    val igst = invoices.sumOf { it.igstTotal }

    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Text("GST Liability & Input Credit Ledger", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Aggregated automatically from your local Quick Bills (Output GST) and Restock logs (Input ITC).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Invoices Recorded", style = MaterialTheme.typography.bodyMedium)
            Text("${invoices.size}", style = MonoNumberMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Output CGST Collected", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(cgst, c), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Output SGST Collected", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(sgst, c), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Output IGST Collected", style = MaterialTheme.typography.bodyMedium)
            Text(GstCalculatorEngine.formatMoney(igst, c), style = MonoNumberSmall)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Output Tax", style = MaterialTheme.typography.labelLarge)
            Text(GstCalculatorEngine.formatMoney(metrics.outputGstTotal, c), style = MonoNumberMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Less: Input Tax Credit (${purchases.size} restocks)", style = MaterialTheme.typography.bodyMedium, color = ProfitGreen)
            Text("-${GstCalculatorEngine.formatMoney(metrics.inputItcTotal, c)}", style = MonoNumberMedium, color = ProfitGreen)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = TaxBlueContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Estimated Net GST Payable", style = MaterialTheme.typography.titleSmall, color = TaxBlue)
                    Text("Output Tax minus Input ITC", style = MaterialTheme.typography.bodySmall, color = TaxBlue)
                }
                Text(
                    GstCalculatorEngine.formatMoney(metrics.netGstLiability, c),
                    style = MonoNumberLarge,
                    color = TaxBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onExportReport,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Export Business & GST Summary")
        }
    }
}
