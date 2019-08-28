package net.corda.samples.flows;

import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class VehicleInsuranceInfo {

    private final String registrationNumber;
    private final String insuranceNumber;
    private final long insuredValue;
    private final int premium;

    private final String insuree;

    public VehicleInsuranceInfo(String registrationNumber, String insuranceNumber, long insuredValue, int premium, String insuree) {
        this.registrationNumber = registrationNumber;
        this.insuranceNumber = insuranceNumber;
        this.insuredValue = insuredValue;
        this.premium = premium;
        this.insuree = insuree;
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

    public String getInsuree() {
        return insuree;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }
}
