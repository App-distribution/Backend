package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TokenManagerTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setUp() {
        dataStore = mockk(relaxed = true)
        tokenManager = TokenManager(dataStore)
    }

    @Test
    fun `getAccessToken returns null when no token stored`() = runTest {
        val prefs = mutablePreferencesOf()
        every { dataStore.data } returns flowOf(prefs)
        assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun `isAuthenticated emits false when no token`() = runTest {
        val prefs = mutablePreferencesOf()
        every { dataStore.data } returns flowOf(prefs)
        val result = mutableListOf<Boolean>()
        tokenManager.isAuthenticated.collect { result.add(it); return@collect }
        assertEquals(false, result.first())
    }
}
