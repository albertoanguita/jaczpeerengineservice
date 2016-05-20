package jacz.peerengineservice.client.connection;

import com.google.gson.Gson;
import com.neovisionaries.i18n.CountryCode;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.util.io.http.HttpClient;
import jacz.util.lists.tuple.Duple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides methods for accessing the remote server functionality
 */
public class ServerAPI {

    public static final class RegistrationRequest {

        private final String peerID;

        public RegistrationRequest(PeerId peerId) {
            this.peerID = peerId.toString();
        }
    }

    private class RegistrationResponseJSON {

        private String response;
    }

    public enum RegistrationResponse {
        OK,
        ALREADY_REGISTERED
    }

    public static final class ConnectionRequest {

        private final String peerID;

        private final String localIPAddress;

        private final int localMainServerPort;

        private final int externalMainServerPort;

        private final String clientCountryCode;

        private final boolean wishRegularConnections;

        public ConnectionRequest(
                PeerId peerId,
                String localIPAddress,
                int localMainServerPort,
                int externalMainServerPort,
                CountryCode mainCountry,
                boolean wishRegularConnections) {
            this.peerID = peerId.toString();
            this.localIPAddress = localIPAddress;
            this.localMainServerPort = localMainServerPort;
            this.externalMainServerPort = externalMainServerPort;
            this.clientCountryCode = mainCountry.name();
            this.wishRegularConnections = wishRegularConnections;
        }
    }

    private class ConnectionResponseJSON {

        private String response;
        private String sessionID;
        private String minReminderTime;
        private String maxReminderTime;
    }

    public enum ConnectionResponseType {
        OK,
        // todo: make server return this if the public ip does not match what we are sending to him. Include public ip in connection message (@FUTURE@)
//        PUBLIC_IP_MISMATCH,
        UNREGISTERED_PEER,
        PEER_MAIN_SERVER_UNREACHABLE,
        PEER_REST_SERVER_UNREACHABLE,
        WRONG_AUTHENTICATION
    }

    public static class ConnectionResponse {

        private ConnectionResponseType response;

        private String sessionID;

        private long minReminderTime;

        private long maxReminderTime;

        public ConnectionResponseType getResponse() {
            return response;
        }

        public String getSessionID() {
            return sessionID;
        }

        public long getMinReminderTime() {
            return minReminderTime;
        }

        public long getMaxReminderTime() {
            return maxReminderTime;
        }

        private static ConnectionResponse buildConnectionResponse(ConnectionResponseJSON connectionResponseJSON) {
            ConnectionResponse connectionResponse = new ConnectionResponse();
            connectionResponse.response = ConnectionResponseType.valueOf(connectionResponseJSON.response);
            if (connectionResponse.response == ConnectionResponseType.OK) {
                connectionResponse.sessionID = connectionResponseJSON.sessionID;
                connectionResponse.minReminderTime = Long.parseLong(connectionResponseJSON.minReminderTime);
                connectionResponse.maxReminderTime = Long.parseLong(connectionResponseJSON.maxReminderTime);
            }
            return connectionResponse;
        }

        @Override
        public String toString() {
            return "ConnectionResponse{" +
                    "response=" + response +
                    ", sessionID='" + sessionID + '\'' +
                    ", minReminderTime=" + minReminderTime +
                    ", maxReminderTime=" + maxReminderTime +
                    '}';
        }
    }

    public static final class UpdateRequest {

        private final String sessionID;

        public UpdateRequest(String sessionID) {
            this.sessionID = sessionID;
        }
    }

    private class UpdateResponseJSON {

        private String response;
    }

    public enum RefreshResponse {

        OK,
        UNRECOGNIZED_SESSION,
        TOO_SOON
    }

    public enum DisconnectResponse {

        OK,
        UNRECOGNIZED_SESSION
    }

    public static final class InfoRequest {

        private final List<String> peerIDList;

        public InfoRequest(List<PeerId> peerIdList) {
            this.peerIDList = new ArrayList<>();
            for (PeerId peerId : peerIdList) {
                this.peerIDList.add(peerId.toString());
            }
        }
    }

    public static final class InfoResponseJSON {

        private List<PeerIDInfoJSON> peerIDInfoList;

        public InfoResponseJSON() {
            peerIDInfoList = new ArrayList<>();
        }
    }

    public static final class RegularPeersRequest {

        private final String clientCountryCode;

        public RegularPeersRequest(CountryCode clientCountryCode) {
            this.clientCountryCode = clientCountryCode.name();
        }
    }

    public static final class PeerIDInfoJSON {

