package corda.samples.upgrades.states;

import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import javax.annotation.Nullable;
import java.util.List;

// *********
// * VehicleState *
// *********
@BelongsToContract(VehicleContract.class)
public class VehicleState implements ContractState {

    private final String redgNumber;
    private final Party owner;
    // Registering Authority
    private final Party rto;

    // New fields introduces in version 2
    private final Integer challanValue;
    private final Party challanIssuer;

    // New fields should be nullable, so that older states issued with previous could be transacted with the newer version.
    public VehicleState(String redgNumber, Party owner, Party rto, @Nullable Integer challanValue, @Nullable Party challanIssuer) {
        this.redgNumber = redgNumber;
        this.owner = owner;
        this.rto = rto;

        //New Fields
        this.challanValue = challanValue;
        this.challanIssuer = challanIssuer;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        // If no challans issued or challans have been settled, no need to have challanIssuer as participant.
        if(challanIssuer != null)
            return ImmutableList.of(owner, rto, challanIssuer);
        else
            return ImmutableList.of(owner, rto);
    }

    // Getters
    public String getRedgNumber() {
        return redgNumber;
    }

    public Party getOwner() {
        return owner;
    }

    public Party getRto() {
        return rto;
    }

    public Integer getChallanValue() {
        return challanValue;
    }

    public Party getChallanIssuer() {
        return challanIssuer;
    }
}