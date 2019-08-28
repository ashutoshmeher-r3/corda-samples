package net.corda.samples.states;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.contracts.VehicleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.samples.schema.PersistentVehicle;
import net.corda.samples.schema.VehicleSchemaV1;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(VehicleContract.class)
public class Vehicle implements QueryableState {

    private final String registrationNumber;
    private final String chasisNumber;
    private final String make;
    private final String model;
    private final String variant;
    private final String color;
    private final String fuelType;

    private final Party dealer;
    private final Party owner;

    public Vehicle(String registrationNumber, String chasisNumber, String make, String model, String variant,
                   String color, String fuelType, Party dealer, Party owner) {
        this.registrationNumber = registrationNumber;
        this.chasisNumber = chasisNumber;
        this.make = make;
        this.model = model;
        this.variant = variant;
        this.color = color;
        this.fuelType = fuelType;
        this.dealer = dealer;
        this.owner = owner;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getChasisNumber() {
        return chasisNumber;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getVariant() {
        return variant;
    }

    public String getColor() {
        return color;
    }

    public String getFuelType() {
        return fuelType;
    }

    public Party getDealer() {
        return dealer;
    }

    public Party getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof VehicleSchemaV1){
            return new PersistentVehicle(
                    this.registrationNumber,
                    this.chasisNumber,
                    this.make,
                    this.model,
                    this.variant,
                    this.color,
                    this.fuelType
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
        return ImmutableList.of(dealer, owner);
    }
}