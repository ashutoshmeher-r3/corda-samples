package corda.samples.upgrades.contracts.v2;

import corda.samples.upgrades.contracts.VehicleContract;
import corda.samples.upgrades.states.VehicleState;
import corda.samples.upgrades.states.v2.VehicleStateV2;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.UpgradedContract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

// ************
// * VehicleContractV2 *
//  Upgraded Contracts must implement the UpgradedContract interface for explicit upgrades.
// ************
public class VehicleContractV2 implements UpgradedContract<VehicleState, VehicleStateV2> {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "corda.samples.upgrades.contracts.v2.VehicleContractV2";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.Register){
            VehicleStateV2 output = (VehicleStateV2) tx.getOutput(0);
            if(output.getRedgNumber() == null){
                throw new IllegalArgumentException("Registration Number should be issued");
            }
            if(!(command.getSigners().contains(output.getRto().getOwningKey()))){
                throw new IllegalArgumentException("RTO must sign");
            }
        }else if(command.getValue() instanceof Commands.Transfer){
            VehicleStateV2 output = (VehicleStateV2) tx.getOutput(0);
            VehicleStateV2 input = (VehicleStateV2) tx.getInput(0);

            if(input.getChallanValue() != 0 && input.getChallanValue() != null){
                throw new IllegalArgumentException("All Challans must be cleared for Vehicle Transfer");
            }

            if(output.getRedgNumber() == null)
                throw new IllegalArgumentException("Vehicle must be registered");

            if(!(command.getSigners().contains(output.getRto().getOwningKey()) && command.getSigners().contains(input.getOwner().getOwningKey())))
                throw new IllegalArgumentException("RTO & Owner must sign");

        }else if(command.getValue() instanceof Commands.IssueChallan){
            VehicleStateV2 output = (VehicleStateV2) tx.getOutput(0);
            if(output.getChallanValue() < 1)
                throw new IllegalArgumentException("Challan Value must be positive");
            if(!(command.getSigners().contains(output.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else if(command.getValue() instanceof Commands.SettleChallan){
            VehicleStateV2 input = (VehicleStateV2) tx.getInput(0);
            if(!(command.getSigners().contains(input.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else{
            throw new IllegalArgumentException("Invalid Command");
        }
    }

    @NotNull
    @Override
    public String getLegacyContract() {
        return VehicleContract.ID;
    }

    @NotNull
    @Override
    public VehicleStateV2 upgrade(@NotNull VehicleState state) {
        // Define state upgrade logic
        return new VehicleStateV2(state.getRedgNumber(),
                state.getOwner(), state.getRto(), null, null);
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Register implements Commands {}
        class Transfer implements Commands {}
        class IssueChallan implements Commands{}
        class SettleChallan implements Commands{}
    }
}