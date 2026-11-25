package com.example.domain

import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseLogEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleLineItemEntity
import com.example.data.local.StoreProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {

    private val dateTimeFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun formatInvoiceReceipt(
        profile: StoreProfile,
        invoice: SaleInvoiceEntity,
        items: List<SaleLineItemEntity>
    ): String {
        val c = profile.currencySymbol
        val sb = StringBuilder()
        sb.appendLine("=================================")
        sb.appendLine(profile.shopName.uppercase())
        if (profile.gstin.isNotBlank()) sb.appendLine("GSTIN: ${profile.gstin}")
        if (profile.phone.isNotBlank()) sb.appendLine("Tel: ${profile.phone}")
        if (profile.address.isNotBlank()) sb.appendLine(profile.address)
        sb.appendLine("---------------------------------")
        sb.appendLine("TAX INVOICE: ${invoice.invoiceNumber}")
        sb.appendLine("Date: ${dateTimeFmt.format(Date(invoice.createdAt))}")
        sb.appendLine("Customer: ${invoice.customerName}")
        sb.appendLine("Supply: ${if (invoice.supplyType == SupplyType.INTRA_STATE.name) "Intra-State" else "Inter-State"}")
        sb.appendLine("---------------------------------")
        items.forEachIndexed { index, item ->
            sb.appendLine("${index + 1}. ${item.productName}")
            val hsn = if (item.hsnCode.isNotBlank()) " [HSN ${item.hsnCode}]" else ""
            sb.appendLine("   ${item.quantity} × ${GstCalculatorEngine.formatMoney(item.unitPriceInclGst, c)} (GST ${GstCalculatorEngine.formatCompactRate(item.gstRate)})$hsn = ${GstCalculatorEngine.formatMoney(item.lineTotal, c)}")
        }
        sb.appendLine("---------------------------------")
        sb.appendLine("Taxable Value : ${GstCalculatorEngine.formatMoney(invoice.subtotalExclGst, c)}")
        if (invoice.discountAmount > 0.0) {
            sb.appendLine("Discount      : -${GstCalculatorEngine.formatMoney(invoice.discountAmount, c)}")
        }
        if (invoice.cgstTotal > 0.0) {
            sb.appendLine("CGST          : ${GstCalculatorEngine.formatMoney(invoice.cgstTotal, c)}")
            sb.appendLine("SGST          : ${GstCalculatorEngine.formatMoney(invoice.sgstTotal, c)}")
        }
        if (invoice.igstTotal > 0.0) {
            sb.appendLine("IGST          : ${GstCalculatorEngine.formatMoney(invoice.igstTotal, c)}")
        }
        if (invoice.roundOff != 0.0) {
            val sign = if (invoice.roundOff > 0) "+" else ""
            sb.appendLine("Round Off     : $sign${GstCalculatorEngine.formatMoney(invoice.roundOff, c)}")
        }
        sb.appendLine("=================================")
        sb.appendLine("GRAND TOTAL   : ${GstCalculatorEngine.formatMoney(invoice.grandTotal, c)}")
        sb.appendLine("=================================")
        sb.appendLine("Thank you for shopping with us!")
        return sb.toString()
    }

    fun formatGstCalculation(
        breakdown: GstBreakdown,
        currencySymbol: String
    ): String {
        return buildString {
            appendLine("GST Tax Breakdown (${if (breakdown.mode == GstMode.INCLUSIVE) "Tax Inclusive" else "Tax Exclusive"})")
            appendLine("Taxable Amount : ${GstCalculatorEngine.formatMoney(breakdown.baseAmount, currencySymbol)}")
            if (breakdown.supplyType == SupplyType.INTRA_STATE) {
                appendLine("CGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent / 2)}) : ${GstCalculatorEngine.formatMoney(breakdown.cgstAmount, currencySymbol)}")
                appendLine("SGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent / 2)}) : ${GstCalculatorEngine.formatMoney(breakdown.sgstAmount, currencySymbol)}")
            } else {
                appendLine("IGST (${GstCalculatorEngine.formatCompactRate(breakdown.gstRatePercent)}) : ${GstCalculatorEngine.formatMoney(breakdown.igstAmount, currencySymbol)}")
            }
            if (breakdown.cessAmount > 0.0) {
                appendLine("Cess (${GstCalculatorEngine.formatCompactRate(breakdown.cessPercent)}) : ${GstCalculatorEngine.formatMoney(breakdown.cessAmount, currencySymbol)}")
            }
            appendLine("Total Tax      : ${GstCalculatorEngine.formatMoney(breakdown.totalTax, currencySymbol)}")
            appendLine("Grand Total    : ${GstCalculatorEngine.formatMoney(breakdown.grandTotal, currencySymbol)}")
        }
    }

    fun formatTaxSummaryReport(
        profile: StoreProfile,
        invoices: List<SaleInvoiceEntity>,
        purchases: List<PurchaseLogEntity>,
        products: List<ProductEntity>
    ): String {
        val c = profile.currencySymbol
        val totalSales = invoices.sumOf { it.grandTotal }
        val totalTaxableSales = invoices.sumOf { it.subtotalExclGst }
        val totalCgst = invoices.sumOf { it.cgstTotal }
        val totalSgst = invoices.sumOf { it.sgstTotal }
        val totalIgst = invoices.sumOf { it.igstTotal }
        val totalOutputGst = totalCgst + totalSgst + totalIgst
        val totalInputItc = purchases.sumOf { it.inputTaxCredit }
        val netPayable = (totalOutputGst - totalInputItc).coerceAtLeast(0.0)
        val totalProfit = invoices.sumOf { it.totalProfit }
        val stockValuation = products.sumOf { it.costPrice * it.stockQty }

        return buildString {
            appendLine("STOCKLEDGER BUSINESS & GST SUMMARY")
            appendLine("Store: ${profile.shopName}")
            if (profile.gstin.isNotBlank()) appendLine("GSTIN: ${profile.gstin}")
            appendLine("Generated: ${dateTimeFmt.format(Date())}")
            appendLine("---------------------------------")
            appendLine("Invoices Recorded : ${invoices.size}")
            appendLine("Gross Sales       : ${GstCalculatorEngine.formatMoney(totalSales, c)}")
            appendLine("Taxable Turnover  : ${GstCalculatorEngine.formatMoney(totalTaxableSales, c)}")
            appendLine("Net Sales Profit  : ${GstCalculatorEngine.formatMoney(totalProfit, c)}")
            appendLine("---------------------------------")
            appendLine("OUTPUT GST COLLECTED")
            appendLine("  CGST            : ${GstCalculatorEngine.formatMoney(totalCgst, c)}")
            appendLine("  SGST            : ${GstCalculatorEngine.formatMoney(totalSgst, c)}")
            appendLine("  IGST            : ${GstCalculatorEngine.formatMoney(totalIgst, c)}")
            appendLine("  Total Output    : ${GstCalculatorEngine.formatMoney(totalOutputGst, c)}")
            appendLine("INPUT TAX CREDIT (Purchases: ${purchases.size})")
            appendLine("  Claimable ITC   : ${GstCalculatorEngine.formatMoney(totalInputItc, c)}")
            appendLine("NET GST PAYABLE   : ${GstCalculatorEngine.formatMoney(netPayable, c)}")
            appendLine("---------------------------------")
            appendLine("Current Stock SKUs: ${products.size}")
            appendLine("Stock Cost Value  : ${GstCalculatorEngine.formatMoney(stockValuation, c)}")
        }
    }
}
