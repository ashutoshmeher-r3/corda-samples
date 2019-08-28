package net.corda.samples.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "VEHICLE_DETAIL")
public class PersistentVehicle extends PersistentState {

    @Column private final String registrationNumber;
    @Column private final String chasisNumber;
    @Column private final String make;
    @Column private final String model;
    @Column private final String variant;
    @Column private final String color;
    @Column private final String fuelType;

    public PersistentVehicle() {
        this.registrationNumber = null;
        this.chasisNumber = null;
        this.make = null;
        this.model = null;
        this.variant = null;
        this.color = null;
        this.fuelType = null;
    }

    public PersistentVehicle(String registrationNumber, String chasisNumber, String make, String model, String variant, String color, String fuelType) {
        this.registrationNumber = registrationNumber;
        this.chasisNumber = chasisNumber;
        this.make = make;
        this.model = model;
        this.variant = variant;
        this.color = color;
        this.fuelType = fuelType;
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
}