        private String peerID;
        private String localIPAddress;
        private String externalIPAddress;
        private String localMainServerPort;
        private String localRESTServerPort;
        private String externalMainServerPort;
        private String externalRESTServerPort;
        private String clientCountryCode;
        private String wishRegularConnections;
    }

    public static final class InfoResponse {

        private final List<PeerIdInfo> peerIdInfoList;

        private InfoResponse(List<PeerIdInfo> peerIdInfoList) {
            this.peerIdInfoList = peerIdInfoList;
        }

        public List<PeerIdInfo> getPeerIdInfoList() {
            return peerIdInfoList;
        }

        private static InfoResponse buildInfoResponse(InfoResponseJSON infoResponseJson) {
            List<PeerIdInfo> peerIdInfoList = new ArrayList<>();
            for (PeerIDInfoJSON peerIDInfoJson : infoResponseJson.peerIDInfoList) {
                peerIdInfoList.add(PeerIdInfo.buildPeerIDInfo(peerIDInfoJson));
            }
            return new InfoResponse(peerIdInfoList);
        }

        @Override
        public String toString() {
            return "InfoResponse{" +
                    "peerIdInfoList=" + peerIdInfoList +
                    '}';
        }
    }

    public static final class PeerIdInfo {

        private final PeerId peerId;
        private final String localIPAddress;
        private final String externalIPAddress;
        private final int localMainServerPort;
        private final int localRESTServerPort;
        private final int externalMainServerPort;
        private final int externalRESTServerPort;
        private final CountryCode mainCountry;
        private final boolean wishRegularConnections;

        public PeerIdInfo(PeerId peerId,
                          String localIPAddress,
                          String externalIPAddress,
                          int localMainServerPort,
                          int localRESTServerPort,
                          int externalMainServerPort,
                          int externalRESTServerPort,
                          CountryCode mainCountry,
                          boolean wishRegularConnections) {
            this.peerId = peerId;
            this.localIPAddress = localIPAddress;
            this.externalIPAddress = externalIPAddress;
            this.localMainServerPort = localMainServerPort;
            this.localRESTServerPort = localRESTServerPort;
            this.externalMainServerPort = externalMainServerPort;
            this.externalRESTServerPort = externalRESTServerPort;
            this.mainCountry = mainCountry;
            this.wishRegularConnections = wishRegularConnections;
        }

        public PeerId getPeerId() {
            return peerId;
        }

        public String getLocalIPAddress() {
            return localIPAddress;
        }

        public String getExternalIPAddress() {
            return externalIPAddress;
        }

        public int getLocalMainServerPort() {
            return localMainServerPort;
        }

        public int getLocalRESTServerPort() {
            return localRESTServerPort;
        }

        public int getExternalMainServerPort() {
            return externalMainServerPort;
        }

        public int getExternalRESTServerPort() {
            return externalRESTServerPort;
        }

        public CountryCode getMainCountry() {
            return mainCountry;
        }

        public boolean isWishRegularConnections() {
            return wishRegularConnections;
        }

        private static PeerIdInfo buildPeerIDInfo(PeerIDInfoJSON peerIDInfoJson) {
            return new PeerIdInfo(
                    new PeerId(peerIDInfoJson.peerID),
                    peerIDInfoJson.localIPAddress,
                    peerIDInfoJson.externalIPAddress,
                    Integer.parseInt(peerIDInfoJson.localMainServerPort),
                    -1,
                    Integer.parseInt(peerIDInfoJson.externalMainServerPort),
                    -1,
                    CountryCode.valueOf(peerIDInfoJson.clientCountryCode),
                    Boolean.parseBoolean(peerIDInfoJson.wishRegularConnections)
            );
        }

        @Override
        public String toString() {
            return "PeerIdInfo{" +
                    "peerId=" + peerId +
                    ", localIPAddress='" + localIPAddress + '\'' +
                    ", externalIPAddress='" + externalIPAddress + '\'' +
                    ", localMainServerPort=" + localMainServerPort +
                    ", localRESTServerPort=" + localRESTServerPort +
                    ", externalMainServerPort=" + externalMainServerPort +
                    ", externalRESTServerPort=" + externalRESTServerPort +
                    ", mainCountry=" + mainCountry +
                    ", wishRegularConnections=" + wishRegularConnections +
                    '}';
        }
    }

