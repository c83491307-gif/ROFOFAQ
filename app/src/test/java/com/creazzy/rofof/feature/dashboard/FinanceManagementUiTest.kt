package com.creazzy.rofof.feature.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.creazzy.rofof.data.local.AccountBalance
import com.creazzy.rofof.data.local.CategoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceManagementUiTest {
    @get:Rule val compose = createComposeRule()

    private fun account(id: String = "wallet-1", name: String = "Wallet", archived: Boolean = false) =
        AccountBalance(id, name, "EGP", 2, 10000L, archived, 1L, 10000L)

    private fun category(id: String = "food", name: String = "Food", archived: Boolean = false) =
        CategoryEntity(id, name, "EXPENSE", null, archived, 1L)

    @Test
    fun accountCanBeRenamed() {
        var id = ""
        var newName = ""
        compose.setContent {
            MaterialTheme {
                AccountManagerDialog(
                    listOf(account()), "en", { _, _, _, _ -> },
                    { accountId, name -> id = accountId; newName = name },
                    {}, {}
                )
            }
        }
        compose.onNodeWithTag("account_edit_wallet-1").performClick()
        compose.onNodeWithTag("account_edit_name").performTextReplacement("Travel cash")
        compose.onNodeWithTag("account_save_edit").performClick()
        compose.runOnIdle {
            assertEquals("wallet-1", id)
            assertEquals("Travel cash", newName)
        }
    }

    @Test
    fun accountArchiveNeedsConfirmation() {
        var archived = ""
        compose.setContent {
            MaterialTheme {
                AccountManagerDialog(
                    listOf(account()), "en", { _, _, _, _ -> }, { _, _ -> },
                    { archived = it }, {}
                )
            }
        }
        compose.onNodeWithTag("account_archive_wallet-1").performClick()
        compose.runOnIdle { assertEquals("", archived) }
        compose.onNodeWithTag("account_archive_confirm").performClick()
        compose.runOnIdle { assertEquals("wallet-1", archived) }
    }

    @Test
    fun defaultCashCannotBeArchived() {
        compose.setContent {
            MaterialTheme {
                AccountManagerDialog(
                    listOf(account("default-cash", "Cash")), "en", { _, _, _, _ -> },
                    { _, _ -> }, {}, {}
                )
            }
        }
        assertTrue(compose.onAllNodesWithTag("account_archive_default-cash").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Cash").fetchSemanticsNode()
    }

    @Test
    fun archivedAccountRemainsVisibleWithoutEditOrArchiveActions() {
        compose.setContent {
            MaterialTheme {
                AccountManagerDialog(
                    listOf(account(archived = true)), "en", { _, _, _, _ -> },
                    { _, _ -> }, {}, {}
                )
            }
        }
        compose.onNodeWithText("Archived").fetchSemanticsNode()
        assertTrue(compose.onAllNodesWithTag("account_edit_wallet-1").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithTag("account_archive_wallet-1").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun categoryCanBeRenamed() {
        var id = ""
        var newName = ""
        compose.setContent {
            MaterialTheme {
                CategoryManagerDialog(
                    listOf(category()), "en", { _, _, _ -> },
                    { categoryId, name -> id = categoryId; newName = name },
                    {}, {}
                )
            }
        }
        compose.onNodeWithTag("category_edit_food").performClick()
        compose.onNodeWithTag("category_edit_name").performTextReplacement("Groceries")
        compose.onNodeWithTag("category_save_edit").performClick()
        compose.runOnIdle {
            assertEquals("food", id)
            assertEquals("Groceries", newName)
        }
    }

    @Test
    fun categoryArchiveNeedsConfirmation() {
        var archived = ""
        compose.setContent {
            MaterialTheme {
                CategoryManagerDialog(
                    listOf(category()), "en", { _, _, _ -> }, { _, _ -> },
                    { archived = it }, {}
                )
            }
        }
        compose.onNodeWithTag("category_archive_food").performClick()
        compose.runOnIdle { assertEquals("", archived) }
        compose.onNodeWithTag("category_archive_confirm").performClick()
        compose.runOnIdle { assertEquals("food", archived) }
    }

    @Test
    fun archivedCategoryRemainsVisibleWithoutManagementActions() {
        compose.setContent {
            MaterialTheme {
                CategoryManagerDialog(
                    listOf(category(archived = true)), "en", { _, _, _ -> },
                    { _, _ -> }, {}, {}
                )
            }
        }
        compose.onNodeWithText("Archived").fetchSemanticsNode()
        assertTrue(compose.onAllNodesWithTag("category_edit_food").fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithTag("category_archive_food").fetchSemanticsNodes().isEmpty())
    }
}
