<p align="center">
  <img src="https://camo.githubusercontent.com/a7b7d659d6e01a9e49ff2d9919f7a66d84aac66e/68747470733a2f2f7777772e636f7264612e6e65742f77702d636f6e74656e742f75706c6f6164732f323031362f31312f66673030355f636f7264615f622e706e67" alt="Corda" width="500">
</p>

# CorDapp Upgrades

This sample shows how to upgrade corDapps using implicit and explicit approaches. 
Signature Constraint (Implicit-Upgrades) introduced in Corda 4 is however the recommended approach to perform upgrades in Corda, since it doesn't 
requires the heavyweight process of creating upgrade transactions for every state on the ledger of all parties.


## Contract and Flow Version
This sample has various versions of contracts and flows which will used to demonstrate implicit & explicit upgrades in Corda.

**Version 1 Contracts & Flows**

Version 1 contracts and flows will be our initial cordapp implementation. Its a simple cordapp on vehicle registration, which contains two flows, 
one for issuing registration number for the vehicle and other for transferring the vehicle to a new owner. There would be 3 parties involved - `RTO` 
, `Party A` and `PartyB`. We assume 
that both the flows would be initiated by the `RTO`. `Police` party would get involved in the later part as we upgrade our cordapps to include new features.

    Regional Transport Office (RTO), is the registering authority who registers vehicle and issue 
    registration numbers for them.

**Version 2 Flows**

Version 2 of flows brings a minor change, while version 1 has the owner and new owner of the vehicle as non-signing parties for vehicle transfer flow, 
this version requires them as signing parties.

**Version 2 Contracts and Version 3 Flows**

Version 2 of contracts introduces a new feature to issue/ pay challan again traffic violation. `Police` party would be issuing the challans 
and payments against challans could be done by the vehicle owner. Two new state variable are introduced in the `VehicleState` (`challanValue` and  `challanIssuer`) for 
this purpose. Corresponding Commands and verify logic are added to the `VehicleContract`. The corresponding flows to accommodate 
this feature is implemented in version 3 of the flows.

    Challans are offical documents, issued by police against traffic rule violation fines. It is generally 
    used as a receipt for payment. Challan value here refers to the fine amount charged against traffic 
    rule violation.

**Version 3 Contracts**

Version 3 contracts updates the transfer verify logic to restrict transfer of vehicle having pending challan dues.

**Version 2 Explicit**

This version of the contract would be used to perform explicit upgrades. It is equivant to version 2 of the contract but the contract implements the 
`UpgradedContract` interface which is required for explicit upgrades of contracts using `WhiteListedByZoneAttachmentConstraint`.

**Version 2 Legacy Explicit**

This version of the contract would be used to perform explicit upgrades for contracts using hash constraint. It is equivant to version 2 
of the contract but the contract implements the `UpgradedLegacyContract` interface which is required for explicit upgrades of contracts 
using `HashAttachmentConstraint`.


# Implicit Upgrade Scenarios And Steps

## Scenario 1: Initial Deployment, Vehicle Registration and Transfer

**Step1:** 

Deploy version1 of contracts and flows by running `./gradlew deployNodes` and then run the nodes using `./build/nodes/runnodes`. 
This would create a network of 4 nodes and a notary all running version 1 of contracts and flows. The `Police` node however would be used once 
the Challan feature is introduced in our cordapp in later versions.

**Step2:** 

Register two vehicle by running the `RegistrationInitiatorFlow` from `RTO`'s shell. We need to pass the registration number and the party to 
whom the vehicle is being registered, as parameters to the flow.

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2321
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2322
    
**Step3:** 

Run vaultQuery to validate the vehicle successfully registered. The state should have been shared with `PartyA` and `RTO`. Run the below 
command in both the party's shell.
    
    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step4:** 

Transfer one of the vehicle to `Party B`.
    
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2321
    
**Step5:** 

Run vaultQuery to validate successful transfer of the vehicle. The state information of the vehicle transferred should now only be available with 
`PartyB` and `RTO`. `PartyA` would not anymore be able to view the state. Run the below command in all the three party's shell and check the result.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
## Scenario 2: Flow Upgrade to version 2 for RTO and Party A

**Step1:** 

Shutdown the nodes and upgrade the flows to version 2 for `RTO` and `PartyA` nodes. Upgrade can be done by using the below script, which would copy
 v2-workflows.jar to cordapps directory of both the nodes.

    cd script
    ./upgrade.sh --node=RTO,PartyA --workflow=2
    
**Step2**: 

Restart the nodes. `RTO` and `PartyA` should now be running version 2 of the flows while `PartyB` would still be running version 1.

**Step3**: 

