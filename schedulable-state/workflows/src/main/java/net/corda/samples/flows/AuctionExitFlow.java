package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.states.AuctionState;

import java.util.List;
import java.util.UUID;

public class AuctionConsumptionFlow {

    private AuctionConsumptionFlow(){}

    @StartableByRPC
    @StartableByService
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private UUID auctionId;

        public Initiator(UUID auctionId) {
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> auctionStateAndRef = auntionStateAndRefs.stream().filter(stateAndRef -> {
                AuctionState auctionState = stateAndRef.getState().getData();
                return auctionState.getAuctionId().toString().equals(auctionId);
            }).findAny().orElseThrow(() -> new FlowException("Auction Not Found"));
            AuctionState auctionState = auctionStateAndRef.getState().getData();

            TransactionBuilder transactionBuilder = new TransactionBuilder(auctionStateAndRef.getState().getNotary())
                    .addInputState(auctionStateAndRef)
                    .addCommand();

            return null;
        }
    }
//
//    @InitiatedBy(Initiator.class)
//    public static class Responder extends FlowLogic<SignedTransaction>{
//
//
//        @Override
//        @Suspendable
//        public SignedTransaction call() throws FlowException {
//            return null;
//        }
//    }
//}
