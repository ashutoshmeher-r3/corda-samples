package net.corda.samples.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.*;

@Entity
@Table(name = "INSURANCE_DETAIL")
public class PersistentInsurance extends PersistentState {

    @Column private final String insuranceNumber;
    @Column private final Long insuredValue;
    @Column private final Integer premium;

    @OneToOne
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private final PersistentVehicle vehicle;

    public PersistentInsurance() {
        this.insuranceNumber = null;
        this.insuredValue = null;
        this.premium = null;
        this.vehicle = null;
    }

    public PersistentInsurance(String insuranceNumber, Long insuredValue, Integer premium, PersistentVehicle vehicle) {
        this.insuranceNumber = insuranceNumber;
        this.insuredValue = insuredValue;
        this.premium = premium;
        this.vehicle = vehicle;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public Long getInsuredValue() {
        return insuredValue;
    }

    public Integer getPremium() {
        return premium;
    }

    public PersistentVehicle getVehicle() {
        return vehicle;
    }
}
