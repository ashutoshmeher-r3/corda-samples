package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contracts.VehicleContract;
import net.corda.samples.states.Vehicle;

// ******************
// * VehicleSaleFlow *
// ******************
public class VehicleSaleFlow {

    private VehicleSaleFlow() {}

    @InitiatingFlow
    @StartableByRPC
    public static class VehicleSaleInitiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker progressTracker = new ProgressTracker();

        private VehicleInfo vehicleInfo;
        private Party owner;

        public VehicleSaleInitiator(VehicleInfo vehicleInfo, Party owner) {
            this.vehicleInfo = vehicleInfo;
            this.owner = owner;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party dealer = getOurIdentity();

            Vehicle vehicle = new Vehicle(vehicleInfo.getRegistrationNumber(), vehicleInfo.getChasisNumber(),
                    vehicleInfo.getMake(), vehicleInfo.getModel(), vehicleInfo.getVariant(), vehicleInfo.getColor(),
                    vehicleInfo.getFuelType(), dealer, owner);

            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addOutputState(vehicle)
                    .addCommand(new VehicleContract.Commands.VehicleSale(), ImmutableList.of(dealer.getOwningKey()));

            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            FlowSession ownerSession = initiateFlow(owner);
            return subFlow(new FinalityFlow(selfSignedTransaction, ImmutableList.of(ownerSession)));
        }
    }

    @InitiatedBy(VehicleSaleInitiator.class)
    public static class VehicleSaleResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public VehicleSaleResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }

}