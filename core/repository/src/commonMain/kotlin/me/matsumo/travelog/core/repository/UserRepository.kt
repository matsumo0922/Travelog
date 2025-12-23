package me.matsumo.travelog.core.repository

import me.matsumo.travelog.core.datasource.api.UserApi
import me.matsumo.travelog.core.model.db.User

class UserRepository(
    private val userApi: UserApi,
) {
    suspend fun createUser(user: User) {
        userApi.createUser(user)
    }

    suspend fun updateUser(user: User) {
        userApi.updateUser(user)
    }

    suspend fun getUser(id: String): User? {
        return userApi.getUser(id)
    }

    suspend fun deleteUser(id: String) {
        userApi.deleteUser(id)
    }
}
