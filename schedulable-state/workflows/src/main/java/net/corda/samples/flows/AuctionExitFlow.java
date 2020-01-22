package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.AuctionContract;
import net.corda.samples.states.AuctionState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionExitFlow {

    private AuctionExitFlow(){}

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private UUID auctionId;

        public Initiator(UUID auctionId) {
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();

            StateAndRef<AuctionState> auctionStateAndRef = auntionStateAndRefs.stream().filter(stateAndRef -> {
                AuctionState auctionState = stateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new FlowException("Auction Not Found"));
            AuctionState auctionState = auctionStateAndRef.getState().getData();

            List<PublicKey> signers = new ArrayList<>();
            signers.add(auctionState.getAuctioneer().getOwningKey());
            if(auctionState.getWinner()!=null){
                signers.add(auctionState.getWinner().getOwningKey());
            }
            TransactionBuilder transactionBuilder = new TransactionBuilder(auctionStateAndRef.getState().getNotary())
                    .addInputState(auctionStateAndRef)
                    .addCommand(new AuctionContract.Commands.Exit(), signers);

            transactionBuilder.verify(getServiceHub());

            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            List<FlowSession> allSessions = new ArrayList<>();

            if(auctionState.getWinner()!=null) {
                if(auctionState.getAuctioneer() == getOurIdentity()){
                    FlowSession winnerSession = initiateFlow(auctionState.getWinner());
                    winnerSession.send(true);
                    allSessions.add(winnerSession);
                    signedTransaction = subFlow(new CollectSignaturesFlow(
                            signedTransaction, ImmutableList.of(winnerSession)));
                }else {
                    FlowSession auctioneerSession = initiateFlow(auctionState.getAuctioneer());
                    auctioneerSession.send(true);
                    allSessions.add(auctioneerSession);
                    signedTransaction = subFlow(new CollectSignaturesFlow(
                            signedTransaction, ImmutableList.of(auctioneerSession)));
                }
            }

            for(Party party: auctionState.getBidders()){
                if(!party.equals(getOurIdentity())) {
                    FlowSession session = initiateFlow(party);
                    session.send(false);
                    allSessions.add(session);
                }
            }

            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.copyOf(allSessions)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            boolean flag = otherPartySession.receive(Boolean.class).unwrap(it -> it);

            if(flag) {
                subFlow(new SignTransactionFlow(otherPartySession) {

                    @Override
                    protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                    }
                });
            }
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