After the upgrade we should still be able perform transactions between nodes. Try to register a new vehicle and transfer a 
pre-registered vehicle from the `RTO`'s shell

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2323
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2322
    
 Note the while transfer of the vehicle, the "Collecting Signature from Counterparty" step would be greyed out which means the step was not executed since `Party B` is 
 still on the older version of the flow, which does not have the feature. However since our new version of the flow is backward compatible we are still able to transact.

## Scenario 3: Flow Upgrade to version 2 for Party B

**Step1:** 

Shutdown the nodes and upgrade the flows to version 2 for `PartyB`.

    cd script
    ./upgrade.sh --node=PartyB --workflow=2

**Step2:** 

Restart the nodes. `PartyB` should now also be running version 2 of the flow.

**Step3:** 

Since all the three nodes are now running the upgraded version of the flow, we should now be able to see the "Collecting Signature from Counterparty" step working.
Run the below commands from the `RTO`'s shell to validate the same.
    
    // Register Vechile just to make sure its working as well.
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2324 
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2323
    
## Scenario 4: Contract & Flow Upgrade to Introduce New Feature for RTO, Police and PartyA

**Step1:** 

Shutdown the nodes and upgrade the flows to version 3 and contracts to version 2 for `RTO`, `Police` and `PartyA` nodes. Upgrade can be done by using the below script, which would copy
 v3-workflows.jar and v2-contracts.jar to cordapps directory of these the nodes.

    cd script
    ./upgrade.sh --node=RTO,PartyA,Police --workflow=3 --contract=2
    
**Step2**: 

Restart the nodes. `RTO`, `Police` and `PartyA` should now be running version 2 of contracts and version 3 of flows while `PartyB` would still be running older versions.

**Step3**: 

After the upgrade we should have the challan feature introduced. Lets first try out the register and transfer flow to check and validate they still work. 
Run the below commands from the `RTO`'s shell to validate the same.

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2325 
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2324
    
You should notice that Register works, but the Transfer fails at the counterparty, that's because `PartyB` is on a different version of the contract and thus cannot validate the transaction. 
In order for the transaction to execute successfully `PartyB` must also upgrade to the latest version of the contract.  You may also notice that the registration 
of vehicle to PartyB works fine, that's because PartyB is not a signing party in the transaction hence does not need to run the contract. However he would not receive 
the state till he upgrades, since he cannot check the validity of the transaction when he receives the notarised transaction to commit in ledger. The flow would thus 
checkpoint and would retry after upgrade.

    start RegistrationInitiatorFlow owner: PartyB, redgNumber: MH01C2326
    
**Step4**: 

Validate that new feature introduced works fine, Issue Pay a challan by running be below command from `Police`'s shell
    
    start IssueChallanInitiatorFlow redgNumber: MH01C2325, rto: RTO, challanValue: 5000
    
Settle challan dues from `PartyA`'s shell using the below command.
    
    start PayChallanInitiatorFlow value: 5000, redgNumber: MH01C2325
    
Above flows would work fine, since the vehicle is registered to PartyA, however if we trigger the `IssueChallanInitiatorFlow` for a vehicle registered with `PartyB`, the flow would wait
indefinitely for the responder to respond, since there is no responder available at `PartyB` as it has not upgraded to the latest version yet. 
You can terminate the flow by pressing `Ctrl+C` in the rpc shell.

    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000

## Scenario 5: Contract & Flow Upgrade to Introduce New Feature for PartyB

**Step1:** 

Shutdown the nodes and upgrade the flows to version 3 and contracts to version 2 for `PartyB`.

    cd script
    ./upgrade.sh --node=PartyB --workflow=3 --contract=2

**Step2:** 

Restart the nodes. `PartyB` should now also be running updated version of contract and flows.

**Step3:** 

Validate Transfer flow works for `PartyB` after the upgrade.
    
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2324
    
**Step4:** 

Validate Issue challan by running be below command from `Police`'s shell & Settle Challan from `PartyB`'s shell.

    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000
    start PayChallanInitiatorFlow value: 5000, redgNumber: MH01C2321
        
## Scenario 6: Contract Upgrade for PartyA, PartyB and Police to new version of contract which Restrict Vehicle Transfer having pending dues.
**Step1:** 

Shutdown the nodes and upgrade the contracts to version 3 for `PartyA`, `PartyB` and `Police`

    cd script
    ./upgrade.sh --node=PartyA,PartyB,Police --contract=3
    
**Step2:** 

Restart the nodes. `PartyA`, `PartyB` and `Police` should now be running version 3 of contracts while `RTO` would still be running versions 2.

**Step3:** 

Try to initiate transfer flow, this should complete successfully even though `RTO` is running the older version, he is still able to transact. This is the 
benefit to implicit upgrades which allows parties with older version to still be able to transact, irrespective other parties are on upgraded versions.

    start TransferInitiatorFlow newOwner: PartyA, redgNumber: MH01C2321
    
