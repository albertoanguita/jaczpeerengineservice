package jacz.peerengineservice.util.data_synchronization;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.util.hash.CRC;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.log.ErrorLog;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Server FSM for data synchronization
 * <p/>
 * Protocol is described in client FSM
 */
public class DataSynchServerFSM implements PeerTimedFSMAction<DataSynchServerFSM.State> {

    /**
     * Name of this custom FSM (must start with a special character because it is not user made)
     */
    public static final String CUSTOM_FSM_NAME = PeerClient.OWN_CUSTOM_PREFIX + "DataSynchronizerFSM";

    enum State {
        // Initial state: the server peer waits for details of the request
        // if ok, we send an OK to the client, and initiate the synch
        WAITING_FOR_REQUEST,

        // Synch state. We wait for ok confirmations from the client, indicating that he received
        // the last data packet, to send more packets
        SYNCHING,

        // successfully synched all data
        // FINAL STATE
        SUCCESS,

        // error during the communication
        // FINAL STATE
        ERROR,

        // initial request denied
        // FINAL STATE
        DENIED,
    }

    public enum SynchRequestAnswer {
        INVALID_REQUEST_FORMAT,
        UNKNOWN_DATA_ACCESSOR,
        REQUEST_DENIED,
        SERVER_BUSY,
        OK
    }

    static class ElementPacket implements Serializable {

        final boolean SERVER_ERROR;

        final List<Serializable> elementPacket;

        final int elementsSent;

        final int totalElementsToSend;

        public ElementPacket(List<Serializable> elementPacket, int elementsSent, int totalElementsToSend) {
            this(false, elementPacket, elementsSent, totalElementsToSend);
        }

        private ElementPacket(boolean SERVER_ERROR, List<Serializable> elementPacket, int elementsSent, int totalElementsToSend) {
            this.SERVER_ERROR = SERVER_ERROR;
            this.elementPacket = elementPacket;
            this.elementsSent = elementsSent;
            this.totalElementsToSend = totalElementsToSend;
        }

        public static ElementPacket generateError() {
            return new ElementPacket(true, null, 0, 0);
        }
    }

    private byte outgoingChannel;

    private UniqueIdentifier fsmID;

    private final DataSynchEventsBridge dataSynchEventsBridge;

    private final PeerID clientPeerID;

    private final DataAccessorContainer dataAccessorContainer;

    private String dataAccessorName;

    private DataAccessor dataAccessor;

    private List<? extends Serializable> elementsToSend;

    private int elementsPerMessage;

    private int CRCBytes;

    private int elementToSendIndex;

    /**
     * Progress notifier for the server side. It is obtained form the list accessor (null if not used)
     */
    ProgressNotificationWithError<Integer, SynchError> progress;

    private SynchError synchError;

    /**
     * Class constructor
     */
    public DataSynchServerFSM(DataSynchEventsBridge dataSynchEventsBridge, PeerID clientPeerID, DataAccessorContainer dataAccessorContainer) {
        this.dataSynchEventsBridge = dataSynchEventsBridge;
        this.clientPeerID = clientPeerID;
        this.dataAccessorContainer = dataAccessorContainer;
        this.dataAccessorName = null;
        this.synchError = new SynchError(SynchError.Type.UNDEFINED, null);
    }

    public void setID(UniqueIdentifier id) {
        this.fsmID = id;
    }

