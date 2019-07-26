package corda.samples.upgrades.states.v3;

import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.v3.VehicleContractV3;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import javax.annotation.Nullable;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(VehicleContractV3.class)
public class VehicleStateV3 implements ContractState {

    private final String redgNumber;
    private final Party owner;
    private final Party rto;
    private final Integer challanValue;
    private final Party challanIssuer;

    public VehicleStateV3(String redgNumber, Party owner, Party rto, @Nullable Integer challanValue, @Nullable Party challanIssuer) {
        this.redgNumber = redgNumber;
        this.owner = owner;
        this.rto = rto;
        this.challanValue = challanValue;
        this.challanIssuer = challanIssuer;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        if(challanIssuer != null)
            return ImmutableList.of(owner, rto, challanIssuer);
        else
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

    public Integer getChallanValue() {
        return challanValue;
    }

    public Party getChallanIssuer() {
        return challanIssuer;
    }
}