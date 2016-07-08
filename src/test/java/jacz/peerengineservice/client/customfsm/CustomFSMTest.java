package jacz.peerengineservice.client.customfsm;

import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.*;
import org.aanguita.jacuzzi.concurrency.ThreadUtil;
import org.aanguita.jacuzzi.lists.tuple.FiveTuple;
import org.aanguita.jacuzzi.lists.tuple.SixTuple;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom FSM tests
 */
public class CustomFSMTest {

    private static final long WARM_UP = 20000;

    private static final long CYCLE_LENGTH = 5000;

    @org.junit.Test
    public void customFSM1() throws Exception {
        String config = "./etc/tests/clientConf_1_new.xml";
        String userDir = "./etc/tests/client1";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), new HashMap<>());
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);
        AskFilesFSM askFilesFSM = new AskFilesFSM();
        client.getPeerClient().registerTimedCustomFSM(PeerIdGenerator.peerID(2), askFilesFSM, ProvideFilesFSM.SERVER_FSM, 5000);

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertTrue(askFilesFSM.isSuccess());
        client.stopClient();
    }

    @org.junit.Test
    public void customFSM2() throws Exception {
        String config = "./etc/tests/clientConf_2_new.xml";
        String userDir = "./etc/tests/client2";
        SixTuple<PeerId, String, String, String, String, String> data = ConfigReader.readPeerClientData(config, userDir);
        PeerId ownPeerId = data.element1;
        String networkConfiguration = data.element2;
        String peersPersonalData = data.element3;
        String peerRelations = data.element4;
        String peerConnectionConfig = data.element5;
        String transferStatistics = data.element6;

        Map<String, PeerFSMFactory> customFSMs = new HashMap<>();
        ProvideFilesFSMFactory provideFilesFSMFactory = new ProvideFilesFSMFactory();
        customFSMs.put(ProvideFilesFSM.SERVER_FSM, provideFilesFSMFactory);

        Client client = new Client(ownPeerId, networkConfiguration, peersPersonalData, peerRelations, peerConnectionConfig, transferStatistics, new GeneralEventsImpl(), new ConnectionEventsImpl(), new PeersEventsImpl(), customFSMs);
        client.startClient();

        ThreadUtil.safeSleep(WARM_UP);

        ThreadUtil.safeSleep(CYCLE_LENGTH);
        Assert.assertTrue(provideFilesFSMFactory.getProvideFilesFSM().isSuccess());
        client.stopClient();
    }
}
