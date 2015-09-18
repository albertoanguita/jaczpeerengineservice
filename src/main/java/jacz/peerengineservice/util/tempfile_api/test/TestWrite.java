package jacz.peerengineservice.util.tempfile_api.test;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 19-may-2010<br>
 * Last Modified: 19-may-2010
 */
public class TestWrite {

    public static void main(String args[]) {

        /*String dir = ".\\trunk\\src\\com.jacuzzi.peerengineservice\\util\\tempfile_api\\test\\files\\";

        TempFileManager tempFileManager = new TempFileManager(dir);

        String indexFile = null;
        try {
            indexFile = tempFileManager.createNewTempFile(8, dir + "finalfile.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = new byte[8];
        data[0] = 100;
        data[1] = 101;
        data[2] = 102;
        data[3] = 103;
        data[4] = 104;
        data[5] = 105;
        data[6] = 106;
        data[7] = 107;
        TaskFinalizationIndicator tfi = tempFileManager.write(indexFile, 0, data);
        tfi.waitForFinalization();

        tfi = tempFileManager.completeTempFile(indexFile);
        tfi.waitForFinalization();


        System.out.println("FIN");*/
    }
}
