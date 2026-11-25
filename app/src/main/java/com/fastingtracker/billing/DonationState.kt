package com.fastingtracker.billing

sealed class DonationState {
    data object Idle : DonationState()
    data object Loading : DonationState()
    data object Processing : DonationState()
    data object Success : DonationState()
    data object Cancelled : DonationState()
    data class Error(val message: String) : DonationState()
}
