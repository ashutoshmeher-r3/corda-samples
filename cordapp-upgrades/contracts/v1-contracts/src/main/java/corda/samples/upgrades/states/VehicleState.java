package corda.samples.upgrades.states;

import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

// *********
// * VehicleState *
// *********
@BelongsToContract(VehicleContract.class)
public class VehicleState implements ContractState {

    // The registration number issued to the vehicle being registered.
    private final String redgNumber;
    private final Party owner;
    //Registering Authority
    private final Party rto;

    public VehicleState(String redgNumber, Party owner, Party rto) {
        this.redgNumber = redgNumber;
        this.owner = owner;
        this.rto = rto;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(owner, rto);
    }

    public String getRedgNumber() {
        return redgNumber;
    }

    public Party getOwner() {
        return owner;
    }

    public Party getRto() {
        return rto;
    }
}