**Step4:** 

Try to issue Challan on vehicles.    
    
    start IssueChallanInitiatorFlow redgNumber: MH01C2321, rto: RTO, challanValue: 5000
    
Notice that the above flow passes successfully, although `RTO` is on a different version of the contract, since `RTO` is a non-signing party and hence contract
is not executed at his end. However `RTO` would not be able to receive the updated state, since he would not be able to validate the validity of the 
transaction while receiving the notarized transaction to commit in ledger, without the latest version of the contract. The flows would be checkpoints at his end, 
and the updates would be received once he moves to the updated contract version. 

Had `RTO` been a signing party, the transaction would have failed to validate, since `RTO` would not be able to validate a state against the new contract as he
has not upgraded to the latest version.
     
    
## Scenario 7: Contract Upgrade to version 3 for RTO.  

**Step1:** 

Shutdown the nodes and upgrade the contracts to version 3 for `RTO`

    cd script
    ./upgrade.sh --node=RTO --contract=3
    
**Step2:** 

Restart the nodes. `RTO` should now be running version 3 of contract.

**Step3:** 

Run vaultQuery to check if `RTO` has received the updated states after the version upgrade.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step4:** 

Validate the transfer vehicle flow works after contract version upgrade on all parties

    start TransferInitiatorFlow newOwner: PartyA, redgNumber: MH01C2321

Note that this would fail, because of pending challans, Pay the challans and try again and it should pass.    


# Explicit Upgrade Steps

## WhiteListedByZoneAttachmentConstraint

**Step1:** 

Update deployNodes task in root build.gradle, uncomment the below two line in nodeDefaults section

    cordapp project(v2_contract_explicit)
    cordapp project(v2_contract_legacy_explicit)
    
**Step2:**    

Update build.gradle in v1-contracts, uncomment the below lines in contract section

    signing {
       enabled false
    }
    
**Step3:** 

Clean deploy the nodes and the run the nodes.

    ./gradlew clean deployNodes
    ./build/nodes/runnodes
    
**Step4:** 
    
Register two vehicle to `PartyA` and transfer one of them to `PartyB`. Run the below commands from `RTO`'s shell

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2321
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2322
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2321
    

**Step5:**

Run vaultQuery in each party's shell to check the states issued.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step6:**  

Perform explicit upgrade to the contracts defined in `v2-contracts-explicit` module. It can be done by running the client `ExplicitContractUpgradeClient` 
using below command

    ./gradlew runUpgradeClient
    
The client uses the `ContractUpgradeFlow` to upgrade the contracts and states to a new version. Note that there is some issue calling the 
`ContractUpgradeFlow.Initiate` from java, hence we have defined a `ExplicitUpgradeFlow` in `v1-workflows` to do the same.

**Step7:**

Run vaultQuery in each party's shell to check the upgraded states issued. Notice that the old states would have been consumed.

    run vaultQuery contractStateType: corda.samples.upgrades.states.v2.VehicleStateV2    


## HashAttachmentConstraint

**Step1:** 

Shutdown the nodes and update the below section in `RegistrationInitiatorFlow` in `v1-workflows`. This can be found in the `call()` method
Comment the line 'addOutputState(vehicleState)' and uncomment the line below it. This is done so that the state issued used HashAttachmentConstraint.

    // addOutputState(vehicleState)
    /* Comment yhe above line '.addOutputState(vehicleState)' and uncomment below to use HashAttachemtConstrant for Explicit Upgrade */
       .addOutputState(vehicleState, VehicleContract.ID,
             new HashAttachmentConstraint(getServiceHub().getCordappProvider().getContractAttachmentID(VehicleContract.ID)))

**Step2:** 

Clean deploy the nodes and the run the nodes.

    ./gradlew clean deployNodes
    ./build/nodes/runnodes
    
**Step3:** 
    
Register two vehicle to `PartyA` and transfer one of them to `PartyB`. Run the below commands from `RTO`'s shell

    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2321
    start RegistrationInitiatorFlow owner: PartyA, redgNumber: MH01C2322
    start TransferInitiatorFlow newOwner: PartyB, redgNumber: MH01C2321
    

**Step5:**

Run vaultQuery in each party's shell to check the states issued.

    run vaultQuery contractStateType: corda.samples.upgrades.states.VehicleState
    
**Step6:**  

Perform explicit upgrade to to the contracts defined in `v3-contracts-legacy-explicit` module. It can be done by running the client 
`ExplicitLegacyContractUpgradeClient` using below command

    ./gradlew runLegacyUpgradeClient

**Step7:**

Run vaultQuery in each party's shell to check the upgraded states issued. Notice that the old states would have been consumed.

    run vaultQuery contractStateType: corda.samples.upgrades.states.v3.VehicleStateV3