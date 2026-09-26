package flashman.einundzwanzig.widget.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun newer() = assertTrue(UpdateChecker.isNewer("1.0.0", "0.4.0"))
    @Test fun `newer patch`() = assertTrue(UpdateChecker.isNewer("v0.4.1", "0.4.0"))
    @Test fun same() = assertFalse(UpdateChecker.isNewer("0.4.0", "0.4.0"))
    @Test fun older() = assertFalse(UpdateChecker.isNewer("0.3.9", "0.4.0"))
    @Test fun `debug suffix is ignored`() = assertFalse(UpdateChecker.isNewer("0.4.0", "0.4.0-debug"))
    @Test fun `numeric not lexical`() = assertTrue(UpdateChecker.isNewer("0.10.0", "0.9.0"))
}
