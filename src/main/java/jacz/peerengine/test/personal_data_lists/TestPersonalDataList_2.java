package jacz.peerengine.test.personal_data_lists;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 4/05/12<br>
 * Last Modified: 4/05/12
 */
public class TestPersonalDataList_2 /*implements ParallelTask*/ {

//    public static void main(String args[]) {
//    }
//
//    @Override
//    public void performTask() {
//        String config = ".\\trunk\\src\\com.jacuzzi.peerengine\\test\\personal_data_lists\\clientConf_2.xml";
//
//        try {
//            List<Object> data = PeerClientConfigIO.readPeerClientData(XMLDom.parse(config));
//            PersonalData personalData = (PersonalData) data.get(0);
//            PeerClientData peerClientData = (PeerClientData) data.get(1);
//            PeerRelations peerRelations = (PeerRelations) data.get(2);
//
//            SimplePeerClientActionImplCustom simplePeerClientActionImplCustom = new SimplePeerClientActionImplCustom(new ForeignPeerDataActionImpl());
//            Client client = new Client(personalData, peerClientData, peerRelations, simplePeerClientActionImplCustom, new HashMap<String, PeerFSMFactory>(), false);
//            simplePeerClientActionImplCustom.setBasicListContainer(client.getBasicListContainer());
//            simplePeerClientActionImplCustom.setListSynchronizer(client.getPeerClient().getListSynchronizer());
//
//            client.startClient();
//
//            if (true) {
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("2 cambia nick a bbb");
//                client.getOwnData().setNick("bbb");
//                client.getPeerClient().broadcastObjectMessage(new ModifiedPersonalDataNotification());
//
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("2 cambia state a BUSY");
//                client.getOwnData().setState(SimplePersonalData.State.BUSY);
//                client.getPeerClient().broadcastObjectMessage(new ModifiedPersonalDataNotification());
//
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                System.out.println("2 cambia message a joderrr");
//                client.getOwnData().setMessage("joderrr");
//                client.getPeerClient().broadcastObjectMessage(new ModifiedPersonalDataNotification());
//
//                for (int i = 0; i < 20; i++) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    client.getPeerClient().broadcastObjectMessage(new ModifiedPersonalDataNotification());
//                }
//
//                /*try {
//           Thread.sleep(10000);
//       } catch (InterruptedException e) {
//           e.printStackTrace();
//       }
//       System.out.println("2 bloquea a 1");
//       client.getPeerClient().addBlockedPeer(PeerIDGenerator.peerID(1));
//
//       try {
//           Thread.sleep(3000);
//       } catch (InterruptedException e) {
//           e.printStackTrace();
//       }
//       System.out.println("2 quita bloqueo a 1");
//       client.getPeerClient().removeBlockedPeer(PeerIDGenerator.peerID(1));
//
//       try {
//           Thread.sleep(15000);
//       } catch (InterruptedException e) {
//           e.printStackTrace();
//       }
//       System.out.println("2 añade a 1");
//       client.getPeerClient().addFriendPeer(PeerIDGenerator.peerID(1));
//       System.out.println("2: el estado de 1 es ahora " + client.getPeerClient().getPeerConnectionStatus(PeerIDGenerator.peerID(1)));*/
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (XMLStreamException e) {
//            e.printStackTrace();
//        }
//    }
}
