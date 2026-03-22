package com.appdist.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        every { dataStore.data } returns flowOf(mutablePreferencesOf())
        assertNull(tokenManager.getAccessToken())
    }

    @Test
    fun `getAccessToken returns stored token`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("access_token") to "tok123")
        every { dataStore.data } returns flowOf(prefs)
        assertEquals("tok123", tokenManager.getAccessToken())
    }

    @Test
    fun `getRefreshToken returns stored refresh token`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("refresh_token") to "ref456")
        every { dataStore.data } returns flowOf(prefs)
        assertEquals("ref456", tokenManager.getRefreshToken())
    }

    @Test
    fun `isAuthenticated emits false when no token`() = runTest {
        every { dataStore.data } returns flowOf(mutablePreferencesOf())
        tokenManager.isAuthenticated.test {
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `isAuthenticated emits true when access token present`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("access_token") to "tok123")
        every { dataStore.data } returns flowOf(prefs)
        tokenManager.isAuthenticated.test {
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }
}
