package com.fastingtracker.billing

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSS distribution donation handler.
 * Contains zero proprietary billing SDKs and requires zero network permissions;
 * delegates voluntary support to the user's system browser.
 */
@Singleton
class DonationManager @Inject constructor() {

    companion object {
        const val SUPPORT_URL = "https://solocraft.app/fasting-tracker#support"
    }

    private val _state = MutableStateFlow<DonationState>(DonationState.Idle)
    val state: StateFlow<DonationState> = _state.asStateFlow()

    fun startDonation(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL))
            activity.startActivity(intent)
            _state.value = DonationState.Idle
        } catch (_: ActivityNotFoundException) {
            _state.value = DonationState.Error("No browser found to open support page")
        }
    }

    fun resetState() {
        _state.value = DonationState.Idle
    }

    fun destroy() = Unit
}
