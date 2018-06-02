package net.corda.examples.obligation

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.driver
import net.corda.testing.node.User

fun main(args: Array<String>) {
    val user = User("user1", "test", permissions = setOf())

    driver(isDebug = true, startNodesInProcess = true, waitForAllNodesToFinish = true) {
        val (nodeA, nodeB, nodeC,nodeD) = listOf(
                startNode(providedName = CordaX500Name("PartyA", "London", "GB"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("PartyB", "New York", "US"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("PartyC", "Paris", "FR"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("Observer", "Pune", "IN"), rpcUsers = listOf(user))
        ).map { it.getOrThrow() }

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
        startWebserver(nodeD)
    }
}