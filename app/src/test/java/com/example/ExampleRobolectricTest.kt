package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FocusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("System Focus HUD", appName)
    }

    @Test
    fun `reward token calculation enforces 5 minute threshold`() {
        assertEquals(0, FocusRepository.calculateTokensForFocusMinutes(0))
        assertEquals(0, FocusRepository.calculateTokensForFocusMinutes(4))
        assertTrue(FocusRepository.calculateTokensForFocusMinutes(5) >= 10)
        assertEquals(100, FocusRepository.calculateTokensForFocusMinutes(35))
    }
}
