package net.corda.samples.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "INSURANCE_DETAIL")
public class PersistentInsurance extends PersistentState {

    @Column private final String policyNumber;
    @Column private final Long insuredValue;
    @Column private final Integer premium;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "registrationNumber", referencedColumnName = "registrationNumber")
    private final PersistentVehicle vehicle;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "policyNumber", referencedColumnName = "policyNumber")
    private List<PersistentClaim> claims;

    public PersistentInsurance() {
        this.policyNumber = null;
        this.insuredValue = null;
        this.premium = null;
        this.vehicle = null;
        this.claims = null;
    }

    public PersistentInsurance(String policyNumber, Long insuredValue, Integer premium, PersistentVehicle vehicle,
                               List<PersistentClaim> claims) {
        this.policyNumber = policyNumber;
        this.insuredValue = insuredValue;
        this.premium = premium;
        this.vehicle = vehicle;
        this.claims = claims;
    }

    public String getPolicyNumber() {
        return policyNumber;
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

    public List<PersistentClaim> getClaims() {
        return claims;
    }
}
