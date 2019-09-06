package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.util.ArrayList;
import java.util.List;

public class BidFlow {

    private BidFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class BidInitiator extends FlowLogic<SignedTransaction>{

        private final Long bidAmount;
        private final String auctionId;

        public BidInitiator(Long bidAmount, String auctionId) {
            this.bidAmount = bidAmount;
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState state, and filter the results based on the policyNumber
            // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
            // transaction.
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
                AuctionState auctionState = auctionStateAndRef.getState().getData();
                return auctionState.getAuctionId().toString().equals(auctionId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));


            AuctionState input = inputStateAndRef.getState().getData();


            //Create the output state
            AuctionState output = new AuctionState(input.getAuctionId(), input.getBasePrice(), bidAmount,
                    getOurIdentity(), input.getBidEndTime(), null, true, input.getAuctioner(),
                    input.getBidders(), null);

            // Build the transaction
            TransactionBuilder builder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new AuctionContract.Commands.Bid(), ImmutableList.of(getOurIdentity().getOwningKey()));

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow
            List<FlowSession> allSessions = new ArrayList<>();
            List<Party> bidders = new ArrayList<>(input.getBidders());
            bidders.remove(getOurIdentity());
            for(Party bidder: bidders)
                allSessions.add(initiateFlow(bidder));

            allSessions.add(initiateFlow(input.getAuctioner()));
            return subFlow(new FinalityFlow(selfSignedTransaction, ImmutableList.copyOf(allSessions)));
        }
    }

    @InitiatedBy(BidInitiator.class)
    public static class BidResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public BidResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}