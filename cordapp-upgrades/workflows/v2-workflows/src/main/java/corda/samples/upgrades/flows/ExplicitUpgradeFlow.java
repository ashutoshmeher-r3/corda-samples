package corda.samples.upgrades.flows;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;

@InitiatingFlow
@StartableByRPC
public class ExplicitUpgradeFlow extends FlowLogic<Void> {

    private final StateAndRef oldStateAndRef;
    private final Class newContractClass;

    public ExplicitUpgradeFlow(StateAndRef oldStateAndRef, Class newContractClass) {
        this.oldStateAndRef = oldStateAndRef;
        this.newContractClass = newContractClass;
    }

    @Override
    public Void call() throws FlowException {

        subFlow(new ContractUpgradeFlow.Initiate(oldStateAndRef, newContractClass));
        return null;
    }
}
