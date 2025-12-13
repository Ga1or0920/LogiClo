package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.data.repository.ClosetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SyncManager(
    private val appScope: CoroutineScope,
    private val closetRepository: ClosetRepository
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = Firebase.firestore

    fun initialize() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("SyncManager", "User logged in: ${user.uid}")
                appScope.launch {
                    try {
                        val closetCollection = firestore.collection("users").document(user.uid).collection("clothing_items")
                        val remoteItemsSnapshot = closetCollection.limit(1).get().await()
                        val localItems = closetRepository.observeAll().first()

                        if (remoteItemsSnapshot.isEmpty && localItems.isNotEmpty()) {
                            Log.d("SyncManager", "Remote is empty, local has data. Syncing up.")
                            closetRepository.syncUp(user.uid)
                        } else {
                            Log.d("SyncManager", "Remote has data or local is empty. Syncing down.")
                            closetRepository.syncDown(user.uid)
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Error during sync, signing out.", e)
                        auth.signOut()
                    }
                }
            } else {
                Log.d("SyncManager", "User logged out")
                appScope.launch {
                    closetRepository.clearAll()
                    closetRepository.seedSampleData()
                }
            }
        }
    }
}
