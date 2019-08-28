package net.corda.samples.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;

public class VehicleSchemaV1 extends MappedSchema {

    public VehicleSchemaV1() {
        super(VehicleSchemaFamily.class, 1, ImmutableList.of(PersistentVehicle.class, PersistentInsurance.class));
    }
}