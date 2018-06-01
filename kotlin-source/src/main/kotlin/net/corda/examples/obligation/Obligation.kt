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
                      val leadBanker: AbstractParty,
                      val coBanker: AbstractParty,
                      val issueName:String,
                      val isin:String,
                      val paid: Amount<Currency> = Amount(0, amount.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() = listOf(leadBanker,coBanker)

    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid + amountToPay)
    fun withNewLeadBanker(newLender: AbstractParty) = copy(leadBanker = newLender)
    fun withoutLeadBanker()) = copy(leadBanker = NullKeys.NULL_PARTY)

    override fun toString(): String {
        val leadBankerString = (leadBanker as? Party)?.name?.organisation ?: leadBanker.owningKey.toBase58String()
        val coBankerString = (coBanker as? Party)?.name?.organisation ?: coBanker.owningKey.toBase58String()
        return "Issue($linearId): with cobanker : $coBankerString and leadBanker $leadBankerString for $amount and paid $paid."
    }
}