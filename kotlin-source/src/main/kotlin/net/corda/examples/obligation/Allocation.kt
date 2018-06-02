package net.corda.examples.obligation

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.utilities.toBase58String
import java.util.*

data class Allocation(val allocatedAmount: Amount<Currency>,
                      val lender: AbstractParty,
                      val borrower: AbstractParty,
                      val linearOrderId: String,
                      val dealName: String,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    fun pay(allocatedAmount: Amount<Currency>) = copy(allocatedAmount = allocatedAmount + allocatedAmount)
    fun withNewLender(newLender: AbstractParty) = copy(lender = newLender)
    fun withoutLender() = copy(lender = NullKeys.NULL_PARTY)

    override fun toString(): String {
        val lenderString = (lender as? Party)?.name?.organisation ?: lender.owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Allocation($linearId): $borrowerString owes $lenderString and has paid $allocatedAmount so far for deal Name $dealName and order identifier $linearOrderId"
    }
}