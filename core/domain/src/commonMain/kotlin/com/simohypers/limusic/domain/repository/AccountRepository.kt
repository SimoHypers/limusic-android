package com.simohypers.limusic.domain.repository

import com.simohypers.limusic.domain.data.entities.GoogleAccountEntity
import com.simohypers.limusic.domain.data.model.account.AccountInfo
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    /** The channels [cookie] can act as; null when the request failed, so nothing is known either way. */
    fun getAccountInfo(cookie: String): Flow<List<AccountInfo>?>

    fun getYouTubeCookie(): String?

    /** YouTube turned down a request sent with the cookie, so the jar may need re-minting. */
    val authRejected: Flow<Unit>

    fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity): Flow<Long>

    fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?>

    fun getUsedGoogleAccount(): Flow<GoogleAccountEntity?>

    suspend fun deleteGoogleAccount(email: String)

    fun updateGoogleAccountUsed(
        email: String,
        isUsed: Boolean,
    ): Flow<Int>
}