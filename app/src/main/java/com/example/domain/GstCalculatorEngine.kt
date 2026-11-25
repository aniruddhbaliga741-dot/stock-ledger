package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

enum class GstMode {
    EXCLUSIVE,
    INCLUSIVE
}

enum class SupplyType(val label: String) {
    INTRA_STATE("Intra-State (CGST + SGST)"),
    INTER_STATE("Inter-State (IGST)")
}

data class GstBreakdown(
    val baseAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val cessAmount: Double,
    val totalTax: Double,
    val grandTotal: Double,
    val gstRatePercent: Double,
    val cessPercent: Double,
    val mode: GstMode,
    val supplyType: SupplyType
)

data class PricingAdvice(
    val netCostExclGst: Double,
    val inputTaxCredit: Double,
    val recommendedSellingExclGst: Double,
    val recommendedMrpInclGst: Double,
    val outputGst: Double,
    val netGstPayableToGovt: Double,
    val expectedProfit: Double,
    val effectiveMarginOnMrpPercent: Double
)

data class DiscountImpact(
    val originalMrp: Double,
    val discountedMrp: Double,
    val customerSavings: Double,
    val taxableValue: Double,
    val outputGst: Double,
    val profitLeft: Double,
    val newMarginPercent: Double,
    val maxBreakevenDiscountPercent: Double,
    val isLossMaking: Boolean
)

data class LineTaxResult(
    val unitPriceInclGst: Double,
    val taxableAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val lineTotal: Double,
    val lineProfit: Double
)

object GstCalculatorEngine {

    val STANDARD_SLABS = listOf(0.0, 0.25, 3.0, 5.0, 12.0, 18.0, 28.0)

    fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun formatMoney(amount: Double, currencySymbol: String = "₹"): String {
        val rounded = round2(amount)
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currencySymbol${formatter.format(rounded)}"
    }

    fun formatCompactRate(rate: Double): String {
        return if (rate % 1.0 == 0.0) "${rate.toInt()}%" else "${round2(rate)}%"
    }

    fun calculateGst(
        amount: Double,
        gstRatePercent: Double,
        cessPercent: Double = 0.0,
        mode: GstMode,
        supplyType: SupplyType
    ): GstBreakdown {
        val safeAmount = amount.coerceAtLeast(0.0)
        val safeGst = gstRatePercent.coerceAtLeast(0.0)
        val safeCess = cessPercent.coerceAtLeast(0.0)
        val combinedRate = safeGst + safeCess

        val base: Double
        val totalGst: Double
        val cess: Double
        val grandTotal: Double

        if (mode == GstMode.EXCLUSIVE) {
            base = round2(safeAmount)
            totalGst = round2(base * (safeGst / 100.0))
            cess = round2(base * (safeCess / 100.0))
            grandTotal = round2(base + totalGst + cess)
        } else {
            val divisor = 1.0 + (combinedRate / 100.0)
            base = if (divisor > 0.0) round2(safeAmount / divisor) else safeAmount
            val totalTaxRaw = (safeAmount - base).coerceAtLeast(0.0)
            if (combinedRate > 0.0) {
                totalGst = round2(totalTaxRaw * (safeGst / combinedRate))
                cess = round2((totalTaxRaw - totalGst).coerceAtLeast(0.0))
            } else {
                totalGst = 0.0
                cess = 0.0
            }
            grandTotal = round2(safeAmount)
        }

        val cgst = if (supplyType == SupplyType.INTRA_STATE) round2(totalGst / 2.0) else 0.0
        val sgst = if (supplyType == SupplyType.INTRA_STATE) round2(totalGst - cgst) else 0.0
        val igst = if (supplyType == SupplyType.INTER_STATE) totalGst else 0.0

        return GstBreakdown(
            baseAmount = base,
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = igst,
            cessAmount = cess,
            totalTax = round2(totalGst + cess),
            grandTotal = grandTotal,
            gstRatePercent = safeGst,
            cessPercent = safeCess,
            mode = mode,
            supplyType = supplyType
        )
    }

