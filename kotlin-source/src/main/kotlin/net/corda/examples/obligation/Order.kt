package net.corda.examples.obligation

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.utilities.toBase58String
import java.util.*

data class Order(val amount: Amount<Currency>,
                      val lender: AbstractParty,
                      val borrower: AbstractParty,
                      val issueId:String,
                      val issueName:String,
                      val investorName:String,
                      val orderStatus:String,
                      val book:String,
                      val country:String,
                      val orderId:String,
                      val allocatedAmount: Amount<Currency> = Amount(0, amount.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    fun pay(amountToPay: Amount<Currency>) = copy(allocatedAmount = allocatedAmount + amountToPay)
    fun withNewLender(newLender: AbstractParty) = copy(borrower = newLender)
    fun withoutLender() = copy(borrower = NullKeys.NULL_PARTY)

    override fun toString(): String {
        val lenderString = (lender as? Party)?.name?.organisation ?: lender.owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Obligation($linearId): $borrowerString owes $lenderString $amount and has paid $allocatedAmount so far."
    }
}