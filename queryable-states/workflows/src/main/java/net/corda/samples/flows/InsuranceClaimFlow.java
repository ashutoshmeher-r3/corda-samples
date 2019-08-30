package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.InsuranceContract;
import net.corda.samples.states.Claim;
import net.corda.samples.states.Insurance;

import java.util.ArrayList;
import java.util.List;

public class InsuranceClaimFlow {

    private InsuranceClaimFlow(){}

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceClaimInitiator extends FlowLogic<SignedTransaction>{

        private final ClaimInfo claimInfo;
        private final String policyNumber;

        public InsuranceClaimInitiator(ClaimInfo claimInfo, String policyNumber) {
            this.claimInfo = claimInfo;
            this.policyNumber = policyNumber;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            List<StateAndRef<Insurance>> insuranceStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(Insurance.class).getStates();

            StateAndRef<Insurance> inputStateAndRef = insuranceStateAndRefs.stream().filter(vehicleStateAndRef -> {
                Insurance insuranceState = vehicleStateAndRef.getState().getData();
                return insuranceState.getPolicyNumber().equals(policyNumber);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Policy Not Found"));

            Claim claim = new Claim(claimInfo.getClaimNumber(), claimInfo.getClaimDescription(),
                    claimInfo.getClaimAmount());
            Insurance input = inputStateAndRef.getState().getData();

            List<Claim> claims = new ArrayList<>();
            if(input.getClaims() == null || input.getClaims().size() == 0 ){
                claims.add(claim);
            }else {
                claims.addAll(input.getClaims());
                claims.add(claim);
            }

            Insurance output = new Insurance(input.getPolicyNumber(), input.getInsuredValue(),
                    input.getDuration(), input.getPremium(), input.getInsurer(), input.getInsuree(),
                    input.getVehicleDetail(), claims);

            TransactionBuilder transactionBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(output)
                    .addCommand(new InsuranceContract.Commands.AddClaim(), ImmutableList.of(getOurIdentity().getOwningKey()));

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            FlowSession counterpartySession = initiateFlow(input.getInsuree());
            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(counterpartySession)));
        }
    }

    @InitiatedBy(InsuranceClaimInitiator.class)
    public static class InsuranceClaimResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public InsuranceClaimResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
