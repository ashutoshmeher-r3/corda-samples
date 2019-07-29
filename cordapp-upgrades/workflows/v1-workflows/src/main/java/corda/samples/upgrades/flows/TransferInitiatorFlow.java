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

@InitiatingFlow
@StartableByRPC
public class TransferInitiatorFlow extends FlowLogic<SignedTransaction>{

    // New Owner to whom the needs to be transferred
    private final Party newOwner;
    private final String redgNumber;

    public TransferInitiatorFlow(Party newOwner , String redgNumber) {
        this.newOwner = newOwner;
        this.redgNumber = redgNumber;
    }

    // Progress Tracker Steps
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
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // RegistrationInitiatorFlow flow logic goes here.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);

        //Get own identity
        Party rto  = getOurIdentity();

        //Use vaultQuery to fetch all VehicleState and then use a filter to find the VehicleState corresponding to the registration number.
        //This resulting state will be used a input for our vehicle transfer transaction.
        List<StateAndRef<VehicleState>> stateStateAndRef = getServiceHub().getVaultService().queryBy(VehicleState.class).getStates();
        StateAndRef<VehicleState> inputStateAndRef = stateStateAndRef.stream().filter(vehicleStateAndRef -> {
            VehicleState vehicleState = vehicleStateAndRef.getState().getData();
            return vehicleState.getRedgNumber().equals(redgNumber);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Vehicle Not Found"));
        VehicleState inputState = inputStateAndRef.getState().getData();

        //Fetch the notary tracking the state.
        Party notary = inputStateAndRef.getState().getNotary();

        // Initiate Flow sessions with owner and new owner.
        FlowSession ownerSession = initiateFlow(inputState.getOwner());
        FlowSession newOwnerSession = initiateFlow(newOwner);

        // Build the output state with the updated owner.
        final VehicleState outputState = new VehicleState(inputState.getRedgNumber(), newOwner, rto);

        //Create the command instance with required signers
        final Command<VehicleContract.Commands.Transfer> transferCommand = new Command<>(
                new VehicleContract.Commands.Transfer(),
                ImmutableList.of(rto.getOwningKey())
        );

        // Build the transaction builder
        final TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(outputState)
                .addCommand(transferCommand);

        //Verify Transaction. This calls the contract's verify method.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        transactionBuilder.verify(getServiceHub());

        // Self sign the transaction
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        final SignedTransaction selfSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        // Call finality to flow to notarise and distribute the updated state to the counter-parties.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(selfSignedTx, ImmutableList.of(ownerSession, newOwnerSession),FINALISING_TRANSACTION.childProgressTracker()));

    }
}
