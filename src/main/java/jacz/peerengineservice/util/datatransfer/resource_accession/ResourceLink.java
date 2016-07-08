package jacz.peerengineservice.util.datatransfer.resource_accession;

import org.aanguita.jacuzzi.numeric.range.LongRange;

/**
 * This interface contains the methods for implementing a resource link. A resource link is the access point that we
 * to retrieve resources shared by external providers. We obtain these links by asking them to the appropriate resource
 * providers.
 * <p/>
 * A resource link is used by a resource streaming master, who issues requests to him. He will do its best to attend
 * those requests.
 * <p/>
 * It must be noted that resource links work at the receiving side. A resource link must actually hide the
 * communications with the other end, making this side believe that it is working locally. The actual implementation
 * will be in charge of issuing the messages to its final destination, whatever it is. Incoming communications are
 * however not handled by resource links, as these will directly reach the resource streaming master.
 */
public interface ResourceLink {

    long recommendedMillisForRequest();

    Long surviveTimeMillis();

    void initialize(Object initializationMessage) throws IllegalArgumentException;

    void requestResourceLength();

    void requestAvailableSegments();

    void requestAssignedSegments();

    void eraseSegments();

    void addNewSegment(LongRange segment);

    void hardThrottle(float variation);

    void softThrottle();

    /**
     * Keep the resource link alive
     */
     void ping();

     void die();
}
