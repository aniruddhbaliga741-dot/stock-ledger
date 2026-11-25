package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CartDraftItem
import com.example.data.local.ProductEntity
import com.example.data.local.PurchaseLogEntity
import com.example.data.local.SaleInvoiceEntity
import com.example.data.local.SaleLineItemEntity
import com.example.data.local.ShopRepository
import com.example.data.local.StoreProfile
import com.example.domain.DiscountImpact
import com.example.domain.GstBreakdown
import com.example.domain.GstCalculatorEngine
import com.example.domain.GstMode
import com.example.domain.PricingAdvice
import com.example.domain.SupplyType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    EXPIRING_SOON
}

data class CartSummary(
    val taxableSubtotal: Double = 0.0,
    val cgstTotal: Double = 0.0,
    val sgstTotal: Double = 0.0,
    val igstTotal: Double = 0.0,
    val discountSavings: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalItems: Int = 0
)

data class DashboardMetrics(
    val todaySales: Double = 0.0,
    val todayProfit: Double = 0.0,
    val totalInventoryCostValue: Double = 0.0,
    val totalInventoryRetailValue: Double = 0.0,
    val outputGstTotal: Double = 0.0,
    val inputItcTotal: Double = 0.0,
    val netGstLiability: Double = 0.0,
    val lowStockCount: Int = 0,
    val expiringSoonCount: Int = 0
)

data class GstLabState(
    val calcAmountText: String = "1000",
    val calcGstRate: Double = 18.0,
    val calcCustomRateText: String = "",
    val calcCessText: String = "0",
    val calcMode: GstMode = GstMode.EXCLUSIVE,
    val calcSupplyType: SupplyType = SupplyType.INTRA_STATE,
    val advisorPurchaseCostText: String = "500",
    val advisorPurchaseIncludesGst: Boolean = true,
    val advisorPurchaseGstRate: Double = 18.0,
    val advisorDesiredMarginText: String = "25",
    val advisorSellingGstRate: Double = 18.0,
    val discountCostText: String = "400",
    val discountMrpText: String = "650",
    val discountGstRate: Double = 18.0,
    val discountPercentText: String = "12"
) {
    val gstBreakdown: GstBreakdown
        get() {
            val amount = calcAmountText.toDoubleOrNull() ?: 0.0
            val rate = calcCustomRateText.toDoubleOrNull() ?: calcGstRate
            val cess = calcCessText.toDoubleOrNull() ?: 0.0
            return GstCalculatorEngine.calculateGst(
                amount = amount,
                gstRatePercent = rate,
                cessPercent = cess,
                mode = calcMode,
                supplyType = calcSupplyType
            )
        }

    val pricingAdvice: PricingAdvice
        get() = GstCalculatorEngine.calculatePricingAdvisor(
            purchasePrice = advisorPurchaseCostText.toDoubleOrNull() ?: 0.0,
            purchaseIncludesGst = advisorPurchaseIncludesGst,
            purchaseGstRate = advisorPurchaseGstRate,
            desiredMarginPercent = advisorDesiredMarginText.toDoubleOrNull() ?: 0.0,
            sellingGstRate = advisorSellingGstRate
        )

    val discountImpact: DiscountImpact
        get() = GstCalculatorEngine.calculateDiscountBreakeven(
            costExclGst = discountCostText.toDoubleOrNull() ?: 0.0,
            mrpInclGst = discountMrpText.toDoubleOrNull() ?: 0.0,
            gstRate = discountGstRate,
            discountPercent = discountPercentText.toDoubleOrNull() ?: 0.0
        )
}

