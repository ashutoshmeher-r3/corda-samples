package corda.samples.upgrades;

import com.google.common.collect.ImmutableList;
import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.contracts.v2.VehicleContractV2;
import corda.samples.upgrades.flows.ExplicitUpgradeFlow;
import corda.samples.upgrades.states.VehicleState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.flows.ContractUpgradeFlow;
import net.corda.core.messaging.CordaRPCOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.corda.core.utilities.NetworkHostAndPort.parse;

public class ExplicitContractUpgradeClient {

    private static final Logger logger = LoggerFactory.getLogger(ExplicitContractUpgradeClient.class);

    public static void main(String[] args) throws InterruptedException{
        if (args.length != 3) throw new IllegalArgumentException("Usage: Client <node address> <node address> <node address> ");

        final CordaRPCClient clientRTO = new CordaRPCClient(parse(args[0]));
        final CordaRPCOps proxyRTO = clientRTO.start("user1", "test").getProxy();
        final CordaRPCClient clientA = new CordaRPCClient(parse(args[1]));
        final CordaRPCOps proxyA = clientA.start("user1", "test").getProxy();
        final CordaRPCClient clientB = new CordaRPCClient(parse(args[2]));
        final CordaRPCOps proxyB = clientB.start("user1", "test").getProxy();

        ImmutableList.of(proxyRTO, proxyA, proxyB).forEach( proxy -> {
            proxy.vaultQuery(VehicleState.class).getStates()
                    .stream().filter(vehicleStateStateAndRef ->
                vehicleStateStateAndRef.getState().getContract().equals(VehicleContract.ID)
            ).forEach(vehicleStateStateAndRef -> {
                System.out.println("--------Calling Authorize--------");
                System.out.println("Authorize:" + vehicleStateStateAndRef);
                proxy.startFlowDynamic(ContractUpgradeFlow.Authorise.class, vehicleStateStateAndRef, VehicleContractV2.class);
            });
        });

        Thread.sleep(10000);

        ImmutableList.of(proxyA, proxyB).forEach( proxy -> {
            proxy.vaultQuery(VehicleState.class).getStates()
                    .stream().filter(vehicleStateStateAndRef -> {
                return vehicleStateStateAndRef.getState().getContract().equals(corda.samples.upgrades.contracts.VehicleContract.ID);
            }).forEach(vehicleStateStateAndRef -> {
                System.out.println("--------Calling Upgrade--------");
                System.out.println("Upgrade:" + vehicleStateStateAndRef);
                proxy.startFlowDynamic(ExplicitUpgradeFlow.class, vehicleStateStateAndRef, VehicleContractV2.class);
            });
        });

    }
}
