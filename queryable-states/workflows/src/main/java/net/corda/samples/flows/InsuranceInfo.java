package net.corda.samples.flows;

import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class InsuranceInfo {

    private final VehicleInfo vehicleInfo;

    private final String policyNumber;
    private final long insuredValue;
    private final int duration;
    private final int premium;

    private final String insuree;

    public InsuranceInfo(String policyNumber, long insuredValue, int duration, int premium, String insuree, VehicleInfo vehicleInfo) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.duration = duration;
        this.premium = premium;
        this.insuree = insuree;
        this.vehicleInfo = vehicleInfo;
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

    public String getInsuree() {
        return insuree;
    }

    public VehicleInfo getVehicleInfo() {
        return vehicleInfo;
    }
}
