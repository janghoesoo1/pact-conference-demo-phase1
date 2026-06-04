package com.conference.cfp.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["features.new-voting-algorithm=true", "features.detailed-logging=true"])
class FeatureFlagTest {
    @Autowired
    private lateinit var featureFlags: FeatureFlags

    @Test
    fun `feature flags are loaded from configuration`() {
        assertTrue(featureFlags.newVotingAlgorithm)
        assertTrue(featureFlags.detailedLogging)
    }
}

@SpringBootTest
class FeatureFlagDefaultTest {
    @Autowired
    private lateinit var featureFlags: FeatureFlags

    @Test
    fun `feature flags default to false`() {
        assertFalse(featureFlags.newVotingAlgorithm)
        assertFalse(featureFlags.detailedLogging)
    }
}
