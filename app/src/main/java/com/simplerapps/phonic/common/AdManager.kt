package com.simplerapps.phonic.common

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.simplerapps.phonic.R

class AdManager {
    companion object {
        @Volatile var rewardedAd: RewardedAd? = null

        fun loadAd(context: Context) {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context, context.resources.getString(R.string.share_start_rewarded_ad_id),
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        rewardedAd = null
                    }

                    override fun onAdLoaded(ad: RewardedAd) {
                        super.onAdLoaded(ad)
                        rewardedAd = ad
                    }
                }
            )
        }
    }
}