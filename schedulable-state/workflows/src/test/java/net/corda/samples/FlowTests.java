package net.corda.samples;

import com.google.common.collect.ImmutableList;
import net.corda.samples.flows.BidFlow;
import net.corda.samples.flows.CreateAuctionFlow;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlowTests {
    private  MockNetwork network; // = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
//        TestCordapp.findCordapp("net.corda.samples.flows"),
//        TestCordapp.findCordapp("net.corda.samples.contracts")
//    )));
    private  StartedMockNode a; // = network.createPartyNode(null);
    private  StartedMockNode b; // = network.createNode();

//    public FlowTests() {
//        a.registerInitiatedFlow(BidFlow.Responder.class);
//        //b.registerInitiatedFlow(Responder.class);
//    }



    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.flows"),
                TestCordapp.findCordapp("net.corda.samples.contracts")
        )));
        a = network.createPartyNode(null);
        b = network.createNode();
        a.registerInitiatedFlow(CreateAuctionFlow.Initiator.class);

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void dummyTest() {

    }
}
