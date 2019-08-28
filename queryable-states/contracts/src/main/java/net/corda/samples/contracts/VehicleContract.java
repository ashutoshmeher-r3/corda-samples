package net.corda.samples.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

// ************
// * Contract *
// ************
public class VehicleContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "VehicleContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {}

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class VehicleSale implements Commands {}
    }
}