package net.corda.samples.webserver;

import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.flows.*;
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

    @PostMapping(value = "/vehicleInsurance/{insuree}")
    private String vehicleSale(@RequestBody InsuranceInfo vehicleInsuranceInfo, @PathVariable String insuree) {

        Set<Party> matchingParties = proxy.partiesFromName(insuree, false);


        proxy.startFlowDynamic(IssueInsuranceFlow.IssueInsuranceInitiator.class, vehicleInsuranceInfo,
                matchingParties.iterator().next());
        return "Issue Insurance Completed";
    }

    @PostMapping(value = "/vehicleInsurance/claim/{policyNumber}")
    private String claim(@RequestBody ClaimInfo claimInfo, @PathVariable String policyNumber) {

        proxy.startFlowDynamic(InsuranceClaimFlow.InsuranceClaimInitiator.class, claimInfo, policyNumber);
        return "Insurance Claim Completed";
    }
}