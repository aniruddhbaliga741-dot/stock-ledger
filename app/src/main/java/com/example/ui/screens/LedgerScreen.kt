package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseLogEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleLineItemEntity
import com.example.data.local.StoreProfile
import com.example.domain.GstCalculatorEngine
import com.example.domain.ReceiptFormatter
import com.example.domain.SupplyType
import com.example.ui.components.LedgerCard
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.sharePlainText
import com.example.ui.theme.MonoNumberMedium
import com.example.ui.theme.MonoNumberSmall
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.TaxBlue
import com.example.ui.theme.TaxBlueContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen(
    storeProfile: StoreProfile,
    invoices: List<SaleInvoiceEntity>,
    purchases: List<PurchaseLogEntity>,
    products: List<ProductEntity>,
    selectedInvoiceItems: List<SaleLineItemEntity>,
    onLoadInvoiceItems: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val c = storeProfile.currencySymbol
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeInvoice by remember { mutableStateOf<SaleInvoiceEntity?>(null) }
    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("Sales Bills (${invoices.size})", "Restock & ITC (${purchases.size})").forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            if (invoices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_ledger),
                        contentDescription = "Empty ledger illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.empty_ledger_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.empty_ledger_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val totalRevenue = invoices.sumOf { it.grandTotal }
                val totalProfit = invoices.sumOf { it.totalProfit }

                LedgerCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("All-Time Revenue", style = MaterialTheme.typography.labelMedium)
                            Text(GstCalculatorEngine.formatMoney(totalRevenue, c), style = MonoNumberMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Net Profit Earned", style = MaterialTheme.typography.labelMedium)
                            Text(
                                "+${GstCalculatorEngine.formatMoney(totalProfit, c)}",
                                style = MonoNumberMedium,
                                color = ProfitGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val report = ReceiptFormatter.formatTaxSummaryReport(storeProfile, invoices, purchases, products)
                            sharePlainText(context, "StockLedger Sales & GST Ledger", report)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Full Sales & GST Report")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(invoices, key = { it.id }) { inv ->
                        val gstTotal = inv.cgstTotal + inv.sgstTotal + inv.igstTotal
                        LedgerCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onLoadInvoiceItems(inv.id)
                                activeInvoice = inv
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(inv.invoiceNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${inv.customerName} • ${dateFmt.format(Date(inv.createdAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    GstCalculatorEngine.formatMoney(inv.grandTotal, c),
                                    style = MonoNumberMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                inv.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusPill(
                                    text = if (inv.supplyType == SupplyType.INTRA_STATE.name) {
                                        "CGST+SGST: ${GstCalculatorEngine.formatMoney(gstTotal, c)}"
                                    } else {
                                        "IGST: ${GstCalculatorEngine.formatMoney(gstTotal, c)}"
                                    },
                                    containerColor = TaxBlueContainer,
                                    contentColor = TaxBlue
                                )
                                StatusPill(
                                    text = "Profit +${GstCalculatorEngine.formatMoney(inv.totalProfit, c)}",
                                    containerColor = ProfitGreenContainer,
                                    contentColor = ProfitGreen
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (purchases.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No Restock Purchases Logged", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Use \"Restock + ITC\" on any inventory item to track wholesale supplier bills and claimable Input Tax Credit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(purchases, key = { it.id }) { pur ->
                        LedgerCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pur.productName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${pur.supplierName} • ${dateFmt.format(Date(pur.createdAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(GstCalculatorEngine.formatMoney(pur.totalPaid, c), style = MonoNumberMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "+${pur.quantityAdded} units @ ${GstCalculatorEngine.formatMoney(pur.unitCostExclGst, c)}",
                                    style = MonoNumberSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                StatusPill(
                                    text = "ITC +${GstCalculatorEngine.formatMoney(pur.inputTaxCredit, c)}",
                                    containerColor = ProfitGreenContainer,
                                    contentColor = ProfitGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeInvoice?.let { inv ->
        ReceiptPreviewDialog(
            context = context,
            profile = storeProfile,
            invoice = inv,
            items = selectedInvoiceItems,
            onDismiss = { activeInvoice = null }
        )
    }
}
