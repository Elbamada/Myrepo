package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode = _isDemoMode.asStateFlow()

    private val _pendingDeposits = MutableStateFlow<List<Deposit>>(emptyList())
    val pendingDeposits = _pendingDeposits.asStateFlow()

    private val _pendingWithdrawals = MutableStateFlow<List<Withdrawal>>(emptyList())
    val pendingWithdrawals = _pendingWithdrawals.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    init {
        if (auth.currentUser != null) {
            setupListeners()
        }
    }

    fun loginWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        _isDemoMode.value = false
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                _isLoggedIn.value = true
                setupListeners()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        _isDemoMode.value = false
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, pass).await()
                _isLoggedIn.value = true
                setupListeners()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    fun enterDemoMode() {
        _isDemoMode.value = true
        _isLoggedIn.value = true
        
        // Populate mock Nigerian/investment records
        _users.value = listOf(
            User("user_1", "Oluwaseun Adebayo", "seun.adebayo@gmail.com", 250000.0, 500000.0, "Guaranty Trust Bank", "0123456789", "Oluwaseun Adebayo"),
            User("user_2", "Chioma Okafor", "chioma.o@yahoo.com", 45000.0, 150000.0, "Access Bank", "0987654321", "Chioma Okafor"),
            User("user_3", "Chidi Obi", "chidi.obi@outlook.com", 1200000.0, 3000000.0, "Zenith Bank", "0011223344", "Chidi Obi")
        )

        _pendingDeposits.value = listOf(
            Deposit("dep_1", "user_1", 50000.0, "PENDING", "https://picsum.photos/seed/deposit1/800/600", System.currentTimeMillis() - 120000),
            Deposit("dep_2", "user_2", 20000.0, "PENDING", "https://picsum.photos/seed/deposit2/800/600", System.currentTimeMillis() - 3600000)
        )

        _pendingWithdrawals.value = listOf(
            Withdrawal("with_1", "user_3", 150000.0, "PENDING", "Zenith Bank", "0011223344", System.currentTimeMillis() - 600000)
        )
    }

    fun logout() {
        if (_isDemoMode.value) {
            _isDemoMode.value = false
            _isLoggedIn.value = false
            _pendingDeposits.value = emptyList()
            _pendingWithdrawals.value = emptyList()
            _users.value = emptyList()
        } else {
            auth.signOut()
            _isLoggedIn.value = false
        }
    }

    private fun setupListeners() {
        if (_isDemoMode.value) return
        db.collection("deposits")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                if (_isDemoMode.value) return@addSnapshotListener
                val docs = snapshot?.documents?.mapNotNull { 
                    it.toObject(Deposit::class.java)?.apply { id = it.id }
                } ?: emptyList()
                _pendingDeposits.value = docs.sortedByDescending { it.timestamp }
            }

        db.collection("withdrawals")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                if (_isDemoMode.value) return@addSnapshotListener
                val docs = snapshot?.documents?.mapNotNull { 
                    it.toObject(Withdrawal::class.java)?.apply { id = it.id }
                } ?: emptyList()
                _pendingWithdrawals.value = docs.sortedByDescending { it.timestamp }
            }
            
        db.collection("users")
            .addSnapshotListener { snapshot, _ ->
                if (_isDemoMode.value) return@addSnapshotListener
                val docs = snapshot?.documents?.mapNotNull {
                    it.toObject(User::class.java)?.apply { id = it.id }
                } ?: emptyList()
                _users.value = docs
            }
    }

    fun approveDeposit(deposit: Deposit) {
        if (_isDemoMode.value) {
            // Update local user balance
            _users.value = _users.value.map {
                if (it.id == deposit.userId) {
                    it.copy(balance = it.balance + deposit.amount)
                } else it
            }
            // Remove from pending deposits
            _pendingDeposits.value = _pendingDeposits.value.filter { it.id != deposit.id }
            return
        }
        viewModelScope.launch {
            try {
                val db = Firebase.firestore
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(deposit.userId)
                    val userSnapshot = transaction.get(userRef)
                    val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
                    
                    transaction.update(userRef, "balance", currentBalance + deposit.amount)
                    
                    val depositRef = db.collection("deposits").document(deposit.id)
                    transaction.update(depositRef, "status", "APPROVED")
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rejectDeposit(deposit: Deposit, reason: String, actionType: String) {
        if (_isDemoMode.value) {
            _pendingDeposits.value = _pendingDeposits.value.filter { it.id != deposit.id }
            return
        }
        viewModelScope.launch {
            try {
                db.collection("deposits").document(deposit.id)
                    .update("status", "REJECTED").await()
                
                sendNotification(
                    userId = deposit.userId,
                    title = "Deposit Rejected",
                    message = reason,
                    actionType = actionType
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun approveWithdrawal(withdrawal: Withdrawal) {
        if (_isDemoMode.value) {
            _pendingWithdrawals.value = _pendingWithdrawals.value.filter { it.id != withdrawal.id }
            return
        }
        viewModelScope.launch {
            try {
                db.collection("withdrawals").document(withdrawal.id)
                    .update("status", "APPROVED").await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rejectWithdrawal(withdrawal: Withdrawal, reason: String, refundToken: Boolean = true) {
        if (_isDemoMode.value) {
            if (refundToken) {
                _users.value = _users.value.map {
                    if (it.id == withdrawal.userId) {
                        it.copy(balance = it.balance + withdrawal.amount)
                    } else it
                }
            }
            _pendingWithdrawals.value = _pendingWithdrawals.value.filter { it.id != withdrawal.id }
            return
        }
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    val withdrawalRef = db.collection("withdrawals").document(withdrawal.id)
                    transaction.update(withdrawalRef, "status", "REJECTED")
                    
                    if (refundToken) {
                        val userRef = db.collection("users").document(withdrawal.userId)
                        val userSnapshot = transaction.get(userRef)
                        val currentBalance = userSnapshot.getDouble("balance") ?: 0.0
                        transaction.update(userRef, "balance", currentBalance + withdrawal.amount)
                    }
                }.await()

                sendNotification(
                    userId = withdrawal.userId,
                    title = "Withdrawal Rejected",
                    message = reason,
                    actionType = "UPDATE_BANK"
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun sendNotification(userId: String, title: String, message: String, actionType: String) {
        if (_isDemoMode.value) return
        viewModelScope.launch {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection("notifications").document()
                val notification = Notification(
                    id = docRef.id,
                    title = title,
                    message = message,
                    createdAt = System.currentTimeMillis(),
                    isRead = false,
                    actionType = actionType
                )
                docRef.set(notification).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
