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
                      val lenders:ArrayList<Party>,
                      val borrower: AbstractParty,
                      val issueName:String,
                      val status:String,
                      val paid: Amount<Currency> = Amount(0, issueSize.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants: List<AbstractParty> get() {
        //listOf(lender, borrower)
          val list1 = mutableListOf(borrower);
        list1.addAll(lenders);

        return list1.toList()
    }
    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid + amountToPay)
   fun withNewLender(newLender: AbstractParty) = copy(borrower = newLender)
   fun withoutLender() = copy(borrower = NullKeys.NULL_PARTY)
    fun updateparticipant (newparticipant:Party):ArrayList<Party> {
        lenders.add(newparticipant)
        return lenders;
    }
    override fun toString(): String {
        val lenderString = (lenders.get(0) as? Party)?.name?.organisation ?: lenders.get(0).owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Obligation($linearId): $borrowerString owes $lenderString $issueSize and has paid $paid so far."
    }
}