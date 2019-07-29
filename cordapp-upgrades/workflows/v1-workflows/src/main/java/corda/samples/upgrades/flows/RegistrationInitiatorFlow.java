package corda.samples.upgrades.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.states.VehicleState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.HashAttachmentConstraint;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

// ******************
// * RegistrationInitiatorFlow flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class RegistrationInitiatorFlow extends FlowLogic<SignedTransaction> {

    private final Party owner;
    private final String redgNumber;

    public RegistrationInitiatorFlow(Party owner, String redgNumber) {
        this.owner = owner;
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
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        Party rto  = getOurIdentity();

        //Build the output state
        final VehicleState vehicleState = new VehicleState(redgNumber, owner, rto);
        //Create the command instance with required signers
        final Command<VehicleContract.Commands.Register> registerCommand = new Command<>(
                new VehicleContract.Commands.Register(),
                ImmutableList.of(getOurIdentity().getOwningKey())
        );
        //Build the Transaction Builder
        final TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addOutputState(vehicleState)
                /* Comment the above line '.addOutputState(vehicleState)' and uncomment below to use HashAttachemtConstrant for Explicit Upgrade */
//                .addOutputState(vehicleState, VehicleContract.ID,
//                        new HashAttachmentConstraint(getServiceHub().getCordappProvider().getContractAttachmentID(VehicleContract.ID)))
                .addCommand(registerCommand);

        //Verify Transaction. This calls the contract's verify method.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        transactionBuilder.verify(getServiceHub());

        //Initiate Flow with owner
        FlowSession ownerSession = initiateFlow(owner);

        //Self Sign the Transaction
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        final SignedTransaction selfSignedTx = getServiceHub().signInitialTransaction(transactionBuilder);

        // Call finality flow to distribute the state to the parties
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(selfSignedTx, ImmutableList.of(ownerSession), FINALISING_TRANSACTION.childProgressTracker()));
    }
}
