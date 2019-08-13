package io.bluebank.braid.server.flow

import net.corda.core.flows.ContractUpgradeFlow
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test

class StarableByRPCFinderTest {

    @Test
    fun shouldfindClassRPCClass() {
        var classes = StartableByRPCFinder().findStartableByRPC();
        assertThat(classes.size, greaterThan(2))
        assertThat(classes, hasItem(ContractUpgradeFlow.Authorise::class.java))
    }

    @Test
    fun shouldfindConstructor() {
        var classes = StartableByRPCFinder().findStartableByRPC();
        assertThat(classes.size, greaterThan(2))
        assertThat(classes,hasItem(ContractUpgradeFlow.Authorise::class.java))
    }
}