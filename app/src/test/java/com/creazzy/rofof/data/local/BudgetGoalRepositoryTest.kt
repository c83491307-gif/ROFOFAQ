       assertTrue(runCatching { plans.updateGoalProgress(id, -1L) }.isFailure)
    }

    @Test fun schemaVersionTenContainsBudgetGoalRecurringDebtAndLearningTables() = runBlocking {
        val tables = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('budgets', 'debt_repayments', 'debts', 'financial_goals', 'learned_keywords', 'recurring_drafts', 'recurring_rules') ORDER BY name"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("budgets", "debt_repayments", "debts", "financial_goals", "learned_keywords", "recurring_drafts", "recurring_rules"), tables)
        val version = database.openHelper.readableDatabase.query("PRAGMA user_version").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
        assertEquals(10, version)
    }
    @Test fun budgetAlertSnapshotUsesOnlyMatchingCategoryCurrencyAndPeriod() = runBlocking {
        val account = finance.createAccount("Main", "EGP", 2, 0L)
        val food = finance.createCategory("Food", "EXPENSE")
        val travel = finance.createCategory("Travel", "EXPENSE")
        val start = LocalDate.of(2026, 11, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 12, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        plans.createBudget(food, 10_000L, "EGP", 2, start, end)
        database.transactionDao().insert(
            TransactionEntity("alert-food", "Groceries", "EXPENS