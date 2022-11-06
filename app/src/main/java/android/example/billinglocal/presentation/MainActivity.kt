package android.example.billinglocal.presentation

import android.annotation.SuppressLint
import android.example.billinglocal.R
import android.example.billinglocal.data.BillingManager
import android.example.billinglocal.domain.PremiumUseCase
import android.example.billinglocal.domain.PremiumUseCaseImpl
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.reactivex.android.schedulers.AndroidSchedulers

class MainActivity : AppCompatActivity() {

    private lateinit var premiumUseCase: PremiumUseCase

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
        val billingManager = BillingManager(this, firebaseCrashlytics)
        premiumUseCase = PremiumUseCaseImpl(billingManager)
        premiumUseCase.observeUserStatus()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.isPremium()) {
                    //есть платный контент
                } else {
                    //нету платный контент
                }
            }

        val purchasePremium = findViewById<Button>(R.id.purchasePremium)
        purchasePremium.setOnClickListener {
            premiumUseCase.startPurchasePremium()//запускаем покупку Premium
        }
    }

    override fun onResume() {
        super.onResume()
        //спецально делаю именно в onResume что бы каждый раз проверять статус покупки и предоставлять платный контент или нет
        premiumUseCase.syncUserStatus()
    }
}