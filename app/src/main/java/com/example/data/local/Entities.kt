package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val category: String,
    val hsnCode: String = "",
    val unit: String = "pcs",
    val stockQty: Int,
    val reorderThreshold: Int = 5,
    val costPrice: Double,
    val sellingPrice: Double,
    val isSellingPriceTaxInclusive: Boolean = true,
    val gstRate: Double = 18.0,
    val expiryEpochMillis: Long? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_invoices")
data class SaleInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String = "Walk-in Customer",
    val supplyType: String = "INTRA_STATE",
    val subtotalExclGst: Double,
    val discountAmount: Double = 0.0,
    val cgstTotal: Double,
    val sgstTotal: Double,
    val igstTotal: Double,
    val roundOff: Double = 0.0,
    val grandTotal: Double,
    val totalProfit: Double,
    val itemCount: Int,
    val itemsSummary: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_line_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleInvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class SaleLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val productName: String,
    val hsnCode: String,
    val quantity: Int,
    val unitPriceInclGst: Double,
    val taxableAmount: Double,
    val gstRate: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val lineTotal: Double,
    val lineProfit: Double
)

@Entity(tableName = "purchase_logs")
data class PurchaseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierName: String,
    val productId: Long,
    val productName: String,
    val quantityAdded: Int,
    val unitCostExclGst: Double,
    val gstRate: Double,
    val inputTaxCredit: Double,
    val totalPaid: Double,
    val createdAt: Long = System.currentTimeMillis()
)
