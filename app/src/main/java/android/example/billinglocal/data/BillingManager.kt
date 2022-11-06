package android.example.billinglocal.data

import android.app.Activity
import android.example.billinglocal.domain.UserStatus
import com.android.billingclient.api.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class BillingManager(
    private val activity: Activity,
    private val firebaseCrashlytics: FirebaseCrashlytics
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(activity)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    private val userStatusSubject = BehaviorSubject.createDefault<UserStatus>(UserStatus.NotPremium)
    private var serviceConnected = false

    companion object {
        const val KEY_PRODUCT_PREMIUM = "premium"
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val purchase = purchases?.first() ?: return
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            !purchase.isAcknowledged &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        ) acknowledgePurchase(purchase)
    }

    fun observeUserStatus(): Observable<UserStatus> = userStatusSubject.hide()

    fun syncUserStatus() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    serviceConnected = true
                    queryUserPurchases()
                } else {
                    serviceConnected = false
                    firebaseCrashlytics.recordException(Throwable(billingResult.debugMessage))
                }
            }

            override fun onBillingServiceDisconnected() {
                serviceConnected = false
            }
        })
    }

    fun startPurchasePremium() {
        if (!serviceConnected) return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(KEY_PRODUCT_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                launchBilling(productDetailsList.first())
            }
            else firebaseCrashlytics.recordException(Throwable(billingResult.debugMessage))
        }
    }

    private fun launchBilling(productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            firebaseCrashlytics.recordException(Throwable(billingResult.debugMessage))
        }
    }

    private fun queryUserPurchases() {
        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val purchase = purchaseList.first()
                if (purchase.isAcknowledged) {
                    userStatusSubject.onNext(UserStatus.Premium)
                } else if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                }
            } else firebaseCrashlytics.recordException(Throwable(billingResult.debugMessage))
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) {
            if (it.responseCode == BillingClient.BillingResponseCode.OK) {
                userStatusSubject.onNext(UserStatus.Premium)
            } else firebaseCrashlytics.recordException(Throwable(it.debugMessage))
        }
    }
}