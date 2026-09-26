package flashman.einundzwanzig.widget

import org.junit.Assert.assertEquals
import org.junit.Test

// Keeps the test task wired up in CI until the data layer tests arrive in phase 2
class SmokeTest {
    @Test
    fun arithmetic() = assertEquals(21, 3 * 7)
}
