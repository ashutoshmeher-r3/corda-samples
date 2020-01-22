package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;

import java.util.Currency;
import java.util.UUID;

@StartableByRPC
public class AuctionSettlementFlow extends FlowLogic<Void> {

    private final UUID auctionId;
    private final Amount<Currency> amount;

    public AuctionSettlementFlow(UUID auctionId, Amount<Currency> amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new AuctionDvPFlow.Initiator(auctionId, amount));
        subFlow(new AuctionExitFlow.Initiator(auctionId));
        return null;
    }
}