package net.corda.samples.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.SchedulableState;
import net.corda.core.contracts.ScheduledActivity;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.FlowLogicRef;
import net.corda.core.flows.FlowLogicRefFactory;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.contracts.AuctionContract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AuctionState State
 */
@BelongsToContract(AuctionContract.class)
public class AuctionState implements SchedulableState {

    private final UUID auctionId;
    private final Long basePrice;
    private final Long currentHighestBid;
    private final Party currentHighestBidder;
    private final Instant bidEndTime;
    private final Long winningBid;
    private final Boolean active;

    private final Party auctioner;
    private final List<Party> bidders;
    private final Party winner;

    public AuctionState(UUID auctionId, Long basePrice, Long currentHighestBid, Party currentHighestBidder,
                        Instant bidEndTime, Long winningBid, Boolean active, Party auctioner, List<Party> bidders,
                        Party winner) {
        this.auctionId = auctionId;
        this.basePrice = basePrice;
        this.currentHighestBid = currentHighestBid;
        this.currentHighestBidder = currentHighestBidder;
        this.bidEndTime = bidEndTime;
        this.winningBid = winningBid;
        this.active = active;
        this.auctioner = auctioner;
        this.bidders = bidders;
        this.winner = winner;
    }

    @Nullable
    @Override
    public ScheduledActivity nextScheduledActivity(@NotNull StateRef thisStateRef,
                                                   @NotNull FlowLogicRefFactory flowLogicRefFactory) {
        FlowLogicRef flowLogicRef = flowLogicRefFactory.create(
                "net.corda.samples.flows.EndAuctionInitiator", auctionId);
        return new ScheduledActivity(flowLogicRef, bidEndTime);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<Party> allParties = new ArrayList<>(bidders);
        allParties.add(auctioner);
        return ImmutableList.copyOf(allParties);
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public Long getBasePrice() {
        return basePrice;
    }

    public Long getCurrentHighestBid() {
        return currentHighestBid;
    }

    public Party getCurrentHighestBidder() {
        return currentHighestBidder;
    }

    public Instant getBidEndTime() {
        return bidEndTime;
    }

    public Long getWinningBid() {
        return winningBid;
    }

    public Party getAuctioner() {
        return auctioner;
    }

    public List<Party> getBidders() {
        return bidders;
    }

    public Party getWinner() {
        return winner;
    }

    public Boolean getActive() {
        return active;
    }
}
