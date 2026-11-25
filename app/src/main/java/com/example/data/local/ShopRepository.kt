package com.example.data.local

import androidx.room.withTransaction
import com.example.domain.GstCalculatorEngine
import com.example.domain.SupplyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class CartDraftItem(
    val product: ProductEntity,
    val quantity: Int,
    val itemDiscountPercent: Double = 0.0
)

class ShopRepository(
    private val database: AppDatabase,
    private val preferencesRepository: StorePreferencesRepository
) {
    private val productDao = database.productDao()
    private val invoiceDao = database.invoiceDao()
    private val purchaseLogDao = database.purchaseLogDao()

    val products: Flow<List<ProductEntity>> = productDao.observeAllProducts()
    val invoices: Flow<List<SaleInvoiceEntity>> = invoiceDao.observeAllInvoices()
    val purchases: Flow<List<PurchaseLogEntity>> = purchaseLogDao.observeAllPurchases()
    val storeProfile: Flow<StoreProfile> = preferencesRepository.storeProfile

    fun observeInvoiceLineItems(invoiceId: Long): Flow<List<SaleLineItemEntity>> {
        return invoiceDao.observeLineItemsForInvoice(invoiceId)
    }

    suspend fun getInvoiceLineItems(invoiceId: Long): List<SaleLineItemEntity> {
        return invoiceDao.getLineItemsForInvoice(invoiceId)
    }

    suspend fun ensureStarterCatalogSeeded() {
        val profile = preferencesRepository.storeProfile.first()
        if (profile.hasSeededCatalog) return
        if (productDao.countProducts() > 0) {
            preferencesRepository.markCatalogSeeded()
            return
        }
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L
        val starterItems = listOf(
            ProductEntity(
                name = "Organic Basmati Rice (5kg)",
                sku = "GRC-101",
                category = "Groceries",
                hsnCode = "1006",
                unit = "bag",
                stockQty = 18,
                reorderThreshold = 6,
                costPrice = 410.0,
                sellingPrice = 540.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 5.0,
                expiryEpochMillis = now + 180 * dayMs,
                notes = "Premium aged long-grain"
            ),
            ProductEntity(
                name = "Cold-Pressed Mustard Oil (1L)",
                sku = "GRC-104",
                category = "Groceries",
                hsnCode = "1514",
                unit = "btl",
                stockQty = 4,
                reorderThreshold = 8,
                costPrice = 152.0,
                sellingPrice = 195.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 5.0,
                expiryEpochMillis = now + 18 * dayMs,
                notes = "Low stock & near expiry batch"
            ),
            ProductEntity(
                name = "USB-C Fast Charger 30W",
                sku = "ELC-202",
                category = "Electronics",
                hsnCode = "8504",
                unit = "pcs",
                stockQty = 12,
                reorderThreshold = 4,
                costPrice = 480.0,
                sellingPrice = 799.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 18.0,
                notes = "1-year shop warranty"
            ),
            ProductEntity(
                name = "Herbal Aloe Face Wash",
                sku = "PRC-305",
                category = "Personal Care",
                hsnCode = "3304",
                unit = "tube",
                stockQty = 3,
                reorderThreshold = 6,
                costPrice = 110.0,
                sellingPrice = 175.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 18.0,
                expiryEpochMillis = now + 12 * dayMs,
                notes = "Fast-moving skin care"
            ),
            ProductEntity(
                name = "Artisan Masala Chai Tin (250g)",
                sku = "BEV-408",
                category = "Beverages",
                hsnCode = "0902",
                unit = "tin",
                stockQty = 25,
                reorderThreshold = 5,
                costPrice = 140.0,
                sellingPrice = 220.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 5.0,
                expiryEpochMillis = now + 240 * dayMs
            ),
            ProductEntity(
                name = "A4 Spiral Register (200 Pg)",
                sku = "STN-512",
                category = "Stationery",
                hsnCode = "4820",
                unit = "pcs",
                stockQty = 32,
                reorderThreshold = 10,
                costPrice = 58.0,
                sellingPrice = 95.0,
                isSellingPriceTaxInclusive = true,
                gstRate = 12.0
            )
        )
        productDao.insertProducts(starterItems)
        preferencesRepository.markCatalogSeeded()
    }

    suspend fun upsertProduct(product: ProductEntity): Long {
        val toSave = product.copy(updatedAt = System.currentTimeMillis())
        return if (toSave.id == 0L) {
            productDao.insertProduct(toSave)
        } else {
            productDao.updateProduct(toSave)
            toSave.id
        }
    }

    suspend fun deleteProduct(productId: Long) {
        productDao.deleteProductById(productId)
    }

    suspend fun adjustStock(productId: Long, delta: Int) {
        productDao.adjustStock(productId, delta)
    }

    suspend fun recordRestock(
        product: ProductEntity,
        quantityAdded: Int,
        supplierName: String,
        unitCostExclGst: Double,
        gstRate: Double
    ) {
        if (quantityAdded <= 0) return
        val baseCost = GstCalculatorEngine.round2(unitCostExclGst * quantityAdded)
        val itc = GstCalculatorEngine.round2(baseCost * (gstRate / 100.0))
        val totalPaid = GstCalculatorEngine.round2(baseCost + itc)

        database.withTransaction {
            val updated = product.copy(
                stockQty = product.stockQty + quantityAdded,
                costPrice = GstCalculatorEngine.round2(unitCostExclGst),
                updatedAt = System.currentTimeMillis()
            )
            productDao.updateProduct(updated)
            purchaseLogDao.insertPurchase(
                PurchaseLogEntity(
                    supplierName = supplierName.trim().ifEmpty { "Wholesale Supplier" },
                    productId = product.id,
                    productName = product.name,
                    quantityAdded = quantityAdded,
                    unitCostExclGst = GstCalculatorEngine.round2(unitCostExclGst),
                    gstRate = gstRate,
                    inputTaxCredit = itc,
                    totalPaid = totalPaid
                )
            )
        }
    }

    suspend fun completeSale(
        cartItems: List<CartDraftItem>,
        customerName: String,
        supplyType: SupplyType,
        billDiscountPercent: Double
    ): SaleInvoiceEntity? {
        if (cartItems.isEmpty()) return null

        val safeBillDiscount = billDiscountPercent.coerceIn(0.0, 95.0)
        var subtotalExcl = 0.0
        var cgstTotal = 0.0
        var sgstTotal = 0.0
        var igstTotal = 0.0
        var grossBeforeBillDiscount = 0.0
        var totalCost = 0.0

        val computedLines = cartItems.map { item ->
            val effectiveDiscount = (1.0 - (1.0 - item.itemDiscountPercent / 100.0) * (1.0 - safeBillDiscount / 100.0)) * 100.0
            val result = GstCalculatorEngine.calculateLineItem(
                costPriceExclGst = item.product.costPrice,
                sellingPrice = item.product.sellingPrice,
                isSellingPriceTaxInclusive = item.product.isSellingPriceTaxInclusive,
                gstRate = item.product.gstRate,
                quantity = item.quantity,
                discountPercent = effectiveDiscount,
                supplyType = supplyType
            )
            val fullGross = GstCalculatorEngine.calculateLineItem(
                costPriceExclGst = item.product.costPrice,
                sellingPrice = item.product.sellingPrice,
                isSellingPriceTaxInclusive = item.product.isSellingPriceTaxInclusive,
                gstRate = item.product.gstRate,
                quantity = item.quantity,
                discountPercent = 0.0,
                supplyType = supplyType
            ).lineTotal

            subtotalExcl += result.taxableAmount
            cgstTotal += result.cgstAmount
            sgstTotal += result.sgstAmount
            igstTotal += result.igstAmount
            grossBeforeBillDiscount += fullGross
            totalCost += item.product.costPrice * item.quantity
            item to result
        }

        val rawGrandTotal = GstCalculatorEngine.round2(subtotalExcl + cgstTotal + sgstTotal + igstTotal)
        val roundedGrandTotal = rawGrandTotal.roundToInt().toDouble()
        val roundOff = GstCalculatorEngine.round2(roundedGrandTotal - rawGrandTotal)
        val discountAmount = GstCalculatorEngine.round2((grossBeforeBillDiscount - rawGrandTotal).coerceAtLeast(0.0))
        val totalProfit = GstCalculatorEngine.round2(subtotalExcl - totalCost)

        val datePart = SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())
        val invoiceNumber = "INV-$datePart-${(10..99).random()}"
        val summaryText = cartItems.joinToString(", ") { "${it.product.name} ×${it.quantity}" }

        return database.withTransaction {
            val invoice = SaleInvoiceEntity(
                invoiceNumber = invoiceNumber,
                customerName = customerName.trim().ifEmpty { "Walk-in Customer" },
                supplyType = supplyType.name,
                subtotalExclGst = GstCalculatorEngine.round2(subtotalExcl),
                discountAmount = discountAmount,
                cgstTotal = GstCalculatorEngine.round2(cgstTotal),
                sgstTotal = GstCalculatorEngine.round2(sgstTotal),
                igstTotal = GstCalculatorEngine.round2(igstTotal),
                roundOff = roundOff,
                grandTotal = roundedGrandTotal,
                totalProfit = totalProfit,
                itemCount = cartItems.sumOf { it.quantity },
                itemsSummary = summaryText
            )
            val invoiceId = invoiceDao.insertInvoice(invoice)
            val lineEntities = computedLines.map { (cartItem, calc) ->
                SaleLineItemEntity(
                    invoiceId = invoiceId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    hsnCode = cartItem.product.hsnCode,
                    quantity = cartItem.quantity,
                    unitPriceInclGst = calc.unitPriceInclGst,
                    taxableAmount = calc.taxableAmount,
                    gstRate = cartItem.product.gstRate,
                    cgstAmount = calc.cgstAmount,
                    sgstAmount = calc.sgstAmount,
                    igstAmount = calc.igstAmount,
                    lineTotal = calc.lineTotal,
                    lineProfit = calc.lineProfit
                )
            }
            invoiceDao.insertLineItems(lineEntities)

            cartItems.forEach { cartItem ->
                productDao.adjustStock(cartItem.product.id, -cartItem.quantity)
            }
            invoice.copy(id = invoiceId)
        }
    }

    suspend fun updateStoreProfile(
        shopName: String,
        gstin: String,
        phone: String,
        address: String,
        currencySymbol: String,
        defaultSupplyType: SupplyType
    ) {
        preferencesRepository.updateStoreProfile(
            shopName = shopName,
            gstin = gstin,
            phone = phone,
            address = address,
            currencySymbol = currencySymbol,
            defaultSupplyType = defaultSupplyType
        )
    }
}
