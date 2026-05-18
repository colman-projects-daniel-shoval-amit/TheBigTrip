package com.dsa.thebigtrip.data.repository.users

import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebaseUserModel
import com.dsa.thebigtrip.data.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    private val userDao = AppLocalDb.db.userDao
    private val firebaseUserModel = FirebaseUserModel()

    companion object {
        val shared = UserRepository()
    }

    suspend fun addUser(user: User) {
        firebaseUserModel.addUser(user)
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    suspend fun getUserById(id: String): User? = withContext(Dispatchers.IO) {
        userDao.getUserById(id)
    }

    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.updateUser(user)
        }
    }
}
