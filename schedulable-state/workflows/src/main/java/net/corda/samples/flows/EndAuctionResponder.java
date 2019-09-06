package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(EndAuctionInitiator.class)
public class EndAuctionResponder extends FlowLogic<SignedTransaction> {

    private FlowSession counterpartySession;

    public EndAuctionResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
