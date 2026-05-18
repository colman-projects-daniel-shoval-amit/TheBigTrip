package com.dsa.thebigtrip.data.repository.users

import android.net.Uri
import com.dsa.thebigtrip.dao.AppLocalDb
import com.dsa.thebigtrip.data.models.FirebaseUserModel
import com.dsa.thebigtrip.data.user.User
import com.dsa.thebigtrip.utils.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {

    private val userDao = AppLocalDb.db.userDao
    private val firebaseUserModel = FirebaseUserModel()

    companion object {
        val shared = UserRepository()
    }

    suspend fun addUser(user: User) {
        withContext(Dispatchers.IO) {
            firebaseUserModel.addUser(user)
            userDao.insert(user)
        }
    }

    suspend fun getUserById(id: String): User? {
        val local = withContext(Dispatchers.IO) { userDao.getUserById(id) }
        if (local != null) return local
        val remote = withContext(Dispatchers.IO) { firebaseUserModel.getUserById(id) }
        if (remote != null) {
            withContext(Dispatchers.IO) { userDao.insert(remote) }
        }
        return remote
    }

    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.updateUser(user)
            firebaseUserModel.updateUser(user)
        }
    }

    suspend fun uploadProfilePicture(uri: Uri, uid: String): String? {
        return withContext(Dispatchers.IO) {
            ImageUtil.uploadUserProfileImage(uri, uid)
        }
    }
}
