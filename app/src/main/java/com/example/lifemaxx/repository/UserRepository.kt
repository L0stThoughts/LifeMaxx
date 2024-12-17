package com.example.lifemaxx.repository

import com.example.lifemaxx.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userCollection = db.collection("users")

    suspend fun addUser(user: User): Boolean {
        return try {
            userCollection.document(user.id).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val document = userCollection.document(userId).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(userId: String, updatedData: Map<String, Any>): Boolean {
        return try {
            userCollection.document(userId).update(updatedData).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
