package jacz.peerengine.test.transfer;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 4/05/12<br>
 * Last Modified: 4/05/12
 */
public class TestTransfers {

    public static void main(String args[]) {


        TestTransfer_1.main(new String[0]);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        TestTransfer_2.main(new String[0]);
    }
}
