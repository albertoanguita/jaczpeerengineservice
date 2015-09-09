package jacz.peerengine.util.datatransfer.resource_accession;

import jacz.util.numeric.LongRange;
import jacz.util.numeric.RangeSet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * This interface contains methods for generating a resource from the received byte arrays from other peers. Using
 * this allows to use not only files, but other types of resources
 */
public interface ResourceWriter {

    /**
     * This method allows the resource writer informing if the size of the resource is already known
     * <p/>
     * This method is invoked by the resource streaming manager upon a download request, to gather data about what
     * needs to be downloaded (it is only invoked once, before any other methods). In case the size is already
     * known by the resource writer, the init method will not be invoked. If the size is unknown, the init method
     * will be invoked before any write method to report the resource size
     *
     * @return a Long object containing the size of the resource in bytes, or null if size is not known yet)
     * @throws IOException problems reading the resource data
     */
    public Long getSize() throws IOException;

    /**
     * This method allows the resource writer informing about what parts of the resource it currently owns
     * <p/>
     * This method is invoked by the resource streaming manager upon a download request, to gather data about what
     * needs to be downloaded (it is only invoked once, right after the getSize method). The parts already owned
     * will not be downloaded again. This is useful if a download was stopped and recovered in a later execution.
     *
     * @return a ResourcePart object containing the segments that this resource writer has already received (through
     *         the write method), or null if it has not even been initialized with the resource size yet
     * @throws IOException problems reading the resource data
     */
    public RangeSet<LongRange, Long> getAvailableSegments() throws IOException;

    public void setUserGenericData(String group, Map<String, Serializable> userGenericData) throws IOException;

    public Map<String, Serializable> getUserGenericData(String group) throws IOException;

    public void setUserGenericDataField(String group, String key, Serializable value) throws IOException;

    public Serializable getUserGenericDataField(String group, String key) throws IOException;

    /**
     * Initializes the resource writer by providing the size of the resource to write. This method is only invoked at the beginning of a writing
     * process, and only if the size of the resource was not previously known
     *
     * @param size the total size of the resource
     * @throws IOException errors setting the size of the resource
     */
    public void init(long size) throws IOException;

    /**
     * Writes a chunk of data into the resource
     *
     * @param offset offset for writing
     * @param data   data to be written
     * @throws IOException               problems writing the data into the resource
     * @throws IndexOutOfBoundsException tried to write data outside the bounds of the temporary file
     */
    public void write(long offset, byte[] data) throws IOException, IndexOutOfBoundsException;

    /**
     * This method is invoked after all data has been written into the resource, to notify the resource writer that the writing process is over.
     * The resource should remain in a final state so the user can use it as desired
     *
     * @throws IOException there were problems completing the resource
     */
    public void complete() throws IOException;

    /**
     * This method is invoked to indicate that the writing process has been cancelled. The resource should be erased, as no more writings will
     * be performed and the resource will no longer be used
     */
    public void cancel();

    /**
     * The client issued a stop order over this download (intended for backing up the download and recover it in
     * a subsequent execution). The resource writer should close all open IO resources, but without deleting the
     * achieved part. It should also be able to remember which parts of the resource were downloaded, and the size
     * of the resource (if it has the size already), since it will be asked that the next time the download is resumed
     */
    public void stop();

    /**
     * This method retrieves the final location of the resource in the disk. This method is never invoked by the peer engine. It is rather
     * focused at the user himself.
     *
     * @return the path to the file that contains the resource in the disk (or null if the resource was not stored in the disk)
     */
    public String getPath();
}
