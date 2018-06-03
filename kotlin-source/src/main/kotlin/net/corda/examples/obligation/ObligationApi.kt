package net.corda.examples.obligation

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.examples.obligation.flows.IssueObligation
import net.corda.examples.obligation.flows.OrderFlow
import net.corda.examples.obligation.flows.SettleObligation
import net.corda.examples.obligation.flows.TransferObligation
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalances
import net.corda.finance.flows.CashIssueFlow
import java.util.*
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

@Path("syncord")
class ObligationApi(val rpcOps: CordaRPCOps) {

    private val myIdentity = rpcOps.nodeInfo().legalIdentities.first()

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun me() = mapOf("me" to myIdentity)

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun peers() = mapOf("peers" to rpcOps.networkMapSnapshot()
            .filter { nodeInfo -> nodeInfo.legalIdentities.first() != myIdentity }
            .map { it.legalIdentities.first().name.organisation })

    @GET
    @Path("issues")
    @Produces(MediaType.APPLICATION_JSON)
    fun obligations() = rpcOps.vaultQuery(Obligation::class.java).states

    @GET
    @Path("orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun cash() = rpcOps.vaultQuery(Order::class.java).states

    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCashBalances() = rpcOps.getCashBalances()

    @GET
    @Path("self-issue-cash")
    fun selfIssueCash(@QueryParam(value = "issueSize") issueSize: Int,
                      @QueryParam(value = "currency") currency: String): Response {

        // 1. Prepare issue request.
        val issueAmount = Amount(issueSize.toLong() * 100, Currency.getInstance(currency))
        val notary = rpcOps.notaryIdentities().firstOrNull() ?: throw IllegalStateException("Could not find a notary.")
        val issueRef = OpaqueBytes.of(0)
        val issueRequest = CashIssueFlow.IssueRequest(issueAmount, issueRef, notary)

        // 2. Start flow and wait for response.
        val (status, message) = try {
            val flowHandle = rpcOps.startFlowDynamic(CashIssueFlow::class.java, issueRequest)
            val result = flowHandle.use { it.returnValue.getOrThrow() }
            CREATED to result.stx.tx.outputs.single().data
        } catch (e: Exception) {
            BAD_REQUEST to e.message
        }

        // 3. Return the response.
        return Response.status(status).entity(message).build()
    }

    @GET
    @Path("createIssue")
    @Produces(MediaType.APPLICATION_JSON)
    fun issueObligation(@QueryParam(value = "issuer") issuer: String,
                        @QueryParam(value = "issueSize") issueSize: Int,
                        @QueryParam(value = "party") party: String,
                        @QueryParam(value = "issueName") issueName: String): Response {
        // 1. Get party objects for the counterparty.
        val lenderIdentity = rpcOps.partiesFromName("Observer", exactMatch = false).singleOrNull()
                ?: throw IllegalStateException("Couldn't lookup node identity for $party.")

        // 2. Create an amount object.
        val issuestatus = "DRAFT"
        val currency = "USD"
        val issueAmount = Amount(issueSize.toLong() * 100, Currency.getInstance(currency))
        
        // 3. Start the IssueObligation flow. We block and wait for the flow to return.
        val (status, message) = try {
            val flowHandle = rpcOps.startFlowDynamic(
                    IssueObligation.Initiator::class.java,
                    issueAmount,
                    lenderIdentity,
                    issueName,
                    issuestatus,
                    party,
                    issuer,
                    false
            )

            val result = flowHandle.use { it.returnValue.getOrThrow() }
            CREATED to "Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single().data}"
        } catch (e: Exception) {
            BAD_REQUEST to e.message
        }

        // 4. Return the result.
        return Response.status(status).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Credentials", "true").header("Access-Control-Allow-Headers","origin, content-type, accept, authorization").header("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS, HEAD").entity(message).build()    
        }
    @GET
    @Path("createOrder")
    @Produces(MediaType.APPLICATION_JSON)
    fun issueObligation(@QueryParam(value = "amount") amount: Int,
                        @QueryParam(value = "party") party: String,
                        @QueryParam(value = "issueId") issueId: String,
                        @QueryParam(value = "issueName") issueName: String,
                        @QueryParam(value = "investorName") investorName: String,
                        @QueryParam(value = "book") book: String,
                        @QueryParam(value = "country") country: String): Response {
        // 1. Get party objects for the counterparty.
         val lenderIdentity = rpcOps.partiesFromName(party, exactMatch = false).singleOrNull()
             ?: throw IllegalStateException("Couldn't lookup node identity for $party.")
        val status1 = "Shared"
        val currency = "USD"
        // 2. Create an amount object.
        val issueAmount = Amount(amount.toLong() * 100, Currency.getInstance(currency))
        //val linearId = UniqueIdentifier.fromString(issueId)
        // 3. Start the IssueObligation flow. We block and wait for the flow to return.
        val (status, message) = try {
            val flowHandle = rpcOps.startFlowDynamic(
                    OrderFlow.Initiator::class.java,
                    issueAmount,
                    issueId,
                    issueName,
                    status1,
                    book,
                    country,
                    investorName,
                    lenderIdentity
            )
            val result = flowHandle.use { it.returnValue.getOrThrow() }
            CREATED to "Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single().data}"
        } catch (e: Exception) {
            BAD_REQUEST to e.message
        }

        // 4. Return the result.
        return Response.status(status).entity(message).build()
    }
    @GET
    @Path("updateStatus")
    fun transferObligation(@QueryParam(value = "id") id: String,
                           @QueryParam(value = "party") party: String): Response {
        val linearId = UniqueIdentifier.fromString(id)
        val newLender = rpcOps.partiesFromName(party, exactMatch = false).singleOrNull()
                ?: throw IllegalStateException("Couldn't lookup node identity for $party.")

        val (status, message) = try {
            val flowHandle = rpcOps.startFlowDynamic(
                    TransferObligation.Initiator::class.java,
                    linearId,
                    newLender,
                    true
            )

            flowHandle.use { flowHandle.returnValue.getOrThrow() }
            CREATED to "Obligation $id transferred to $party."
        } catch (e: Exception) {
            BAD_REQUEST to e.message
        }

        return Response.status(status).entity(message).build()
    }

    @GET
    @Path("settle-obligation")
    fun settleObligation(@QueryParam(value = "id") id: String,
                         @QueryParam(value = "issueSize") issueSize: Int,
                         @QueryParam(value = "currency") currency: String): Response {
        val linearId = UniqueIdentifier.fromString(id)
        val settleissueSize = Amount(issueSize.toLong() * 100, Currency.getInstance(currency))

        val (status, message) = try {
            val flowHandle = rpcOps.startFlowDynamic(
                    SettleObligation.Initiator::class.java,
                    linearId,
                    settleissueSize,
                    true
            )

            flowHandle.use { flowHandle.returnValue.getOrThrow() }
            CREATED to "$issueSize $currency paid off on obligation id $id."
        } catch (e: Exception) {
            BAD_REQUEST to e.message
        }

        return Response.status(status).entity(message).build()
    }
}