package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SchedulableFlow
@InitiatingFlow
public class EndAuctionInitiator extends FlowLogic<SignedTransaction>{

    private final UUID auctionId;

    public EndAuctionInitiator(UUID auctionId) {
        this.auctionId = auctionId;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        List<StateAndRef<AuctionState>> auctionStateAndRefs = getServiceHub().getVaultService().queryBy(AuctionState.class)
                .getStates();

        StateAndRef<AuctionState> inputStateAndRef = auctionStateAndRefs.stream().filter(auctionStateAndRef -> {
            AuctionState auctionState = auctionStateAndRef.getState().getData();
            return auctionState.getAuctionId().toString().equals(this.auctionId.toString());
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));

        Party notary = inputStateAndRef.getState().getNotary();
        AuctionState inputState = inputStateAndRef.getState().getData();

        if(getOurIdentity().getName().toString().equals(inputState.getAuctioner().getName().toString())) {
            AuctionState outputState = new AuctionState(inputState.getAuctionId(), inputState.getBasePrice(),
                    inputState.getCurrentHighestBid(), inputState.getCurrentHighestBidder(), inputState.getBidEndTime(),
                    inputState.getCurrentHighestBid(), false, inputState.getAuctioner(), inputState.getBidders(),
                    inputState.getCurrentHighestBidder());

            TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState)
                    .addCommand(new AuctionContract.Commands.EndAuction(), getOurIdentity().getOwningKey());

            transactionBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            List<FlowSession> bidderSessions = new ArrayList<>();
            for (Party bidder : inputState.getBidders())
                bidderSessions.add(initiateFlow(bidder));
            return subFlow(new FinalityFlow(signedTransaction, bidderSessions));
        }else{
            return null;
        }
    }
}