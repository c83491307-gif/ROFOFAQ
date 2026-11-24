package com.creazzy.rofof.feature.awareness

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RuleLoaderTest {
    @Test fun loadsBundledConfigurationAndAllTwelveRules() {
        val configuration = RuleLoader().load(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(16, configuration.categories.size)
        assertEquals(12, configuration.rules.size)
        assertTrue(configuration.keywords.isNotEmpty())
        assertEquals("budget-exceeded-100", configuration.rules.first().id)
    }

    @Test fun rejectsUnknownCategoryReferenceInKeywordMap() {
        val categories = """{"categories":[{"id":"food","name":"Food","transactionType":"EXPENSE"}]}"""
        val keywords = """{"keywords":[{"keyword":"اكل","categoryId":"missing","transactionType":"EXPENSE"}]}"""
        val rules = """{"rules":[{"id":"r1","enabled":true,"priority":1,"conditions":[{"fact":"x","operator":"eq","value":1}],"actions":[{"type":"NOTICE","severity":"INFO","messageAr":"م","messageEn":"m"}]}]}"""
        assertTrue(runCatching { RuleLoader().parse(categories, keywords, rules) }.isFailure)
    }

    @Test fun rejectsUnsupportedOperatorsAndDuplicateRuleIds() {
        val categories = """{"categories":[]}"""
        val keywords = """{"keywords":[]}"""
        val invalidOperator = """{"rules":[{"id":"r1","conditions":[{"fact":"x","operator":"approx","value":1}],"actions":[{"type":"x","severity":"INFO","messageAr":"x","messageEn":"x"}]}]}"""
        assertTrue(runCatching { RuleLoader().parse(categories, keywords, invalidOperator) }.isFailure)
        val duplicateIds = """{"rules":[
          {"id":"r1","conditions":[{"fact":"x","operator":"eq","value":1}],"actions":[{"type":"x","severity":"INFO","messageAr":"x","messageEn":"x"}]},
          {"id":"r1","conditions":[{"fact":"y","operator":"eq","value":1}],"actions":[{"type":"y","severity":"INFO","messageAr":"y","messageEn":"y"}]}
        ]}"""
        assertTrue(runCatching { RuleLoader().parse(categories, keywords, duplicateIds) }.isFailure)
    }
}
