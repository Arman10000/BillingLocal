package android.example.billinglocal.domain

import android.example.billinglocal.data.BillingManager
import io.reactivex.Observable

sealed class UserStatus {
    object NotPremium : UserStatus()
    object Premium : UserStatus()

    fun isNotPremium(): Boolean = this is NotPremium
    fun isPremium(): Boolean = this is Premium
}

interface PremiumUseCase {
    fun syncUserStatus()
    fun startPurchasePremium()
    fun observeUserStatus(): Observable<UserStatus>
}

class PremiumUseCaseImpl(
    private val billingManager: BillingManager
) : PremiumUseCase {

    override fun observeUserStatus(): Observable<UserStatus> = billingManager.observeUserStatus()

    override fun startPurchasePremium() {
        billingManager.startPurchasePremium()
    }

    override fun syncUserStatus() {
        billingManager.syncUserStatus()
    }
}