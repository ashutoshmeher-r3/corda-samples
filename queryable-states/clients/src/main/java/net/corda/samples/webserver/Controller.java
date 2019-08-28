package net.corda.samples.webserver;

import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.flows.VehicleInsuranceFlow;
import net.corda.samples.flows.VehicleInsuranceInfo;
import net.corda.samples.flows.VehicleSaleFlow;
import net.corda.samples.flows.VehicleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }

    @PostMapping(value = "/vehicleSale")
    private String vehicleSale(@RequestBody VehicleInfo vehicleData) {

        Set<Party> matchingParties = proxy.partiesFromName(vehicleData.getOwner().toString(), false);

        proxy.startFlowDynamic(VehicleSaleFlow.VehicleSaleInitiator.class, vehicleData,
                matchingParties.iterator().next());
        return "Vehicle Sale Completed";
    }

    @PostMapping(value = "/vehicleInsurance/{insuree}")
    private String vehicleSale(@RequestBody VehicleInsuranceInfo vehicleInsuranceInfo, @PathVariable String insuree) {

        Set<Party> matchingParties = proxy.partiesFromName(insuree, false);


        proxy.startFlowDynamic(VehicleInsuranceFlow.VehicleInsuranceInitiator.class, vehicleInsuranceInfo,
                matchingParties.iterator().next(), vehicleInsuranceInfo.getRegistrationNumber());
        return "Vehicle Sale Completed";
    }
}