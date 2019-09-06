package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreateAuctionFlow {

    private CreateAuctionFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class CreateAuctionInitiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker progressTracker = new ProgressTracker();

        private final Long basePrice;
        private final List<Party> bidders;

        public CreateAuctionInitiator(Long basePrice, List<Party> bidders) {
            this.basePrice = basePrice;
            this.bidders = bidders;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party auctioner = getOurIdentity();

            // Output state
            AuctionState auctionState = new AuctionState(UUID.randomUUID(), basePrice, null,
                    null, Instant.now().plusSeconds(180), null, true, auctioner,
                    bidders, null);

            // Build the transaction
            TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(auctionState)
                .addCommand(new AuctionContract.Commands.CreateAuction(), ImmutableList.of(auctioner.getOwningKey()));

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow
            List<FlowSession> bidderSessions = new ArrayList<>();
            for(Party bidder: bidders)
                bidderSessions.add(initiateFlow(bidder));

            return subFlow(new FinalityFlow(selfSignedTransaction, ImmutableList.copyOf(bidderSessions)));
        }
    }

    @InitiatedBy(CreateAuctionInitiator.class)
    public static class CreateAuctionResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public CreateAuctionResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
