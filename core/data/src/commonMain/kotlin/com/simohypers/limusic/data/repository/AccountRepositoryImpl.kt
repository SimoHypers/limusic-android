package com.simohypers.limusic.data.repository

import com.simohypers.limusic.data.db.datasource.LocalDataSource
import com.simohypers.limusic.data.mapping.toDomainAccountInfo
import com.simohypers.limusic.domain.data.entities.GoogleAccountEntity
import com.simohypers.limusic.domain.data.model.account.AccountInfo
import com.simohypers.limusic.domain.repository.AccountRepository
import com.simohypers.limusic.kotlinytmusicscraper.YouTube
import com.simohypers.limusic.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val TAG = "AccountRepositoryImpl"

internal class AccountRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val youTube: YouTube,
) : AccountRepository {
    override fun getYouTubeCookie() = youTube.cookie

    override val authRejected: Flow<Unit> get() = youTube.authRejected

    override fun getAccountInfo(cookie: String): Flow<List<AccountInfo>> =
        flow {
            // Asks with this jar alone: the live session is left as it is until the caller
            // decides to switch to it.
            youTube
                .getAccountListWithPageId(cookie)
                .onSuccess {
                    emit(it.map { account -> account.toDomainAccountInfo() })
                }.onFailure {
                    Logger.e(TAG, "getAccountInfo: ${it.message}", it)
                    emit(emptyList())
                }
        }.flowOn(Dispatchers.IO)

    override fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) =
        flow {
            emit(localDataSource.insertGoogleAccount(googleAccountEntity))
        }.flowOn(Dispatchers.IO)

    override fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?> =
        flow<List<GoogleAccountEntity>?> { emit(localDataSource.getGoogleAccounts()) }.flowOn(
            Dispatchers.IO,
        )

    override fun getUsedGoogleAccount(): Flow<GoogleAccountEntity?> =
        flow<GoogleAccountEntity?> { emit(localDataSource.getUsedGoogleAccount()) }.flowOn(
            Dispatchers.IO,
        )

    override suspend fun deleteGoogleAccount(email: String) =
        withContext(Dispatchers.IO) {
            localDataSource.deleteGoogleAccount(email)
        }

    override fun updateGoogleAccountUsed(
        email: String,
        isUsed: Boolean,
    ): Flow<Int> = flow { emit(localDataSource.updateGoogleAccountUsed(email, isUsed)) }.flowOn(Dispatchers.IO)
}