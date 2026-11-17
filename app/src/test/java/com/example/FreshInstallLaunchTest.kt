package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurePrefs
import com.example.notification.NotificationHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = SamadApplication::class, qualifiers = "w411dp-h915dp")
class FreshInstallLaunchTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstall_launchesLoginScreenWithoutCrashing() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("username_input").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("password_input").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertExists().performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(sdk = [24])
    fun freshInstall_minSdk24_launchesWithoutJavaTimeCrash() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("username_input").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertExists()
    }

    @Test
    fun freshInstall_concurrentSecurePrefsInit_isSafe() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = Executors.newFixedThreadPool(4)
        val tasks = (1..8).map { idx ->
            Callable {
                val prefs = SecurePrefs(context)
                assertFalse(prefs.isTokenValid())
                assertNotNull(prefs.baseUrl)
                prefs.autoCheckUpdates = (idx % 2 == 0)
                prefs
            }
        }
        val futures = executor.invokeAll(tasks)
        futures.forEach { it.get() }
        executor.shutdown()
    }

    @Test
    fun freshInstall_notificationHelpersDoNotCrashWithoutPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationHelper.showAppUpdateNotification(
            context = context,
            version = "1.6.2",
            changelog = "رفع مشکل بسته شدن برنامه در نصب جدید",
            downloadUrl = "https://github.com/valiolla77-wq/samadQR/releases"
        )
        NotificationHelper.showMealReminderNotification(
            context = context,
            notificationId = 101,
            mealType = "ناهار",
            servingTime = "12:00",
            foodName = "چلوکباب",
            selfName = "سلف مرکزی",
            code = "123456"
        )
    }
}
