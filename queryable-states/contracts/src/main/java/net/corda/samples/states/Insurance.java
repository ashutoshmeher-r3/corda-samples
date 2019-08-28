package net.corda.samples.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.contracts.InsuranceContract;
import net.corda.samples.schema.InsuranceSchemaV1;
import net.corda.samples.schema.PersistentInsurance;
import net.corda.samples.schema.PersistentVehicle;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BelongsToContract(InsuranceContract.class)
public class Insurance implements QueryableState {

    private final VehicleDetail vehicleDetail;

    private final String policyNumber;
    private final long insuredValue;
    private final int duration;
    private final int premium;

    private final Party insurer;
    private final Party insuree;

    private final List<Claim> claims;

    public Insurance(String policyNumber, long insuredValue, int duration, int premium, Party insurer,
                     Party insuree, VehicleDetail  vehicleDetail, List<Claim> claims) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.insurer = insurer;
        this.insuree = insuree;
        this.vehicleDetail = vehicleDetail;
        this.claims = claims;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof InsuranceSchemaV1){
            return new PersistentInsurance(
                    this.policyNumber,
                    this.insuredValue,
                    this.premium,
                    this.vehicleDetail==null ? null : new PersistentVehicle(
                            vehicleDetail.getRegistrationNumber(),
                            vehicleDetail.getChasisNumber(),
                            vehicleDetail.getMake(),
                            vehicleDetail.getModel(),
                            vehicleDetail.getVariant(),
                            vehicleDetail.getColor(),
                            vehicleDetail.getFuelType()
                    ),
                    this.claims == null? null:
            );
        }else{
            throw new IllegalArgumentException("Unsupported Schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new InsuranceSchemaV1());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(insuree, insurer);
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public long getInsuredValue() {
        return insuredValue;
    }

    public int getDuration() {
        return duration;
    }

    public int getPremium() {
        return premium;
    }

    public Party getInsurer() {
        return insurer;
    }

    public Party getInsuree() {
        return insuree;
    }

    public VehicleDetail getVehicleDetail() {
        return vehicleDetail;
    }

    public List<Claim> getClaims() {
        return claims;
    }
}
