package com.creazzy.rofof.data.local

/** Projection for the four most-used active expense categories. */
data class FrequentCategory(
    val categoryId: String,
    val categoryName: String,
    val transactionCount: Long
)
