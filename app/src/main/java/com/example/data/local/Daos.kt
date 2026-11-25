package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY updatedAt DESC, id DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("UPDATE products SET stockQty = MAX(0, stockQty + :delta), updatedAt = :updatedAt WHERE id = :productId")
    suspend fun adjustStock(productId: Long, delta: Int, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM sale_invoices ORDER BY createdAt DESC, id DESC")
    fun observeAllInvoices(): Flow<List<SaleInvoiceEntity>>

    @Query("SELECT * FROM sale_line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun observeLineItemsForInvoice(invoiceId: Long): Flow<List<SaleLineItemEntity>>

    @Query("SELECT * FROM sale_line_items WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getLineItemsForInvoice(invoiceId: Long): List<SaleLineItemEntity>

    @Insert
    suspend fun insertInvoice(invoice: SaleInvoiceEntity): Long

    @Insert
    suspend fun insertLineItems(items: List<SaleLineItemEntity>)

    @Query("DELETE FROM sale_invoices WHERE id = :invoiceId")
    suspend fun deleteInvoiceById(invoiceId: Long)
}

@Dao
interface PurchaseLogDao {
    @Query("SELECT * FROM purchase_logs ORDER BY createdAt DESC, id DESC")
    fun observeAllPurchases(): Flow<List<PurchaseLogEntity>>

    @Insert
    suspend fun insertPurchase(log: PurchaseLogEntity): Long
}
