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

        // Check the version of the flow installed on all the counter-parties and decide whether to sign the transaction.
        if(counterpartySession.getCounterpartyFlowInfo().getFlowVersion() == 2 && counterpartySession.receive(Boolean.class).unwrap(it->it)) {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    // Implement responder flow transaction checks here
                }
            });
        }
        // call receive finality flow to receive the state information.
        return subFlow(new ReceiveFinalityFlow(counterpartySession));
    }
}
