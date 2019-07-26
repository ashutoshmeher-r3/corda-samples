package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.states.VehicleState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

/* New Flow Added to PayChallan amount issued for Traffic Law Violation by IssueChallan Flow */
public class PayChallanFlow {

    private PayChallanFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class PayChallanInitiatorFlow extends FlowLogic<SignedTransaction>{

        // Challan Settlement Value.
        private final int value;
        private final String redgNumber;

        // Progress Tracker Steps
        private final ProgressTracker.Step RECEIVE_OUTPUT_STATE = new ProgressTracker.Step("Receiving OutputState from Counterparty");
        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying Contract");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with private key.");
        private final ProgressTracker.Step GATHERING_SIGNATURE = new ProgressTracker.Step("Collecting Signature from Counterparty");
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // Progress Tracker to display flow progress on console.
        private final ProgressTracker progressTracker = new ProgressTracker(
                RECEIVE_OUTPUT_STATE,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNATURE,
                FINALISING_TRANSACTION
        );

        public PayChallanInitiatorFlow(int value, String redgNumber) {
            this.value = value;
            this.redgNumber = redgNumber;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            progressTracker.setCurrentStep(RECEIVE_OUTPUT_STATE);
            // Query the vault for the vehicleState against the registration number.
            List<StateAndRef<VehicleState>> stateStateAndRef = getServiceHub().getVaultService().queryBy(VehicleState.class).getStates();
            StateAndRef<VehicleState> inputStateAndRef = stateStateAndRef.stream().filter(vehicleStateAndRef -> {
                VehicleState vehicleState = vehicleStateAndRef.getState().getData();
                return vehicleState.getRedgNumber().equals(redgNumber);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Vehicle Not Found"));
            VehicleState inputState = inputStateAndRef.getState().getData();

            // Fetch the notary tracking the state
            Party notary = inputStateAndRef.getState().getNotary();

            // Initiate flow session with police, to get the output state. The challan value adjustment must be done by the challanIssuer,
            // hence the initiator (who in this case is the owner) does not construct the output state but request it from the police (challan issuer)
            FlowSession policeSession = initiateFlow(inputState.getChallanIssuer());

            //Recieve the output state from police using ReceiveOutputFlow. ReceiveOutputFlow is a initiating flow implemented to communicate with the police
            //to receive the output vehicle state.
            final VehicleState outputState = subFlow(new ReceiveOutputFlow(inputState.getChallanIssuer(), value, redgNumber));

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Build the command instance with the required signers.
            final Command<VehicleContract.Commands.SettleChallan> settleChallanCommand =
                    new Command<>(
                            new VehicleContract.Commands.SettleChallan(),
                            ImmutableList.of(inputState.getChallanIssuer().getOwningKey(), inputState.getOwner().getOwningKey())
                    );

            // Build the transaction builder.
            final TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState)
                    .addCommand(settleChallanCommand);

            // Verify the transaction, this calls the contract's verify method.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            transactionBuilder.verify(getServiceHub());

            // Self sign the transaction.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction selfSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

            // Initiate flow session with the rto.
            FlowSession rtoSession = initiateFlow(inputState.getRto());

            // Flag to manage who should sign the transaction in the responder flow. Rto is not a required signer.
            rtoSession.send(false);
            policeSession.send(true);

            // Collect signature form police.
            progressTracker.setCurrentStep(GATHERING_SIGNATURE);
            SignedTransaction counterSignedTrnx = subFlow(new CollectSignaturesFlow(selfSignedTx, ImmutableList.of(policeSession)));

            // Call finality to flow to notarise and distribute the updated state to all the parties.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(counterSignedTrnx, ImmutableList.of(rtoSession, policeSession), FINALISING_TRANSACTION.childProgressTracker()));
        }
    }


    @InitiatedBy(PayChallanInitiatorFlow.class)
    public static class PayChallanResponderFlow extends FlowLogic<SignedTransaction>{
        private final FlowSession counterpartySession;

        public PayChallanResponderFlow(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            //Receive flag from counter-party to check if signature is required for this party
            boolean isSignRequired = counterpartySession.receive(Boolean.class).unwrap(flag -> flag);

            // If signature required sign the transaction
            if(isSignRequired){
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

    // Separate Initiating flow to receive output state for this transaction from police.
    @InitiatingFlow
    public static class ReceiveOutputFlow extends FlowLogic<VehicleState>{
        private final Party challanIssuer;
        private final int settlementValue;
        private final String redgNumber;

        public ReceiveOutputFlow(Party challanIssuer, int settlementValue, String redgNumber) {
            this.challanIssuer = challanIssuer;
            this.settlementValue = settlementValue;
            this.redgNumber = redgNumber;
        }

        @Override
        @Suspendable
        public VehicleState call() throws FlowException {

            // Start flow session with police.
            FlowSession policeSession = initiateFlow(challanIssuer);

            //Send the registration number and the settlementValue paid
            policeSession.send(redgNumber);
            policeSession.send(settlementValue);

            // Receive the output state from police and return to the calling flow.
            return  policeSession.receive(VehicleState.class).unwrap(value -> value);
        }
    }

    @InitiatedBy(ReceiveOutputFlow.class)
    public static class ReceiveOutputFlowResponder extends FlowLogic<Void>{
        private final FlowSession cpSession;

        public ReceiveOutputFlowResponder(FlowSession cpSession) {
            this.cpSession = cpSession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {

            //Receive registration number and the settlementValue paid from counter-party
            String redgNumber = cpSession.receive(String.class).unwrap(value -> value);
            int settlementValue = cpSession.receive(Integer.class).unwrap(value -> value);

            // Query vault to fetch the state information
            List<StateAndRef<VehicleState>> stateStateAndRef = getServiceHub().getVaultService().queryBy(VehicleState.class).getStates();
            StateAndRef<VehicleState> inputStateAndRef = stateStateAndRef.stream().filter(vehicleStateAndRef -> {
                VehicleState vehicleState = vehicleStateAndRef.getState().getData();
                return vehicleState.getRedgNumber().equals(redgNumber);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Vehicle Not Found"));
            VehicleState inputState = inputStateAndRef.getState().getData();

            // Check if the settlementValue paid is equal to the challanValue and send it back to the counter-party.
            if(settlementValue == inputState.getChallanValue()){
                cpSession.send(new VehicleState(inputState.getRedgNumber(),
                        inputState.getOwner(), inputState.getRto(), 0, null));
            }else {
                throw new FlowException("Inappropriate Settlement Value");
            }
            return null;
        }
    }
}
