package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.states.VehicleState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

/* New Flow Added to IssueChallan for Traffic Law Violation */
public class IssueChallanFlow {

    private IssueChallanFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class IssueChallanInitiatorFlow extends FlowLogic<SignedTransaction>{

        private final String redgNumber;
        // RTO required to fetch vehicle information since its not available with Police party
        private final Party rto;
        private final int challanValue;

        // Progress Tracker Steps
        private final ProgressTracker.Step RECEIVE_INPUT_STATE = new ProgressTracker.Step("Receiving InputState And Transaction from Counterparty");
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying Contract");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with private key.");
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // Progress Tracker to display flow progress on console.
        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVE_INPUT_STATE,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        public IssueChallanInitiatorFlow(String redgNumber, Party rto, int challanValue) {
            this.redgNumber = redgNumber;
            this.rto = rto;
            this.challanValue = challanValue;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            progressTracker.setCurrentStep(RECEIVE_INPUT_STATE);
            // Initiate a flow session to receive Vehicle State from RTO.
            FlowSession rtoSession = initiateFlow(rto);

            // Flag to be used between rto session and other party session in the counterparty flow,
            // such that only RTO executes the logic to send Vehicle State information.
            rtoSession.send(true);

            if(rtoSession.getCounterpartyFlowInfo().getFlowVersion() != 1){
                throw new FlowException("All Parties not upgraded to recent version of the app");
            }

            // Send registration number and receive the corresponding StateAndRef information
            StateAndRef inputStateAndRef = rtoSession.sendAndReceive(StateAndRef.class, redgNumber).unwrap(it->it);
            VehicleState inputState = (VehicleState) inputStateAndRef.getState().getData();

            // Receive the corresponding transactions of the state, to verify evolution of the state while running against the contract.
            // The counter-party must call the SendTransactionFlow corresponding to this ReceiveTransactionFlow.
            subFlow(new ReceiveTransactionFlow(rtoSession, true, StatesToRecord.ONLY_RELEVANT));

            // Fetch the notary tracking the state.
            Party notary = inputStateAndRef.getState().getNotary();


            // Initiate a flow session with vehicle owner.
            FlowSession ownerSession = initiateFlow(inputState.getOwner());

            if(ownerSession.getCounterpartyFlowInfo().getFlowVersion() != 1){
                throw new FlowException("All Parties not upgraded to recent version of the app");
            }
            //Flag to identify non-RTO session such that the logic to send Vehicle State information is not executed.
            ownerSession.send(false);

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //Construct the output state with the challanValue and challanIssuer.
            final VehicleState outputState = new VehicleState(inputState.getRedgNumber(),
                    inputState.getOwner(), inputState.getRto(), challanValue, getOurIdentity());

            //Build and instance of the command with the required signers.
            final Command<VehicleContract.Commands.IssueChallan> issueChallanCommand =
                    new Command<>(
                            new VehicleContract.Commands.IssueChallan(),
                            ImmutableList.of(getOurIdentity().getOwningKey())
                    );

            // Build the TransactionBuilder.
            final TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState)
                    .addCommand(issueChallanCommand);

            // Verify the transaction, this calls the contract verify method.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            transactionBuilder.verify(getServiceHub());

            // Self sign the transaction
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction selfSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

            // Call finality to flow to notarise and distribute the updated state to all the parties.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(selfSignedTx, ImmutableList.of(ownerSession, rtoSession), FINALISING_TRANSACTION.childProgressTracker()));
        }
    }

    @InitiatedBy(IssueChallanInitiatorFlow.class)
    public static class IssueChallanResponderFlow extends FlowLogic<SignedTransaction>{
        private final FlowSession counterpartySession;

        public IssueChallanResponderFlow(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Receive flag to identify rto party.
            Boolean isRto = counterpartySession.receive(Boolean.class).unwrap(it->it);
            // If rto execute logic to send vehicle state and transaction information.
            if(isRto){
                // Receive registartion number from RTO session.
                String redgNumber = counterpartySession.receive(String.class).unwrap(it->it);

                //Query the vault to fetch vehicle stateAndRef information.
                List<StateAndRef<VehicleState>> stateStateAndRef = getServiceHub().getVaultService().queryBy(VehicleState.class).getStates();
                StateAndRef<VehicleState> inputStateAndRef = stateStateAndRef.stream().filter(vehicleStateAndRef -> {
                    VehicleState vehicleState = vehicleStateAndRef.getState().getData();
                    return vehicleState.getRedgNumber().equals(redgNumber);
                }).findAny().orElseThrow(() -> new IllegalArgumentException("Vehicle Not Found"));

                //Send Vehicle stateAndRef to police.
                counterpartySession.send(inputStateAndRef);

                //Send state transaction to  police.
                subFlow(new SendTransactionFlow(counterpartySession, getServiceHub().getValidatedTransactions().getTransaction(inputStateAndRef.getRef().getTxhash())));
            }

            // call receive finality flow to receive the state information.
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
