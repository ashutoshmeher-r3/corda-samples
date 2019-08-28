package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contracts.VehicleContract;
import net.corda.samples.schema.PersistentVehicle;
import net.corda.samples.states.Insurance;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class VehicleInsuranceFlow {

    private VehicleInsuranceFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class VehicleInsuranceInitiator extends FlowLogic<SignedTransaction> {

        private final ProgressTracker progressTracker = new ProgressTracker();

        private final VehicleInsuranceInfo vehicleInsuranceInfo;
        private final Party insuree;
        private final String vehicleRegistrationNumber;

        public VehicleInsuranceInitiator(VehicleInsuranceInfo vehicleInsuranceInfo, Party insuree,
                                         String vehicleRegistrationNumber) {
            this.vehicleInsuranceInfo = vehicleInsuranceInfo;
            this.insuree = insuree;
            this.vehicleRegistrationNumber = vehicleRegistrationNumber;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            Party insurer = getOurIdentity();


            List<PersistentVehicle> persistentVehicleList =
                    getServiceHub().withEntityManager(entityManager -> {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();

                AbstractQuery<PersistentVehicle> abstractQuery = cb.createQuery(PersistentVehicle.class);
                Root<PersistentVehicle> vehicle = abstractQuery.from(PersistentVehicle.class);
                abstractQuery.where(cb.equal(vehicle.get("registrationNumber"), vehicleRegistrationNumber));
                CriteriaQuery<PersistentVehicle> select = ((CriteriaQuery<PersistentVehicle>) abstractQuery).select(vehicle);
                return entityManager.createQuery(select).getResultList();
            });

            if(persistentVehicleList ==  null || persistentVehicleList.size() == 0){
                throw new FlowException("Vehicle Not Found");
            }

            Insurance insurance = new Insurance(vehicleInsuranceInfo.getInsuranceNumber(),
                vehicleInsuranceInfo.getInsuredValue(), vehicleInsuranceInfo.getPremium(), insurer, insuree,
                persistentVehicleList.get(0));

            TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(insurance)
                .addCommand(new VehicleContract.Commands.VehicleSale(), ImmutableList.of(insurer.getOwningKey()));

            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            FlowSession ownerSession = initiateFlow(insuree);
            return subFlow(new FinalityFlow(selfSignedTransaction, ImmutableList.of(ownerSession)));
        }
    }

    @InitiatedBy(VehicleInsuranceInitiator.class)
    public static class VehicleInsuranceResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public VehicleInsuranceResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
