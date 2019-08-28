package net.corda.samples.schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "CLAIM_DETAIL")
public class PersistentClaim {

    @Column @Id private final String claimNumber;
    @Column private final String claimDescription;
    @Column private final Integer claimAmount;

    public PersistentClaim() {
        this.claimNumber = null;
        this.claimDescription = null;
        this.claimAmount = null;
    }

    public PersistentClaim(String claimNumber, String claimDescription, Integer claimAmount) {
        this.claimNumber = claimNumber;
        this.claimDescription = claimDescription;
        this.claimAmount = claimAmount;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public String getClaimDescription() {
        return claimDescription;
    }

    public Integer getClaimAmount() {
        return claimAmount;
    }
}
