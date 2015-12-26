package jacz.peerengineservice.util.tempfile_api;

import jacz.util.concurrency.concurrency_controller.ConcurrencyController;
import jacz.util.concurrency.concurrency_controller.ConcurrencyControllerReadWrite;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.concurrency.task_executor.TaskFinalizationIndicator;
import jacz.util.files.FileUtil;
import jacz.util.io.object_serialization.VersionedObjectSerializer;
import jacz.util.io.object_serialization.VersionedSerializationException;
import jacz.util.lists.tuple.Duple;
import jacz.util.numeric.range.LongRangeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * This class provides access to temporary files. This includes reading and writing access and controlling concurrency.
 * <p/>
 * Temporary files are structured in two different files: an index file and a data file. The index file contains
 * metadata about the data file. In concrete, it contains the segments that the data file is actually storing. The
 * data file contains the actual data in its final position.
 * <p/>
 * Temporary files are referred to with the name of any of its files, without extension
 * <p/>
 * Once a temporary file is completed, the index file is deleted. The user is expected to handle the generated data file as needed.
 * <p/>
 * All access (create, read, write, complete) to any temporary file is done through this manager class. This way,
 * concurrency issues are all handled at the same place. Several reads can work concurrently on the same file, but
 * only a write can happen at a time.
 * <p/>
 * The index file allows storing a generic object. This allows users to store custom metadata about the download that can
 * be retrieved after a download process has been interrupted (for example, to which library and which library item does
 * this file correspond).
 * <p/>
 * Initial structure (may change in the future):
 * - All disk space for the data file is allocated from a beginning
 * - Data is placed in its final location
 * - Generating the final file is simply renaming and moving the data file
 * - The index file contains the portions of file already completed (as long ranges). Since they are placed in their
 * final location, no mapping of ranges is needed
 * <p/>
 * Future structure (more optimal, but more complicated to code). No plans to implement it. It is good as it is.
 * - Random location of blocks by double indexing (allows to grow size of temp data file progressively)
 * - Background daemons optimizing the temp data file (ordering blocks, defragging...)
 * - Creation of final file is simply the total ordering of the temp data file and its renaming and reallocation
 * - No need for taking space for a second file during final file creation
 * - Optimizing also simplifies the index file a lot
 * - Final generation of the file can be allowing optimizers do all work
 * <p/>
 * Optimizers:
 * - Reorder blocks of the temp data file
 * - Blocks of up to around 10MB, otherwise too much memory would be consumed
 * - No optimizer should run when ANY file is being accessed
 * - Optimizers should run one by one
 * - They could be set up in groups, dividing their task in sub-tasks. This way if a file access comes, the global
 * optimizing task can be easily interrupted
 */
public class TempFileManager {

    /**
     * file pre-names and extensions
     */
    private static final String TEMP_FILE_NAME_INIT = "temp";

    private static final String TEMP_FILE_INDEX_NAME_END = ".ndx";

    private static final String TEMP_FILE_INDEX_BACKUP_NAME_END = ".bak";

    private static final String TEMP_FILE_DATA_NAME_END = ".dat";

    private static final int TEMP_FILE_INDEX_CRC_BYTES = 4;

    /**
     * Directory where temp files are stored (ending with the path.separator character)
     */
    private final String baseDir;

    private final TempFileManagerEventsBridge tempFileManagerEventsBridge;

    /**
     * This map stores concurrency controller objects for each temporary file. These objects are not created at
     * file creation time, but rather whenever the file is accessed. The concurrency controller objects ensure that
     * no forbidden concurrent accesses are carried out on one file. They allow several parallel reads but only
     * one write with no reads.
     * <p/>
     * The concurrency controller objects are destroyed one the file is completed
     * <p/>
     * This object also provides a way to store all active temp files
     */
    private final Map<String, ConcurrencyController> concurrencyControllers;