    public static String hello(String serverURL) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "hello"),
                HttpClient.Verb.GET,
                HttpClient.ContentType.JSON);
        checkError(result);
        return result.element2;
    }

    public static RegistrationResponse register(String serverURL, RegistrationRequest registrationRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "register"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(registrationRequest));
        checkError(result);
        RegistrationResponseJSON registrationResponseJSON = new Gson().fromJson(result.element2, RegistrationResponseJSON.class);
        try {
            return RegistrationResponse.valueOf(registrationResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized register response", result.element2);
            throw e;
        }
    }

    public static ConnectionResponse connect(String serverURL, ConnectionRequest connectionRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "connect"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(connectionRequest));

        checkError(result);
        ConnectionResponseJSON connectionResponseJSON = new Gson().fromJson(result.element2, ConnectionResponseJSON.class);
        try {
            return ConnectionResponse.buildConnectionResponse(connectionResponseJSON);
        } catch (IllegalArgumentException e) {
            // unrecognized values --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized connect response", result.element2);
            throw e;
        }
    }

    public static RefreshResponse refresh(String serverURL, UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "refresh"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        UpdateResponseJSON updateResponseJSON = new Gson().fromJson(result.element2, UpdateResponseJSON.class);
        try {
            return RefreshResponse.valueOf(updateResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized refresh response", result.element2);
            throw e;
        }
    }

    public static DisconnectResponse disconnect(String serverURL, UpdateRequest updateRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "disconnect"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(updateRequest));
        checkError(result);
        UpdateResponseJSON updateResponseJSON = new Gson().fromJson(result.element2, UpdateResponseJSON.class);
        try {
            return DisconnectResponse.valueOf(updateResponseJSON.response);
        } catch (IllegalArgumentException e) {
            // unrecognized value --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized disconnect response", result.element2);
            throw e;
        }
    }

    public static InfoResponse info(String serverURL, InfoRequest infoRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "info"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(infoRequest));
        checkError(result);
        InfoResponseJSON infoResponseJson = new Gson().fromJson(result.element2, InfoResponseJSON.class);
        try {
            return InfoResponse.buildInfoResponse(infoResponseJson);
        } catch (Exception e) {
            // unrecognized values --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized info response", result.element2);
            throw new IOException("Unrecognized info response");
        }
    }

    public static InfoResponse regularPeersRequest(String serverURL, RegularPeersRequest regularPeersRequest) throws IOException, ServerAccessException {
        Duple<Integer, String> result = HttpClient.httpRequest(
                getURL(serverURL, "regular_peers_request"),
                HttpClient.Verb.POST,
                HttpClient.ContentType.JSON,
                HttpClient.ContentType.JSON,
                new Gson().toJson(regularPeersRequest));
        checkError(result);
        InfoResponseJSON infoResponseJson = new Gson().fromJson(result.element2, InfoResponseJSON.class);
        try {
            return InfoResponse.buildInfoResponse(infoResponseJson);
        } catch (Exception e) {
            // unrecognized values --> log error and re-throw
            PeerClient.reportFatalError("Unrecognized regularPeersRequest response", result.element2);
            throw new IOException("Unrecognized regularPeersRequest response");
        }
    }

    private static String getURL(String serverURL, String method) {
        return serverURL + method;
    }

    private static void checkError(Duple<Integer, String> response) throws ServerAccessException {
        if (response.element1 / 100 == 4 || response.element1 / 100 == 5) {
            throw new ServerAccessException(response.element2, response.element1);
        }
    }


//    public static void main(String[] args) throws Exception {
//        String serverURL = "https://testserver01-1100.appspot.com/_ah/api/server/v1/";
////        System.out.println(hello());
//
////        RegistrationResponse registrationResponse = register(serverURL, new RegistrationRequest(new PeerId("0000000000000000000000000000000000000000003")));
////
////        System.out.println(registrationResponse);
//
//
//        ConnectionResponse connectionResponse = connect(serverURL, new ConnectionRequest(new PeerId("0000000000000000000000000000000000000000003"), "192.168.1.1", 50000, 50001));
//        System.out.println(connectionResponse);
//
////        RefreshResponse refreshResponse = refresh(serverURL, new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIDvmYoJDA"));
////        System.out.println(refreshResponse);
//
////        UpdateResponse disconnectResponse = disconnect(serverURL, new UpdateRequest("ahNlfnRlc3RzZXJ2ZXIwMS0xMTAwchoLEg1BY3RpdmVTZXNzaW9uGICAgIC6jYkKDA"));
////        System.out.println(disconnectResponse);
//
//        List<PeerId> peerIdList = new ArrayList<>();
//        peerIdList.add(new PeerId("0000000000000000000000000000000000000000003"));
//        peerIdList.add(new PeerId("0000000000000000000000000000000000000000004"));
//        InfoResponse infoResponse = info(serverURL, new InfoRequest(peerIdList));
//        System.out.println(infoResponse);
//
//
//    }
}
