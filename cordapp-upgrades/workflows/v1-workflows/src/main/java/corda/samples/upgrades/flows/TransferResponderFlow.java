package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(TransferInitiatorFlow.class)
public class TransferResponderFlow extends FlowLogic<SignedTransaction> {
    private FlowSession counterpartySession;

    public TransferResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // call receive finality flow to receive the notarised transaction and updated state.
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