data class BillingState(
    val cart: List<CartDraftItem> = emptyList(),
    val customerName: String = "",
    val supplyType: SupplyType = SupplyType.INTRA_STATE,
    val billDiscountText: String = "0",
    val searchQuery: String = "",
    val lastCompletedInvoice: SaleInvoiceEntity? = null,
    val lastCompletedItems: List<SaleLineItemEntity> = emptyList()
) {
    val summary: CartSummary
        get() {
            if (cart.isEmpty()) return CartSummary()
            val billDisc = (billDiscountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 95.0)
            var taxable = 0.0
            var cgst = 0.0
            var sgst = 0.0
            var igst = 0.0
            var fullGross = 0.0
            var totalCost = 0.0

            cart.forEach { item ->
                val effectiveDisc = (1.0 - (1.0 - item.itemDiscountPercent / 100.0) * (1.0 - billDisc / 100.0)) * 100.0
                val line = GstCalculatorEngine.calculateLineItem(
                    costPriceExclGst = item.product.costPrice,
                    sellingPrice = item.product.sellingPrice,
                    isSellingPriceTaxInclusive = item.product.isSellingPriceTaxInclusive,
                    gstRate = item.product.gstRate,
                    quantity = item.quantity,
                    discountPercent = effectiveDisc,
                    supplyType = supplyType
                )
                val full = GstCalculatorEngine.calculateLineItem(
                    costPriceExclGst = item.product.costPrice,
                    sellingPrice = item.product.sellingPrice,
                    isSellingPriceTaxInclusive = item.product.isSellingPriceTaxInclusive,
                    gstRate = item.product.gstRate,
                    quantity = item.quantity,
                    discountPercent = 0.0,
                    supplyType = supplyType
                ).lineTotal
                taxable += line.taxableAmount
                cgst += line.cgstAmount
                sgst += line.sgstAmount
                igst += line.igstAmount
                fullGross += full
                totalCost += item.product.costPrice * item.quantity
            }
            val rawGrand = GstCalculatorEngine.round2(taxable + cgst + sgst + igst)
            val roundedGrand = rawGrand.roundToInt().toDouble()
            val roundOff = GstCalculatorEngine.round2(roundedGrand - rawGrand)
            return CartSummary(
                taxableSubtotal = GstCalculatorEngine.round2(taxable),
                cgstTotal = GstCalculatorEngine.round2(cgst),
                sgstTotal = GstCalculatorEngine.round2(sgst),
                igstTotal = GstCalculatorEngine.round2(igst),
                discountSavings = GstCalculatorEngine.round2((fullGross - rawGrand).coerceAtLeast(0.0)),
                roundOff = roundOff,
                grandTotal = roundedGrand,
                netProfit = GstCalculatorEngine.round2(taxable - totalCost),
                totalItems = cart.sumOf { it.quantity }
            )
        }
}

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    val storeProfile: StateFlow<StoreProfile> = repository.storeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreProfile())

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<SaleInvoiceEntity>> = repository.invoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseLogEntity>> = repository.purchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inventorySearch = MutableStateFlow("")
    val inventorySearch: StateFlow<String> = _inventorySearch

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _inventoryFilter = MutableStateFlow(InventoryFilter.ALL)
    val inventoryFilter: StateFlow<InventoryFilter> = _inventoryFilter

    private val _billingState = MutableStateFlow(BillingState())
    val billingState: StateFlow<BillingState> = _billingState

    private val _gstLabState = MutableStateFlow(GstLabState())
    val gstLabState: StateFlow<GstLabState> = _gstLabState

    private val _selectedInvoiceItems = MutableStateFlow<List<SaleLineItemEntity>>(emptyList())
    val selectedInvoiceItems: StateFlow<List<SaleLineItemEntity>> = _selectedInvoiceItems

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        products,
        invoices,
        purchases
    ) { prodList, invList, purList ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayInvoices = invList.filter { it.createdAt >= startOfDay }
        val outputGst = invList.sumOf { it.cgstTotal + it.sgstTotal + it.igstTotal }
        val inputItc = purList.sumOf { it.inputTaxCredit }
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 86_400_000L

        val retailVal = prodList.sumOf { p ->
            val unitRetail = if (p.isSellingPriceTaxInclusive) {
                p.sellingPrice
            } else {
                p.sellingPrice * (1.0 + p.gstRate / 100.0)
            }
            unitRetail * p.stockQty
        }

        DashboardMetrics(
            todaySales = GstCalculatorEngine.round2(todayInvoices.sumOf { it.grandTotal }),
            todayProfit = GstCalculatorEngine.round2(todayInvoices.sumOf { it.totalProfit }),
            totalInventoryCostValue = GstCalculatorEngine.round2(prodList.sumOf { it.costPrice * it.stockQty }),
            totalInventoryRetailValue = GstCalculatorEngine.round2(retailVal),
            outputGstTotal = GstCalculatorEngine.round2(outputGst),
            inputItcTotal = GstCalculatorEngine.round2(inputItc),
            netGstLiability = GstCalculatorEngine.round2((outputGst - inputItc).coerceAtLeast(0.0)),
            lowStockCount = prodList.count { it.stockQty <= it.reorderThreshold },
            expiringSoonCount = prodList.count { p ->
                val exp = p.expiryEpochMillis ?: return@count false
                exp <= now + thirtyDaysMs && p.stockQty > 0
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    init {
        viewModelScope.launch {
            repository.ensureStarterCatalogSeeded()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setInventorySearch(query: String) {
        _inventorySearch.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setInventoryFilter(filter: InventoryFilter) {
        _inventoryFilter.value = filter
    }

    fun upsertProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.upsertProduct(product)
            _statusMessage.value = "${product.name} saved to inventory"
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            _billingState.update { state ->
                state.copy(cart = state.cart.filterNot { it.product.id == product.id })
            }
            _statusMessage.value = "${product.name} removed"
        }
    }

    fun adjustStock(product: ProductEntity, delta: Int) {
        viewModelScope.launch {
            repository.adjustStock(product.id, delta)
        }
    }

    fun recordRestock(
        product: ProductEntity,
        quantityAdded: Int,
        supplierName: String,
        unitCostExclGst: Double,
        gstRate: Double
    ) {
        viewModelScope.launch {
            repository.recordRestock(
                product = product,
                quantityAdded = quantityAdded,
                supplierName = supplierName,
                unitCostExclGst = unitCostExclGst,
                gstRate = gstRate
            )
            _statusMessage.value = "Restocked +$quantityAdded ${product.unit} & logged ITC"
        }
    }

    // --- Quick Bill Cart ---
    fun setBillingSearch(query: String) {
        _billingState.update { it.copy(searchQuery = query) }
    }

    fun setCustomerName(name: String) {
        _billingState.update { it.copy(customerName = name) }
    }

    fun setBillingSupplyType(type: SupplyType) {
        _billingState.update { it.copy(supplyType = type) }
    }

    fun setBillDiscountText(value: String) {
        _billingState.update { it.copy(billDiscountText = value) }
    }

    fun addToCart(product: ProductEntity) {
        if (product.stockQty <= 0) {
            _statusMessage.value = "${product.name} is out of stock"
            return
        }
        _billingState.update { state ->
            val existing = state.cart.find { it.product.id == product.id }
            val updatedCart = if (existing != null) {
                if (existing.quantity >= product.stockQty) {
                    _statusMessage.value = "Max available stock reached (${product.stockQty} ${product.unit})"
                    state.cart
                } else {
                    state.cart.map {
                        if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                    }
                }
            } else {
                state.cart + CartDraftItem(product = product, quantity = 1)
            }
            state.copy(cart = updatedCart)
        }
    }

    fun updateCartItemQuantity(productId: Long, delta: Int) {
        _billingState.update { state ->
            val updated = state.cart.mapNotNull { item ->
                if (item.product.id != productId) {
                    item
                } else {
                    val nextQty = item.quantity + delta
                    when {
                        nextQty <= 0 -> null
                        nextQty > item.product.stockQty -> {
                            _statusMessage.value = "Only ${item.product.stockQty} ${item.product.unit} in stock"
                            item
                        }
                        else -> item.copy(quantity = nextQty)
                    }
                }
            }
            state.copy(cart = updated)
        }
    }

    fun clearCart() {
        _billingState.update { it.copy(cart = emptyList(), billDiscountText = "0", customerName = "") }
    }

    fun checkoutCart() {
        val snapshot = _billingState.value
        if (snapshot.cart.isEmpty()) return
        viewModelScope.launch {
            val invoice = repository.completeSale(
                cartItems = snapshot.cart,
                customerName = snapshot.customerName,
                supplyType = snapshot.supplyType,
                billDiscountPercent = snapshot.billDiscountText.toDoubleOrNull() ?: 0.0
            )
            if (invoice != null) {
                val items = repository.getInvoiceLineItems(invoice.id)
                _billingState.update {
                    it.copy(
                        cart = emptyList(),
                        customerName = "",
                        billDiscountText = "0",
                        lastCompletedInvoice = invoice,
                        lastCompletedItems = items
                    )
                }
            }
        }
    }

    fun dismissCompletedReceipt() {
        _billingState.update { it.copy(lastCompletedInvoice = null, lastCompletedItems = emptyList()) }
    }

    fun loadInvoiceItems(invoiceId: Long) {
        viewModelScope.launch {
            _selectedInvoiceItems.value = repository.getInvoiceLineItems(invoiceId)
        }
    }

    // --- GST Lab Updates ---
    fun updateGstLab(transform: (GstLabState) -> GstLabState) {
        _gstLabState.update(transform)
    }

    fun prefillDiscountSimulatorFromProduct(product: ProductEntity, suggestedDiscount: Double = 15.0) {
        val mrp = if (product.isSellingPriceTaxInclusive) {
            product.sellingPrice
        } else {
            GstCalculatorEngine.round2(product.sellingPrice * (1.0 + product.gstRate / 100.0))
        }
        _gstLabState.update {
            it.copy(
                discountCostText = product.costPrice.toString(),
                discountMrpText = mrp.toString(),
                discountGstRate = product.gstRate,
                discountPercentText = suggestedDiscount.toInt().toString()
            )
        }
    }

    // --- Store Profile ---
    fun updateStoreProfile(
        shopName: String,
        gstin: String,
        phone: String,
        address: String,
        currencySymbol: String,
        defaultSupplyType: SupplyType
    ) {
        viewModelScope.launch {
            repository.updateStoreProfile(
                shopName = shopName,
                gstin = gstin,
                phone = phone,
                address = address,
                currencySymbol = currencySymbol,
                defaultSupplyType = defaultSupplyType
            )
            _billingState.update { it.copy(supplyType = defaultSupplyType) }
            _gstLabState.update { it.copy(calcSupplyType = defaultSupplyType) }
            _statusMessage.value = "Store profile updated"
        }
    }

    class Factory(private val repository: ShopRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShopViewModel(repository) as T
        }
    }
}
