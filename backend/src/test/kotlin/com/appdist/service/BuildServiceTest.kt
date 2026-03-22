package com.appdist.service

import com.appdist.TestDatabase
import com.appdist.domain.service.NotificationService
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class BuildServiceTest {
    private val mockNotificationService = mockk<NotificationService>(relaxed = true)

    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    @Test
    fun `notifyNewBuild is not called before any upload`() = runTest {
        coVerify(exactly = 0) { mockNotificationService.notifyNewBuild(any(), any(), any()) }
    }
}