    /**
     * Constructor of the temporary file manager. A path to an existing directory is received. All files will be
     * created inside that directory. External changes on that dir should be avoided while on use
     *
     * @param baseDir the path to the directory where all files will be placed
     */
    public TempFileManager(String baseDir, TempFileManagerEvents tempFileManagerEvents) {
        this.baseDir = buildBaseDir(baseDir);
        this.tempFileManagerEventsBridge = new TempFileManagerEventsBridge(tempFileManagerEvents);
        concurrencyControllers = new HashMap<>();
    }

    /**
     * Sets up the base directory path, adding a file separator at the end if needed
     *
     * @param baseDir user given base directory path
     * @return correct base directory path (end file separator included)
     */
    private static String buildBaseDir(String baseDir) {
        if (!baseDir.endsWith(File.separator)) {
            baseDir = baseDir + File.separator;
        }
        return baseDir;
    }

    /**
     * Builds an array of two file pre-names for file name generation purposes
     */
    private static List<String> buildBaseFileNameList() {
        List<String> baseFileNameList = new ArrayList<>();
        baseFileNameList.add(TEMP_FILE_NAME_INIT);
        baseFileNameList.add(TEMP_FILE_NAME_INIT);
        baseFileNameList.add(TEMP_FILE_NAME_INIT);
        return baseFileNameList;
    }

    /**
     * Builds an array of two file extensions for file name generation purposes
     */
    private static List<String> buildExtensionList() {
        List<String> extensionList = new ArrayList<>();
        extensionList.add(TEMP_FILE_INDEX_NAME_END);
        extensionList.add(TEMP_FILE_INDEX_BACKUP_NAME_END);
        extensionList.add(TEMP_FILE_DATA_NAME_END);
        return extensionList;
    }

    /**
     * Retrieves the base directory for placing the temporary files
     *
     * @return the base directory for placing the temporary files
     */
    String getBaseDir() {
        return baseDir;
    }

    /**
     * Returns a set containing all existing temporary files in the base directory
     *
     * @return a set with the names of the temporary files contained in the base directory (base names, no extension)
     */
    public synchronized Set<String> getExistingTempFiles() {
        Set<String> tempFiles = new HashSet<>();
        try {
            String[] filesInBaseDir = FileUtil.getDirectoryContents(baseDir);
            for (String file : filesInBaseDir) {
                if (file.endsWith(TEMP_FILE_INDEX_NAME_END) && FileUtil.isFile(baseDir + file)) {
                    // an index file was found -> look for its data file
                    String tempFile = generateIndexFilePath(file);
                    try {
                        TempIndex tempIndex = readIndexFile(tempFile);
                        String tempDataFilePath = tempIndex.getTempDataFilePath();
                        if (FileUtil.isFile(tempDataFilePath)) {
                            // the data file also exists -> valid temp file
                            tempFileManagerEventsBridge.indexFileRecovered(tempFile);
                            tempFiles.add(file);
                        }
                    } catch (IOException | VersionedSerializationException e) {
                        // error reading the file, ignore
                    }
                }
            }
            return tempFiles;
        } catch (FileNotFoundException e) {
            // could not read from base dir -> return an empty set
            return tempFiles;
        }
    }

    /**
     * Creates a new temporary file (composed by an index file and a data file) with no generic user object.
     * The name of the index file (which will be used to refer to the temporary file) is returned
     *
     * @return the name of the temporary file (of the index file actually). We will have to refer to the file with
     * this name in order to access to it subsequently
     * @throws IOException there was an error generating the files
     */
    public synchronized String createNewTempFile(HashMap<String, Serializable> userDictionary) throws IOException {
        // generate the file names and the actual files
        List<Duple<String, String>> fileNames = FileUtil.createFiles(
                baseDir,
                buildBaseFileNameList(),
                buildExtensionList(),
                "_",
                "",
                false
        );
        generateInitialIndexFile(fileNames.get(0).element2, fileNames.get(2).element2, userDictionary);
        tempFileManagerEventsBridge.indexFileGenerated(fileNames.get(0).element2);
        return fileNames.get(0).element2;
    }

