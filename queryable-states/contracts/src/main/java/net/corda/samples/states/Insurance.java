package net.corda.samples.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.contracts.VehicleContract;
import net.corda.samples.schema.PersistentInsurance;
import net.corda.samples.schema.PersistentVehicle;
import net.corda.samples.schema.VehicleSchemaV1;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BelongsToContract(VehicleContract.class)
public class Insurance implements QueryableState {

    private final String insuranceNumber;
    private final long insuredValue;
    private final int premium;

    private final Party insurer;
    private final Party insuree;

    private final PersistentVehicle persistentVehicle;

    public Insurance(String insuranceNumber, long insuredValue, int premium, Party insurer, Party insuree, PersistentVehicle persistentVehicle) {
        this.insuranceNumber = insuranceNumber;
        this.insuredValue = insuredValue;
        this.premium = premium;
        this.insurer = insurer;
        this.insuree = insuree;
        this.persistentVehicle = persistentVehicle;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof VehicleSchemaV1){
            return new PersistentInsurance(
                    this.insuranceNumber,
                    this.insuredValue,
                    this.premium,
                    this.persistentVehicle
            );
        }else{
            throw new IllegalArgumentException("Unsupported Schema");
        }
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new VehicleSchemaV1());
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(insuree, insurer);
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public long getInsuredValue() {
        return insuredValue;
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

    public PersistentVehicle getPersistentVehicle() {
        return persistentVehicle;
    }
}
