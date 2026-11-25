package com.example

import com.example.domain.GstCalculatorEngine
import com.example.domain.GstMode
import com.example.domain.SupplyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GstCalculatorEngineTest {

    @Test
    fun `exclusive intra state GST splits equally into CGST and SGST`() {
        val result = GstCalculatorEngine.calculateGst(
            amount = 1000.0,
            gstRatePercent = 18.0,
            cessPercent = 0.0,
            mode = GstMode.EXCLUSIVE,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(1000.0, result.baseAmount, 0.01)
        assertEquals(90.0, result.cgstAmount, 0.01)
        assertEquals(90.0, result.sgstAmount, 0.01)
        assertEquals(0.0, result.igstAmount, 0.01)
        assertEquals(180.0, result.totalTax, 0.01)
        assertEquals(1180.0, result.grandTotal, 0.01)
    }

    @Test
    fun `inclusive inter state GST extracts base amount and assigns IGST`() {
        val result = GstCalculatorEngine.calculateGst(
            amount = 1180.0,
            gstRatePercent = 18.0,
            cessPercent = 0.0,
            mode = GstMode.INCLUSIVE,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(1000.0, result.baseAmount, 0.01)
        assertEquals(0.0, result.cgstAmount, 0.01)
        assertEquals(0.0, result.sgstAmount, 0.01)
        assertEquals(180.0, result.igstAmount, 0.01)
        assertEquals(1180.0, result.grandTotal, 0.01)
    }

    @Test
    fun `pricing advisor calculates ITC net cost and recommended MRP`() {
        val advice = GstCalculatorEngine.calculatePricingAdvisor(
            purchasePrice = 1180.0,
            purchaseIncludesGst = true,
            purchaseGstRate = 18.0,
            desiredMarginPercent = 20.0,
            sellingGstRate = 18.0
        )
        assertEquals(1000.0, advice.netCostExclGst, 0.01)
        assertEquals(180.0, advice.inputTaxCredit, 0.01)
        assertEquals(1200.0, advice.recommendedSellingExclGst, 0.01)
        assertEquals(216.0, advice.outputGst, 0.01)
        assertEquals(1416.0, advice.recommendedMrpInclGst, 0.01)
        assertEquals(36.0, advice.netGstPayableToGovt, 0.01)
        assertEquals(200.0, advice.expectedProfit, 0.01)
    }

    @Test
    fun `discount breakeven detects safe vs loss-making discount`() {
        val safe = GstCalculatorEngine.calculateDiscountBreakeven(
            costExclGst = 400.0,
            mrpInclGst = 590.0, // 500 base + 18% GST
            gstRate = 18.0,
            discountPercent = 10.0
        )
        assertFalse(safe.isLossMaking)
        assertEquals(20.0, safe.maxBreakevenDiscountPercent, 0.1)

        val loss = GstCalculatorEngine.calculateDiscountBreakeven(
            costExclGst = 400.0,
            mrpInclGst = 590.0,
            gstRate = 18.0,
            discountPercent = 25.0
        )
        assertTrue(loss.isLossMaking)
        assertTrue(loss.profitLeft < 0.0)
    }
}
