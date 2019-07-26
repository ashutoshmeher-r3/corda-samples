package corda.samples.upgrades.contracts;

import corda.samples.upgrades.states.VehicleState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

// ************
// * VehicleContract *
// ************
public class VehicleContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "corda.samples.upgrades.contracts.VehicleContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        Command command = tx.getCommand(0);
        if (command.getValue() instanceof Commands.Register){
            VehicleState output = (VehicleState) tx.getOutput(0);
            if(output.getRedgNumber() == null){
                throw new IllegalArgumentException("Registration Number should be issued");
            }
            if(!(command.getSigners().contains(output.getRto().getOwningKey()))){
                throw new IllegalArgumentException("RTO must sign");
            }
        }else if(command.getValue() instanceof Commands.Transfer){
            VehicleState output = (VehicleState) tx.getOutput(0);
            VehicleState input = (VehicleState) tx.getInput(0);
            if(output.getRedgNumber() == null)
                throw new IllegalArgumentException("Vehicle must be registered");

            if(!(command.getSigners().contains(output.getRto().getOwningKey()) && command.getSigners().contains(input.getOwner().getOwningKey())))
                throw new IllegalArgumentException("RTO & Owner must sign");

            // Change in verify logic in version 3, to restrict transfer of vehicle with pending challans.
            if(input.getChallanValue() != 0 && input.getChallanValue() != null){
                throw new IllegalArgumentException("All Challans must be cleared for Vehicle Transfer");
            }
        }

        // New verify Logic for IssueChallan and SettleChallan commands in version 2.
        else if(command.getValue() instanceof Commands.IssueChallan){
            VehicleState output = (VehicleState) tx.getOutput(0);
            if(output.getChallanValue() < 1)
                throw new IllegalArgumentException("Challan Value must be positive");
            if(!(command.getSigners().contains(output.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else if(command.getValue() instanceof Commands.SettleChallan){
            VehicleState input = (VehicleState) tx.getInput(0);
            if(!(command.getSigners().contains(input.getChallanIssuer().getOwningKey())))
                throw new IllegalArgumentException("Challan Issuer must sign");
        }else{
            throw new IllegalArgumentException("Invalid Command");
        }
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Register implements Commands {}
        class Transfer implements Commands {}
        class IssueChallan implements Commands{}
        class SettleChallan implements Commands{}
    }
}