    fun calculatePricingAdvisor(
        purchasePrice: Double,
        purchaseIncludesGst: Boolean,
        purchaseGstRate: Double,
        desiredMarginPercent: Double,
        sellingGstRate: Double
    ): PricingAdvice {
        val safePurchase = purchasePrice.coerceAtLeast(0.0)
        val safeInGst = purchaseGstRate.coerceAtLeast(0.0)
        val safeOutGst = sellingGstRate.coerceAtLeast(0.0)
        val safeMargin = desiredMarginPercent.coerceAtLeast(0.0)

        val netCost: Double
        val itc: Double
        if (purchaseIncludesGst) {
            val divisor = 1.0 + (safeInGst / 100.0)
            netCost = if (divisor > 0.0) round2(safePurchase / divisor) else round2(safePurchase)
            itc = round2((safePurchase - netCost).coerceAtLeast(0.0))
        } else {
            netCost = round2(safePurchase)
            itc = round2(netCost * (safeInGst / 100.0))
        }

        val targetSellingExcl = round2(netCost * (1.0 + safeMargin / 100.0))
        val outputGst = round2(targetSellingExcl * (safeOutGst / 100.0))
        val mrp = round2(targetSellingExcl + outputGst)
        val netPayable = round2((outputGst - itc).coerceAtLeast(0.0))
        val profit = round2(targetSellingExcl - netCost)
        val marginOnMrp = if (mrp > 0.0) round2((profit / mrp) * 100.0) else 0.0

        return PricingAdvice(
            netCostExclGst = netCost,
            inputTaxCredit = itc,
            recommendedSellingExclGst = targetSellingExcl,
            recommendedMrpInclGst = mrp,
            outputGst = outputGst,
            netGstPayableToGovt = netPayable,
            expectedProfit = profit,
            effectiveMarginOnMrpPercent = marginOnMrp
        )
    }

    fun calculateDiscountBreakeven(
        costExclGst: Double,
        mrpInclGst: Double,
        gstRate: Double,
        discountPercent: Double
    ): DiscountImpact {
        val safeCost = costExclGst.coerceAtLeast(0.0)
        val safeMrp = mrpInclGst.coerceAtLeast(0.0)
        val safeGst = gstRate.coerceAtLeast(0.0)
        val safeDiscount = discountPercent.coerceIn(0.0, 99.0)

        val discountedMrp = round2(safeMrp * (1.0 - safeDiscount / 100.0))
        val savings = round2((safeMrp - discountedMrp).coerceAtLeast(0.0))
        val divisor = 1.0 + (safeGst / 100.0)
        val taxable = if (divisor > 0.0) round2(discountedMrp / divisor) else discountedMrp
        val outputGst = round2((discountedMrp - taxable).coerceAtLeast(0.0))
        val profitLeft = round2(taxable - safeCost)
        val newMargin = if (taxable > 0.0) round2((profitLeft / taxable) * 100.0) else 0.0

        val breakevenMrp = round2(safeCost * divisor)
        val maxDiscount = if (safeMrp > 0.0 && breakevenMrp < safeMrp) {
            round2(((safeMrp - breakevenMrp) / safeMrp) * 100.0)
        } else {
            0.0
        }

        return DiscountImpact(
            originalMrp = round2(safeMrp),
            discountedMrp = discountedMrp,
            customerSavings = savings,
            taxableValue = taxable,
            outputGst = outputGst,
            profitLeft = profitLeft,
            newMarginPercent = newMargin,
            maxBreakevenDiscountPercent = maxDiscount,
            isLossMaking = profitLeft < 0.0
        )
    }

    fun calculateProductMarginPercent(
        costPriceExclGst: Double,
        sellingPrice: Double,
        isSellingPriceTaxInclusive: Boolean,
        gstRate: Double
    ): Double {
        if (costPriceExclGst <= 0.0) return 0.0
        val divisor = 1.0 + (gstRate.coerceAtLeast(0.0) / 100.0)
        val sellingExcl = if (isSellingPriceTaxInclusive && divisor > 0.0) {
            sellingPrice / divisor
        } else {
            sellingPrice
        }
        return round2(((sellingExcl - costPriceExclGst) / costPriceExclGst) * 100.0)
    }

    fun calculateLineItem(
        costPriceExclGst: Double,
        sellingPrice: Double,
        isSellingPriceTaxInclusive: Boolean,
        gstRate: Double,
        quantity: Int,
        discountPercent: Double,
        supplyType: SupplyType
    ): LineTaxResult {
        val safeQty = quantity.coerceAtLeast(1)
        val divisor = 1.0 + (gstRate.coerceAtLeast(0.0) / 100.0)
        val rawUnitIncl = if (isSellingPriceTaxInclusive) {
            sellingPrice
        } else {
            sellingPrice * divisor
        }
        val discountedUnitIncl = rawUnitIncl * (1.0 - discountPercent.coerceIn(0.0, 99.0) / 100.0)
        val lineGrossIncl = round2(discountedUnitIncl * safeQty)
        val breakdown = calculateGst(
            amount = lineGrossIncl,
            gstRatePercent = gstRate,
            cessPercent = 0.0,
            mode = GstMode.INCLUSIVE,
            supplyType = supplyType
        )
        val lineProfit = round2(breakdown.baseAmount - (costPriceExclGst * safeQty))
        return LineTaxResult(
            unitPriceInclGst = round2(discountedUnitIncl),
            taxableAmount = breakdown.baseAmount,
            cgstAmount = breakdown.cgstAmount,
            sgstAmount = breakdown.sgstAmount,
            igstAmount = breakdown.igstAmount,
            lineTotal = breakdown.grandTotal,
            lineProfit = lineProfit
        )
    }
}
