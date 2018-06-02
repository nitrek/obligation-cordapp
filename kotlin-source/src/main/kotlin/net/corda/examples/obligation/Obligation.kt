package net.corda.examples.obligation

import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.utilities.toBase58String
import java.util.*

data class Obligation(val issueSize: Amount<Currency>,
                      val lender: AbstractParty,
                      val borrower: AbstractParty,
                      val observer: AbstractParty,
                      val issueName:String,
                      val status:String,
                      val paid: Amount<Currency> = Amount(0, issueSize.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower,observer)

    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid + amountToPay)
    fun withNewLender(newLender: AbstractParty) = copy(lender = newLender)
    fun withoutLender() = copy(lender = NullKeys.NULL_PARTY)

    override fun toString(): String {
        val lenderString = (lender as? Party)?.name?.organisation ?: lender.owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Obligation($linearId): $borrowerString owes $lenderString $issueSize and has paid $paid so far."
    }
}