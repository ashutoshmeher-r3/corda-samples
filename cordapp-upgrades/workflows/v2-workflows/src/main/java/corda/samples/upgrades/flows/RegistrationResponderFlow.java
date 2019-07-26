package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

// ******************
// * RegistrationResponderFlow flow *
// ******************
@InitiatedBy(RegistrationInitiatorFlow.class)
public class RegistrationResponderFlow extends FlowLogic<SignedTransaction> {
    private FlowSession counterpartySession;

    public RegistrationResponderFlow(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // call receive finality flow to receive the state information.
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
