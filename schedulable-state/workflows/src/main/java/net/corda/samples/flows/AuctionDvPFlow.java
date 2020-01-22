package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.finance.contracts.asset.OnLedgerAsset;
import net.corda.finance.contracts.asset.PartyAndAmount;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.samples.states.Asset;
import net.corda.samples.states.AuctionState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.*;

public class AuctionDvPFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private final UUID auctionId;
        private final Amount<Currency> payment;

        public Initiator(UUID auctionId, Amount<Currency> payment) {
            this.auctionId = auctionId;
            this.payment = payment;
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

            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null, ImmutableList.of(auctionStateAndRef.getState().getData().getAuctionItem()
                    .resolve(getServiceHub()).getState().getData().getLinearId().getId()),
                    null, Vault.StateStatus.UNCONSUMED);
            StateAndRef<Asset> assetStateAndRef = getServiceHub().getVaultService().
                    queryBy(Asset.class, queryCriteria).getStates().get(0);

            CommandAndState commandAndState = assetStateAndRef.getState().getData()
                    .withNewOwner(auctionState.getWinner());

            TransactionBuilder transactionBuilder = new TransactionBuilder(getServiceHub().getNetworkMapCache()
                    .getNotaryIdentities().get(0));

            Pair<TransactionBuilder, List<PublicKey>> txAndKeysPair =
                    CashUtils.generateSpend(getServiceHub(), transactionBuilder, payment, getOurIdentityAndCert(),
                            auctionState.getAuctioneer(), Collections.emptySet());
            transactionBuilder = txAndKeysPair.getFirst();

            transactionBuilder.addInputState(assetStateAndRef)
                    .addOutputState(commandAndState.getOwnableState())
                    .addCommand(commandAndState.getCommand(),
                            ImmutableList.of(auctionState.getAuctioneer().getOwningKey()));

            transactionBuilder.verify(getServiceHub());

            List<PublicKey> keysToSign = txAndKeysPair.getSecond();
            keysToSign.add(getOurIdentity().getOwningKey());
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(transactionBuilder, keysToSign);

            FlowSession auctioneerFlow = initiateFlow(auctionState.getAuctioneer());
            SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(selfSignedTransaction,
                    ImmutableList.of(auctioneerFlow)));

            return subFlow(new FinalityFlow(signedTransaction, ImmutableList.of(auctioneerFlow)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{

        private FlowSession otherPartySession;

        public Responder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            subFlow(new SignTransactionFlow(otherPartySession) {

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    // Additional Checks here
                }
            });
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
