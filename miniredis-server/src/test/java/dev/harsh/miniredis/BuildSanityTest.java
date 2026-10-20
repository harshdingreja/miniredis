package dev.harsh.miniredis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 0 canary test. It proves the build pipeline works end to end: JUnit is on the test
 * classpath, Surefire finds and runs tests, and CI executes them on a real JDK 21.
 */
class BuildSanityTest {

    @Test
    void runsOnJava21OrNewer() {
        int javaVersion = Runtime.version().feature();
        assertTrue(javaVersion >= 21, "MiniRedis targets Java 21+, but tests ran on Java " + javaVersion);
    }
}
