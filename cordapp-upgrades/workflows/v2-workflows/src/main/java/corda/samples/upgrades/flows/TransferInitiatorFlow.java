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

// Update version of the flow to 2
@InitiatingFlow(version = 2)
@StartableByRPC
public class TransferInitiatorFlow extends FlowLogic<SignedTransaction>{

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
    private final ProgressTracker.Step GATHERING_SIGNATURE = new ProgressTracker.Step("Collecting Signature from Counterparty");
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
            // New Step Added in version 2
            GATHERING_SIGNATURE,
            FINALISING_TRANSACTION

    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        // Get Own Identity
        Party rto  = getOurIdentity();

        //Fetch stateAndRef from Vault using vaultQuery.
        List<StateAndRef<VehicleState>> stateStateAndRef = getServiceHub().getVaultService().queryBy(VehicleState.class).getStates();
        StateAndRef<VehicleState> inputStateAndRef = stateStateAndRef.stream().filter(vehicleStateAndRef -> {
            VehicleState vehicleState = vehicleStateAndRef.getState().getData();
            return vehicleState.getRedgNumber().equals(redgNumber);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Vehicle Not Found"));
        VehicleState inputState = inputStateAndRef.getState().getData();

        //Fetch the notary tracking the state.
        Party notary = inputStateAndRef.getState().getNotary();

        // Initiate Flow sessions with owner and new owner
        FlowSession ownerSession = initiateFlow(inputState.getOwner());
        FlowSession newOwnerSession = initiateFlow(newOwner);

        // Build the output state with the updated owner.
        final VehicleState outputState = new VehicleState(inputState.getRedgNumber(), newOwner, rto);

        // Create the command instance with required signers
        // Check the version of the flow installed on the counter-parties and decide on the required signers. This is to ensure backward compatibility.
        final Command<VehicleContract.Commands.Transfer> transferCommand;
        if(ownerSession.getCounterpartyFlowInfo().getFlowVersion() == 2 && newOwnerSession.getCounterpartyFlowInfo().getFlowVersion() == 2) {
            transferCommand = new Command<>(
                    new VehicleContract.Commands.Transfer(),
                    ImmutableList.of(rto.getOwningKey(), inputState.getOwner().getOwningKey(), newOwner.getOwningKey())
            );
        }else{
            transferCommand = new Command<>(
                    new VehicleContract.Commands.Transfer(),
                    ImmutableList.of(rto.getOwningKey())
            );
        }

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


        // Check the version of the flow installed on the counter-parties and decide whether to call CollectSignaturesFlow.
        // This is again to ensure backward compatibility.
        if(ownerSession.getCounterpartyFlowInfo().getFlowVersion() == 2 && newOwnerSession.getCounterpartyFlowInfo().getFlowVersion() == 2) {

            //Flag for responder flow to identify signing is not required in case only one counterparty is upgraded to version two.
            ownerSession.send(true);
            newOwnerSession.send(true);
            // Collect Signatures from CounterParties.
            progressTracker.setCurrentStep(GATHERING_SIGNATURE);
            SignedTransaction counterSignedTrnx = subFlow(new CollectSignaturesFlow(selfSignedTx, ImmutableList.of(ownerSession, newOwnerSession)));

            // Call finality to flow to notarise and distribute the updated state to all the parties.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(counterSignedTrnx, ImmutableList.of(ownerSession, newOwnerSession),FINALISING_TRANSACTION.childProgressTracker()));

        }else{

            // Only one party is upgraded to version 2, hence signature not required from counterparty. Using flag to identify this case in responder flow.
            if(ownerSession.getCounterpartyFlowInfo().getFlowVersion() == 2 ^ newOwnerSession.getCounterpartyFlowInfo().getFlowVersion() == 2) {
                if(ownerSession.getCounterpartyFlowInfo().getFlowVersion() == 2){
                    ownerSession.send(false);
                }
                if(newOwnerSession.getCounterpartyFlowInfo().getFlowVersion() == 2){
                    newOwnerSession.send(false);
                }
            }

            // Call finality to flow to notarise and distribute the updated state to all the parties.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(selfSignedTx, ImmutableList.of(ownerSession, newOwnerSession),FINALISING_TRANSACTION.childProgressTracker()));
        }
    }
}