package jacz.peerengineservice.util.datatransfer;

import jacz.peerengineservice.util.datatransfer.resource_accession.ResourceReader;

/**
 * Response to a request for downloading a resource from other peer
 */
public class ResourceStoreResponse {

    /**
     * Possible response values
     */
    public enum Response {
        // the client did not find the requested file
        RESOURCE_NOT_FOUND,
        // the client denied the requested file for this peer
        REQUEST_DENIED,
        // the client is too busy to approve the request now, but it can be tried later
        TOO_BUSY,
        // the client accepted the request, the file is provided
        REQUEST_APPROVED
    }

    /**
     * Type of response
     */
    private final Response response;

    /**
     * Resource reader for uploading the requested resource, if request was approved
     */
    private final ResourceReader resourceReader;

    /**
     * Build a resource not found response
     *
     * @return a resource not found response
     */
    public static ResourceStoreResponse resourceNotFound() {
        return new ResourceStoreResponse(Response.RESOURCE_NOT_FOUND);
    }

    /**
     * Build a request denied response
     *
     * @return a request denied response
     */
    public static ResourceStoreResponse requestDenied() {
        return new ResourceStoreResponse(Response.REQUEST_DENIED);
    }

    /**
     * Build a too busy response
     *
     * @return a too busy response
     */
    public static ResourceStoreResponse tooBusy() {
        return new ResourceStoreResponse(Response.TOO_BUSY);
    }

    /**
     * Build a request approved response
     *
     * @param resourceReader the resource reader associated to the requested resource
     * @return a request approved response
     */
    public static ResourceStoreResponse resourceApproved(ResourceReader resourceReader) {
        return new ResourceStoreResponse(resourceReader);
    }

    /**
     * Resource not found or request denied constructor
     *
     * @param response response value
     */
    private ResourceStoreResponse(Response response) {
        this.response = response;
        this.resourceReader = null;
    }

    /**
     * Request approved constructor
     *
     * @param resourceReader resource reader
     */
    private ResourceStoreResponse(ResourceReader resourceReader) {
        this.response = Response.REQUEST_APPROVED;
        this.resourceReader = resourceReader;
    }

    /**
     * Retrieves the response value
     *
     * @return response value
     */
    public Response getResponse() {
        return response;
    }

    /**
     * Retrieves the resource reader, in case the request is approved (null otherwise)
     *
     * @return the resource reader
     */
    public ResourceReader getResourceReader() {
        return resourceReader;
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
