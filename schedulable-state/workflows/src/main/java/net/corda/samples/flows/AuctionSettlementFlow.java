package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;
import net.corda.finance.contracts.asset.CashUtilities;
import net.corda.finance.flows.CashPaymentFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.samples.states.AuctionState;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class AuctionClearanceFlow {

    private AuctionClearanceFlow() {}

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private final UUID auctionId;
        private final Amount<Currency> payment;

        public Initiator(UUID auctionId, Amount<Currency> payment) {
            this.auctionId = auctionId;
            this.payment = payment;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> inputStateAndRef = auntionStateAndRefs.stream().filter(auctionStateAndRef -> {
                AuctionState auctionState = auctionStateAndRef.getState().getData();
                return auctionState.getAuctionId().toString().equals(auctionId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));
            AuctionState auctionState = inputStateAndRef.getState().getData();


            //subFlow(new CashPaymentFlow(payment, auctionState.getWinner(), false));

            return null;
        }
    }
}