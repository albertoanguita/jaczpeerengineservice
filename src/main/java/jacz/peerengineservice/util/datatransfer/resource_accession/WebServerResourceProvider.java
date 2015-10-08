package jacz.peerengineservice.util.datatransfer.resource_accession;

import jacz.peerengineservice.util.datatransfer.ResourceStreamingManager;

import java.net.URL;

/**
 *
 */
public class WebServerResourceProvider implements ResourceProvider {

    private final URL url;

    private ResourceStreamingManager resourceStreamingManager;

    public WebServerResourceProvider(URL url, ResourceStreamingManager resourceStreamingManager) {
        this.url = url;
        this.resourceStreamingManager = resourceStreamingManager;
    }

    @Override
    public String getID() {
        return url.toString();
    }

    @Override
    public Type getType() {
        return Type.WEB;
    }

    @Override
    public ResourceLink requestResource(String storeName, String resourceID, short assignedSubchannel) {
        return new WebServerResourceLink(resourceStreamingManager, url);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebServerResourceProvider that = (WebServerResourceProvider) o;

        if (!getID().equals(that.getID())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getID().hashCode();
    }
}
