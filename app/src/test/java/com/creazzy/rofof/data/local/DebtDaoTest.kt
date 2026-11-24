package com.creazzy.rofof.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebtDaoTest {
    private lateinit var database: RofofDatabase
    private lateinit var dao: DebtDao

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            RofofDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.debtDao()
    }

    @After fun tearDown() {
        database.close()
    }

    @Test fun repaymentsUpdateOutstandingAndCloseSettledDebt() = runBlocking {
        dao.insertDebt(debt())
        val afterFirst = dao.recordRepayment(repayment("r1", 4_000L))
        assertEquals(6_000L, afterFirst.outstandingMinor)
        assertEquals("OPEN", afterFirst.status)

        val afterFinal = dao.recordRepayment(repayment("r2", 6_000L))
        assertEquals(0L, afterFinal.outstandingMinor)
        assertEquals("CLOSED", afterFinal.status)
    }

    @Test fun overpaymentIsRejectedWithoutPersistingRepayment() = runBlocking {
        dao.insertDebt(debt())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dao.recordRepayment(repayment("too-much", 10_001L)) }
        }
        assertEquals(0L, dao.sumRepayments("debt-1"))
    }

    @Test fun repaymentMustBePositive() = runBlocking {
        dao.insertDebt(debt())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dao.recordRepayment(repayment("zero", 0L)) }
        }
        assertEquals(0L, dao.sumRepayments("debt-1"))
    }

    private fun debt() = DebtEntity(
        id = "debt-1", title = "Test loan", direction = "OWED_BY_ME",
        principalMinor = 10_000L, currencyCode = "EGP", currencyExponent = 2,
        annualRateBasisPoints = 0, dueAtEpochMillis = null, status = "OPEN",
        note = null, createdAtEpochMillis = 1L, updatedAtEpochMillis = 1L
    )

    private fun repayment(id: String, amount: Long) = DebtRepaymentEntity(
        id = id, debtId = "debt-1", amountMinor = amount, paidAtEpochMillis = 2L,
        note = null, createdAtEpochMillis = 2L
    )
}
