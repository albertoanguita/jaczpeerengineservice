package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;
import jacz.util.numeric.LongRange;

import java.net.URL;

/**
 * A resource link served via web
 */
public class WebServerResourceLink implements ResourceLink {

    private final ResourceStreamingManager resourceStreamingManager;

    private final URL resourceURL;

    public WebServerResourceLink(ResourceStreamingManager resourceStreamingManager, URL resourceURL) {
        this.resourceStreamingManager = resourceStreamingManager;
        this.resourceURL = resourceURL;
    }

    @Override
    public long recommendedMillisForRequest() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Long surviveTimeMillis() {
        return null;
    }

    @Override
    public void initialize(Object initializationMessage) throws IllegalArgumentException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void requestAvailableSegments() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void requestResourceLength() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void requestAssignedSegments() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void eraseSegments() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addNewSegment(LongRange segment) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void hardThrottle(float variation) {

    }

    @Override
    public void softThrottle() {

    }

    @Override
    public void ping() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void die() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
