package org.ljcode.myPlugin.tests;

import org.junit.jupiter.api.Test;
import org.ljcode.myPlugin.listeners.UpdateCheckerListener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionComparisonTest {

    @Test
    void detectsNewerVersions() {
        assertTrue(UpdateCheckerListener.isNewerVersion("1.2.1", "1.3.0"));
        assertTrue(UpdateCheckerListener.isNewerVersion("1.2.1", "1.10.0"));
        assertTrue(UpdateCheckerListener.isNewerVersion("1.2.1", "v2.0.0"));
        assertFalse(UpdateCheckerListener.isNewerVersion("1.3.0", "1.3.0"));
        assertFalse(UpdateCheckerListener.isNewerVersion("1.3.1", "1.3.0"));
    }
}
