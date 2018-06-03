package net.corda.examples.obligation

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.driver
import net.corda.testing.node.User

fun main(args: Array<String>) {
    val user = User("user1", "test", permissions = setOf())

    driver(isDebug = true, startNodesInProcess = true, waitForAllNodesToFinish = true) {
        val (nodeA, nodeB, nodeC) = listOf(
                startNode(providedName = CordaX500Name("Citibank", "London", "GB"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("Barclays", "New York", "US"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("HSBC", "Paris", "FR"), rpcUsers = listOf(user)),
                startNode(providedName = CordaX500Name("Observer", "Paris", "FR"), rpcUsers = listOf(user))
        ).
        map { it.getOrThrow() }

        startWebserver(nodeA)
        startWebserver(nodeB)
        startWebserver(nodeC)
    }
}