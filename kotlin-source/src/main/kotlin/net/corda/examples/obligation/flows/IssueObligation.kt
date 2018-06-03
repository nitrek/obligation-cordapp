package net.corda.examples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.seconds
import net.corda.examples.obligation.Obligation
import net.corda.examples.obligation.ObligationContract
import net.corda.examples.obligation.ObligationContract.Companion.OBLIGATION_CONTRACT_ID
import java.security.PublicKey
import java.util.*

object IssueObligation {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val amount: Amount<Currency>,
                    private val lender: Party,
                    private val issueName:String,
                    private val status:String,
                    private val coBankers:ArrayList<Party>,
                    private val coBanker:String,
                    private val issuer:String,
                    private val anonymous: Boolean = true) : ObligationBaseFlow() {

        companion object {
            object INITIALISING : Step("Performing initial steps.")
            object BUILDING : Step("Building and verifying transaction.")
            object SIGNING : Step("Signing transaction.")
            object COLLECTING : Step("Collecting counterparty signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }
            object FINALISING : Step("Finalising transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING)
        }

        override val progressTracker: ProgressTracker = tracker()


        @Suspendable
        override fun call(): SignedTransaction {
            // Step 1. Initialisation.
            progressTracker.currentStep = INITIALISING
            val obligation = Obligation(issuer,amount, lender, ourIdentity,coBankers,coBanker,issueName,status)

            val signers = ArrayList<PublicKey>(5)

            signers.add(ourIdentity.owningKey)


            for (transferparty in coBankers) {
                signers.add(transferparty.owningKey)
            }

            val ourSigningKey = obligation.borrower.owningKey

            // Step 2. Building.
            progressTracker.currentStep = BUILDING
            val utx = TransactionBuilder(firstNotary)
                    .addOutputState(obligation, OBLIGATION_CONTRACT_ID)
                    .addCommand(ObligationContract.Commands.Issue(), signers)
                    .setTimeWindow(serviceHub.clock.instant(), 30.seconds)

            // Step 3. Sign the transaction.
            progressTracker.currentStep = SIGNING
            val ptx = serviceHub.signInitialTransaction(utx, ourSigningKey)

            // Step 4. Get the counter-party signature.
            progressTracker.currentStep = COLLECTING
            val lenderFlow = ArrayList<FlowSession>(5)

            for (transferparty in coBankers) {

                lenderFlow.add(initiateFlow(transferparty))

                println("Finished gathering signatures stage 9")
            }

            //val lenderFlow = initiateFlow(lender)
            val stx = subFlow(CollectSignaturesFlow(
                    ptx,
                    lenderFlow,
                   COLLECTING.childProgressTracker())
            )

            // Step 5. Finalise the transaction.
            progressTracker.currentStep = FINALISING
            return subFlow(FinalityFlow(stx, FINALISING.childProgressTracker()))
        }

        // @Suspendable
        // private fun createAnonymousObligation(): Obligation {
        //     val txKeys = subFlow(SwapIdentitiesFlow(lender))

        //     check(txKeys.size == 2) { "Something went wrong when generating confidential identities." }

        //     val anonymousMe = txKeys[ourIdentity] ?: throw FlowException("Couldn't create our conf. identity.")
        //     val anonymousLender = txKeys[lender] ?: throw FlowException("Couldn't create lender's conf. identity.")

        //     return Obligation(amount, anonymousLender, anonymousMe,"Observer","anon","invalid")
        // }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val otherFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val stx = subFlow(SignTxFlowNoChecking(otherFlow))
            return waitForLedgerCommit(stx.id)
        }
    }
}
