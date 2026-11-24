package com.creazzy.rofof.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("""
        SELECT accounts.*,
            accounts.openingBalanceMinor
            + COALESCE((
                SELECT SUM(CASE
                    WHEN transactions.transactionType = 'INCOME' THEN transactions.amountMinor
                    WHEN transactions.transactionType = 'EXPENSE' THEN -transactions.amountMinor
                    ELSE 0
                END)
                FROM transactions WHERE transactions.accountId = accounts.id
            ), 0)
            + COALESCE((
                SELECT SUM(transfers.amountMinor)
                FROM transfers WHERE transfers.toAccountId = accounts.id
            ), 0)
            - COALESCE((
                SELECT SUM(transfers.amountMinor)
                FROM transfers WHERE transfers.fromAccountId = accounts.id
            ), 0) AS balanceMinor
        FROM accounts
        WHERE accounts.isArchived = 0
        ORDER BY accounts.name COLLATE NOCASE
    """)
    fun observeBalances(): Flow<List<AccountBalance>>

    @Query("""
        SELECT accounts.*,
            accounts.openingBalanceMinor
            + COALESCE((
                SELECT SUM(CASE WHEN transactions.transactionType = 'INCOME' THEN transactions.amountMinor
                    WHEN transactions.transactionType = 'EXPENSE' THEN -transactions.amountMinor ELSE 0 END)
                FROM transactions WHERE transactions.accountId = accounts.id
            ), 0)
            + COALESCE((SELECT SUM(transfers.amountMinor) FROM transfers WHERE transfers.toAccountId = accounts.id), 0)
            - COALESCE((SELECT SUM(transfers.amountMinor) FROM transfers WHERE transfers.fromAccountId = accounts.id), 0)
            AS balanceMinor
        FROM accounts
        ORDER BY accounts.isArchived, accounts.name COLLATE NOCASE
    """)
    fun observeAllBalances(): Flow<List<AccountBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AccountEntity?

    @Query("""
        SELECT * FROM accounts
        WHERE isArchived = 0 AND currencyCode = :currencyCode AND currencyExponent = :currencyExponent
        ORDER BY CASE WHEN id = 'default-cash' THEN 0 ELSE 1 END, createdAtEpochMillis
        LIMIT 1
    """)
    suspend fun findActiveAccountForCurrency(currencyCode: String, currencyExponent: Int): AccountEntity?

    @Query("""
        SELECT accounts.*,
            accounts.openingBalanceMinor
            + COALESCE((SELECT SUM(CASE WHEN transactionType = 'INCOME' THEN amountMinor
                WHEN transactionType = 'EXPENSE' THEN -amountMinor ELSE 0 END)
                FROM transactions WHERE accountId = accounts.id), 0)
            + COALESCE((SELECT SUM(amountMinor) FROM transfers WHERE toAccountId = accounts.id), 0)
            - COALESCE((SELECT SUM(amountMinor) FROM transfers WHERE fromAccountId = accounts.id), 0)
            AS balanceMinor
        FROM accounts
        WHERE accounts.id = :accountId AND accounts.isArchived = 0
        LIMIT 1
    """)
    suspend fun findBalance(accountId: String): AccountBalance?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Query("UPDATE accounts SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE accounts SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)
}