    /**
     * Creates in disk the physical file corresponding to a new index file
     *
     * @param indexFileName name of the index file
     * @param dataFileName  name of the data file
     * @throws IOException error creating the files
     */
    private void generateInitialIndexFile(String indexFileName, String dataFileName, HashMap<String, Serializable> userDictionary) throws IOException {
        TempIndex index = new TempIndex(baseDir + dataFileName, userDictionary);
        writeIndexFile(baseDir + indexFileName, index);
    }

    /**
     * Returns the size of a resource stored as a temporary file
     *
     * @param tempFileName the name of the temporary file to query
     * @return the size of a resource stored as a temporary file (null if not known)
     * @throws IOException there were errors accessing the given temporary file
     */
    public Long getTemporaryResourceSize(String tempFileName) throws IOException {
        TaskFinalizationIndicator tfi;
        GetSizeTask getSizeTask = new GetSizeTask(this, generateIndexFilePath(tempFileName));
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    getSizeTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
        tfi.waitForFinalization();
        return getSizeTask.getSize();
    }

    /**
     * Sets the size of a resource stored as a temporary file
     *
     * @param tempFileName the name of the temporary file to query
     * @param size         the new size for the resource
     * @throws IOException there were errors accessing the given temporary file
     */
    public void setTemporaryResourceSize(String tempFileName, long size) throws IOException {
        TaskFinalizationIndicator tfi;
        SetSizeTask setSizeTask = new SetSizeTask(this, generateIndexFilePath(tempFileName), size);
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    setSizeTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.WRITE_ACTIVITY);
        }
        tfi.waitForFinalization();
        setSizeTask.checkCorrectResult();
    }

    /**
     * Returns the owned parts of a resource stored as a temporary file
     *
     * @param tempFileName the name of the temporary file to query
     * @return a range set of long ranges representing the stored segments of the resource
     * @throws IOException there were errors accessing the given temporary file
     */
    public LongRangeList getTemporaryOwnedParts(String tempFileName) throws IOException {
        TaskFinalizationIndicator tfi;
        OwnedPartsTask ownedPartsTask = new OwnedPartsTask(this, generateIndexFilePath(tempFileName));
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    ownedPartsTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
        tfi.waitForFinalization();
        return ownedPartsTask.getOwnedParts();
    }

    /**
     * This method tells the temp file manager that a specific temporary file has been completed and thus it is no
     * longer necessary. The method removes the index file and returns the path to the data file (which is the
     * actual downloaded file). The user is expected to remove the file from the temp directory and to place the
     * file somewhere else (it is not a good idea to keep files in this dir).
     * <p/>
     * The method does not check if the file was actually 100% complete or not. The user is expected to invoke it
     * appropriately
     *
     * @param tempFileName name of the file to complete
     * @return the path to the data file
     * @throws IOException problems accessing the index file
     */
    public String completeTempFile(String tempFileName) throws IOException {
        TaskFinalizationIndicator tfi;
        CompleterTask completerTask = new CompleterTask(this, generateIndexFilePath(tempFileName));
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    completerTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.WRITE_ACTIVITY);
        }
        tfi.waitForFinalization();
        // the concurrency controller is no longer needed, remove it
        synchronized (this) {
            concurrencyControllers.remove(tempFileName);
        }
        return completerTask.getFinalPath();
    }

    /**
     * This method tells the temp file manager that a specific temporary file must be removed. Both the index and the data files will be removed
     * from disk.
     *
     * @param tempFileName name of the file to remove
     * @throws IOException problems accessing the index file
     */
    public synchronized void removeTempFile(String tempFileName) throws IOException {
        String dataFile = completeTempFile(tempFileName);
        try {
            FileUtil.deleteFile(dataFile);
        } catch (FileNotFoundException e) {
            // ignore
        }
    }

    public HashMap<String, Serializable> getUserDictionary(String tempFileName) throws IOException {
        TaskFinalizationIndicator tfi;
        GetUserDictionary getUserDictionary = new GetUserDictionary(this, generateIndexFilePath(tempFileName));
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    getUserDictionary,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
        tfi.waitForFinalization();
        return getUserDictionary.getUserDictionary();
    }

    public HashMap<String, Serializable> getSystemDictionary(String tempFileName) throws IOException {
        TaskFinalizationIndicator tfi;
        GetSystemDictionary getSystemDictionary = new GetSystemDictionary(this, generateIndexFilePath(tempFileName));
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    getSystemDictionary,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
        tfi.waitForFinalization();
        return getSystemDictionary.getSystemDictionary();
    }

    public void setSystemField(String tempFileName, String key, Serializable value) throws IOException {
        TaskFinalizationIndicator tfi;
        SetSystemField setSystemField = new SetSystemField(this, generateIndexFilePath(tempFileName), key, value);
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    setSystemField,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.WRITE_ACTIVITY);
        }
        tfi.waitForFinalization();
        setSystemField.checkCorrectResult();
    }

    /**
     * Reads a section of a temporary file
     *
     * @param tempFileName the name of the temporary file
     * @param offset       offset for reading
     * @param length       length of data to read
     * @return an array of bytes containing the data read
     * @throws IOException               problems accessing the index file
     * @throws IndexOutOfBoundsException tried to read data outside the bounds of the temporary file
     */
    public byte[] read(String tempFileName, long offset, int length) throws IOException, IndexOutOfBoundsException {
        TaskFinalizationIndicator tfi;
        ReaderTask readerTask = new ReaderTask(this, generateIndexFilePath(tempFileName), offset, length);
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    readerTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.READ_ACTIVITY);
        }
        tfi.waitForFinalization();
        return readerTask.getData();
    }

    /**
     * Writes a chunk of data into a temporary file
     *
     * @param tempFileName the name of the temporary file
     * @param offset       offset for writing
     * @param data         data to be written
     * @throws IOException               problems accessing the index file
     * @throws IndexOutOfBoundsException tried to write data outside the bounds of the temporary file
     */
    public void write(String tempFileName, long offset, byte[] data) throws IOException, IndexOutOfBoundsException {
        TaskFinalizationIndicator tfi;
        WriterTask writerTask = new WriterTask(this, generateIndexFilePath(tempFileName), offset, data);
        synchronized (this) {
            tfi = ParallelTaskExecutor.executeTask(
                    writerTask,
                    accessTempFileConcurrencyController(tempFileName),
                    ConcurrencyControllerReadWrite.WRITE_ACTIVITY);

        }
        tfi.waitForFinalization();
        writerTask.checkCorrectResult();
    }

    private ConcurrencyController accessTempFileConcurrencyController(String tempFileName) {
        if (!concurrencyControllers.containsKey(tempFileName)) {
            concurrencyControllers.put(tempFileName, new ConcurrencyController(new ConcurrencyControllerReadWrite()));
        }
        return concurrencyControllers.get(tempFileName);
    }

    private String generateIndexFilePath(String tempFileName) {
        return baseDir + tempFileName;
    }

    TempIndex readIndexFile(String indexFilePath) throws IOException, VersionedSerializationException {
        try {
            TempIndex tempIndex = new TempIndex(indexFilePath, generateBackupPath(indexFilePath));
            if (tempIndex.getDeserializeTries() > 0) {
                tempFileManagerEventsBridge.indexFileErrorRestoredWithBackup(indexFilePath);
            }
            return tempIndex;
        } catch (IOException | VersionedSerializationException e) {
            // could not restore data from backup
            tempFileManagerEventsBridge.indexFileError(indexFilePath);
            throw e;
        }
    }

    static void writeIndexFile(String indexFilePath, TempIndex index) throws IOException {
        VersionedObjectSerializer.serialize(index, TEMP_FILE_INDEX_CRC_BYTES, indexFilePath, generateBackupPath(indexFilePath));
    }

    public static String generateBackupPath(String indexFilePath) {
        return indexFilePath.replace(TEMP_FILE_INDEX_NAME_END, TEMP_FILE_INDEX_BACKUP_NAME_END);
    }

    public synchronized void stop() {
        tempFileManagerEventsBridge.stop();
    }
}