    @Override
    public State processMessage(State currentState, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (currentState) {
            case WAITING_FOR_REQUEST:
                return processInitialRequest(message, ccp);

            case SYNCHING:
                return sendElementPack(ccp);

            default:
                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unexpected object data at state " + currentState);
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State currentState, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        // ignore, cannot happen
        synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unexpected byte array data");
        return State.ERROR;
    }

    private State processInitialRequest(Object message, ChannelConnectionPoint ccp) {
        try {
            if (!(message instanceof DataSynchClientFSM.SynchRequest)) {
                // unrecognized class
                throw new ClassNotFoundException("");
            }
            DataSynchClientFSM.SynchRequest request = (DataSynchClientFSM.SynchRequest) message;
            dataAccessorName = request.dataAccessorName;
            dataAccessor = dataAccessorContainer.getAccessorForTransmitting(clientPeerID, dataAccessorName);
            ServerSynchRequestAnswer serverSynchRequestAnswer = dataAccessor.initiateListSynchronizationAsServer(clientPeerID);

            if (serverSynchRequestAnswer.type == ServerSynchRequestAnswer.Type.OK) {
                // valid request -> send ok and start synching
                dataSynchEventsBridge.serverSynchRequestAccepted(clientPeerID, dataAccessorName, fsmID);
                dataAccessor.beginSynchProcess(DataAccessor.Mode.SERVER);
                ccp.write(outgoingChannel, SynchRequestAnswer.OK, false);
                progress = serverSynchRequestAnswer.progress;
                if (progress != null) {
                    progress.addNotification(0);
                }
                // send the server database ID
                ccp.write(outgoingChannel, dataAccessor.getDatabaseID(), false);
                String clientDatabaseID = request.databaseID;
                int lastTimestamp = request.lastTimestamp != null ? request.lastTimestamp : -1;
                if (clientDatabaseID == null || !clientDatabaseID.equals(dataAccessor.getDatabaseID())) {
                    // the whole list is required, as database IDs do not match
                    elementsToSend = dataAccessor.getElementsFrom(0);
                } else {
                    // pass the timestamp given by the client, as not the whole list is required
                    elementsToSend = dataAccessor.getElementsFrom(lastTimestamp + 1);
                }
                elementToSendIndex = 0;
                elementsPerMessage = Math.max(dataAccessor.elementsPerMessage(), 1);
                CRCBytes = Math.max(dataAccessor.CRCBytes(), 0);
                return sendElementPack(ccp);
            } else {
                synchError = new SynchError(SynchError.Type.SERVER_BUSY, null);
                ccp.write(outgoingChannel, SynchRequestAnswer.SERVER_BUSY);
                return State.DENIED;
            }
        } catch (ClassNotFoundException e) {
            // invalid class found, error
            ErrorLog.reportError(PeerClient.ERROR_LOG, e.toString(), clientPeerID, dataAccessorName, fsmID);
            synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Invalid request format");
            ccp.write(outgoingChannel, SynchRequestAnswer.INVALID_REQUEST_FORMAT);
            return State.DENIED;
        } catch (UnavailablePeerException e) {
            synchError = new SynchError(SynchError.Type.SERVER_BUSY, null);
            ccp.write(outgoingChannel, SynchRequestAnswer.SERVER_BUSY);
            return State.DENIED;
        } catch (AccessorNotFoundException e) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, e.toString(), clientPeerID, dataAccessorName, fsmID);
            synchError = new SynchError(SynchError.Type.UNKNOWN_ACCESSOR, "Accessor: " + dataAccessorName);
            ccp.write(outgoingChannel, SynchRequestAnswer.UNKNOWN_DATA_ACCESSOR);
            return State.DENIED;
        } catch (DataAccessException e) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Data access error in server synch FSM", clientPeerID, dataAccessorName, fsmID);
            synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, null);
            ccp.write(outgoingChannel, ElementPacket.generateError());
            return State.DENIED;
        }
    }

    public State sendElementPack(ChannelConnectionPoint ccp) throws IllegalArgumentException {
        int packetSize = Math.min(elementsToSend.size() - elementToSendIndex, elementsPerMessage);
        List<Serializable> packet = new ArrayList<>();
        for (int index = elementToSendIndex; index < elementToSendIndex + packetSize; index++) {
            packet.add(elementsToSend.get(index));
        }
        elementToSendIndex += packetSize;
        byte[] bytePacket = new byte[0];
        try {
            bytePacket = Serializer.serializeObject(new ElementPacket(packet, elementToSendIndex, elementsToSend.size()));
        } catch (IOException e) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Cannot serialize ElementPacket", fsmID, clientPeerID, dataAccessorName, elementsToSend, elementToSendIndex);
        }
        bytePacket = CRC.addCRC(bytePacket, CRCBytes, true);
        ccp.write(outgoingChannel, bytePacket);
        if (progress != null) {
            progress.addNotification(NumericUtil.displaceInRange(elementToSendIndex, 0, elementsToSend.size(), 0, DataSynchronizer.PROGRESS_MAX));
        }
        if (elementToSendIndex < elementsToSend.size()) {
            // not finished yet
            return State.SYNCHING;
        } else {
            return State.SUCCESS;
        }
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        // initially we wait for the original request from the client peer
        return State.WAITING_FOR_REQUEST;
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS:
                dataSynchEventsBridge.serverSynchSuccess(clientPeerID, dataAccessorName, fsmID);
                dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
                dataSynchEventsBridge.serverSynchError(clientPeerID, dataAccessorName, fsmID, synchError);
                if (dataAccessor != null) {
                    dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, false);
                }
                if (progress != null) {
                    progress.error(synchError);
                }
                return true;

            case DENIED:
                dataSynchEventsBridge.serverSynchRequestDenied(clientPeerID, dataAccessorName, fsmID, synchError);
                if (dataAccessor != null) {
                    dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, false);
                }
                if (progress != null) {
                    progress.error(synchError);
                }
                return true;
        }
        return false;
    }

    @Override
    public void disconnected(ChannelConnectionPoint ccp) {
        // we got disconnected from the other peer before the synch finished -> notify as an error
        if (dataAccessor != null) {
            dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, false);
        }
        if (progress != null) {
            progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        dataSynchEventsBridge.serverSynchTimeout(clientPeerID, dataAccessorName, fsmID);
        if (dataAccessor != null) {
            dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, false);
        }
        if (progress != null) {
            progress.timeout();
        }
    }

    @Override
    public void setOutgoingChannel(byte channel) {
        outgoingChannel = channel;
    }

    @Override
    public void errorRequestingFSM(PeerFSMServerResponse serverResponse) {
        // ignore
    }
}
