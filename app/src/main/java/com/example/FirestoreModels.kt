package com.example

import com.google.firebase.firestore.Exclude

data class User(
    @get:Exclude var id: String = "",
    val fullName: String = "",
    val email: String = "",
    val balance: Double = 0.0,
    val totalInvested: Double = 0.0,
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankAccountName: String = ""
)

data class Deposit(
    @get:Exclude var id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val status: String = "",
    val proofImageUrl: String = "",
    val timestamp: Long = 0L
)

data class Withdrawal(
    @get:Exclude var id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val status: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val timestamp: Long = 0L
)

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
    val isRead: Boolean = false,
    val actionType: String = ""
)
