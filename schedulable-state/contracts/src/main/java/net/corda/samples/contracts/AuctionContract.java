package net.corda.samples.contracts;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.states.AuctionState;

// ************
// * Contract *
// ************
public class AuctionContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "net.corda.samples.contracts.AuctionContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        if(tx.getCommands().size() == 0){
            throw new IllegalArgumentException("One command Expected");
        }

        Command command = tx.getCommand(0);
        if(command.getValue() instanceof Commands.CreateAuction)
            verifyAuctionCreation(tx);

        else if(command.getValue() instanceof Commands.Bid)
            verifyBid(tx);

        else if (command.getValue() instanceof Commands.EndAuction)
            verifyEndAuction(tx);

        else
            throw new IllegalArgumentException("Invalid Command");

    }

    private void verifyAuctionCreation(LedgerTransaction tx){
        // Auction Creation Contract Verification Logic goes here

    }

    private void verifyBid(LedgerTransaction tx){
        // Bid Contract Verification Logic goes here
        if(tx.getInputStates().size() != 1) throw new IllegalArgumentException("One Input Expected");
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");

        AuctionState inputState = (AuctionState)tx.getInput(0);
        AuctionState outputState = (AuctionState) tx.getOutput(0);

        if(!inputState.getActive()) throw new IllegalArgumentException("Auction has Ended");

        if(outputState.getCurrentHighestBid() < inputState.getBasePrice())
            throw new IllegalArgumentException("Bid Price should be greater than base price");

        if(inputState.getCurrentHighestBid() != null && outputState.getCurrentHighestBid() <= inputState.getCurrentHighestBid())
            throw new IllegalArgumentException("Bid Price should be greater than previous highest bid");
    }

    private void verifyEndAuction(LedgerTransaction tx){
        // End Auction Contract Verification Logic goes here
        if(tx.getOutputStates().size() != 1) throw new IllegalArgumentException("One Output Expected");
        Command command = tx.getCommand(0);
        if(!(command.getSigners().contains(((AuctionState)tx.getOutput(0)).getAuctioner().getOwningKey())))
            throw new IllegalArgumentException("Auctioner Signature Required");
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class CreateAuction implements Commands {}
        class Bid implements Commands {}
        class EndAuction implements Commands {}
    }

}