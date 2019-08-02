package corda.samples.upgrades.contracts.v3;

import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.states.VehicleState;
import corda.samples.upgrades.states.v3.VehicleStateV3;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

// ************
// * VehicleContractV3 *
// In order to upgrade from HashConstraint, the upgraded contract must implement the UpgradedContractWithLegacyConstraint interface.
// ************
public class VehicleContractV3 implements UpgradedContractWithLegacyConstraint<VehicleState, VehicleStateV3> {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "corda.samples.upgrades.contracts.v3.VehicleContractV3";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.Register){
            VehicleStateV3 output = (VehicleStateV3) tx.getOutput(0);
            if(output.getRedgNumber() == null){
                throw new IllegalArgumentException("Registration Number should be issued");
            }
            if(!(command.getSigners().contains(output.getRto().getOwningKey()))){
                throw new IllegalArgumentException("RTO must sign");
            }
        }else if(command.getValue() instanceof Commands.Transfer){
            VehicleStateV3 output = (VehicleStateV3) tx.getOutput(0);
            VehicleStateV3 input = (VehicleStateV3) tx.getInput(0);

            if(input.getChallanValue() != 0 && input.getChallanValue() != null){
                throw new IllegalArgumentException("All Challans must be cleared for Vehicle Transfer");
            }

            if(output.getRedgNumber() == null)
                throw new IllegalArgumentException("Vehicle must be registered");

            if(!(command.getSigners().contains(output.getRto().getOwningKey()) && command.getSigners().contains(input.getOwner().getOwningKey())))
                throw new IllegalArgumentException("RTO & Owner must sign");
        }
        // New Verify Logic
        else if(command.getValue() instanceof Commands.IssueChallan){
            VehicleStateV3 output = (VehicleStateV3) tx.getOutput(0);
            if(output.getChallanValue() < 1)
                throw new IllegalArgumentException("Challan Value must be positive");
            if(!(command.getSigners().contains(output.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else if(command.getValue() instanceof Commands.SettleChallan){
            VehicleStateV3 input = (VehicleStateV3) tx.getInput(0);
            if(!(command.getSigners().contains(input.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else{
            throw new IllegalArgumentException("Invalid Command");
        }
    }


    @NotNull
    @Override
    public AttachmentConstraint getLegacyContractConstraint() {
        // Return the HashAttachment Constraint of the previous jar. Calculate the SHA256 hash of v1-contracts.jar file.
        // Run the CalculateSHA256Hash.java in the client module. It outputs the SHA256 hash on the console.
        return new HashAttachmentConstraint(SecureHash.parse("82F5D2FC022D1D8BE24AEEF6BCD1AF1CA908E277A44AE2D3CE86C6B9DE7892DC"));
    }

    @NotNull
    @Override
    public String getLegacyContract() {
        return VehicleContract.ID;
    }

    @NotNull
    @Override
    public VehicleStateV3 upgrade(@NotNull VehicleState state) {
        // State Upgrade Logic
        return new VehicleStateV3(state.getRedgNumber(), state.getOwner(), state.getRto(), null, null);
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Register implements Commands {}
        class Transfer implements Commands {}
        class IssueChallan implements Commands{}
        class SettleChallan implements Commands{}
    }
}