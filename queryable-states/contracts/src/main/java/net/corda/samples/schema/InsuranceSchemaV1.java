package net.corda.samples.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;

public class InsuranceSchemaV1 extends MappedSchema {

    public InsuranceSchemaV1() {
        super(InsuranceSchemaFamily.class, 1, ImmutableList.of(PersistentInsurance.class,
                PersistentVehicle.class, PersistentClaim.class));
    }
}