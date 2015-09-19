package jacz.peerengineservice.util.data_synchronization;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.UnavailablePeerException;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.peerengineservice.util.ConnectionStatus;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

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
    }

    enum SynchRequestAnswer {
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

    private static final int MAX_ELEMENT_PACKET_SIZE = 10;

    private byte outgoingChannel;

    private ConnectionStatus requestingPeerStatus;

    private final DataAccessorContainer dataAccessorContainer;

    private DataAccessor dataAccessor;

    private List<? extends Serializable> elementsToSend;

    private int elementToSendIndex;

    /**
     * Progress notifier for the server side. It is obtained form the list accessor (null if not used)
     */
    ProgressNotificationWithError<Integer, SynchError> progress;

    private SynchError synchError;

    /**
     * Class constructor
     *
     * @param requestingPeerStatus status of the requesting peer
     */
    public DataSynchServerFSM(ConnectionStatus requestingPeerStatus, DataAccessorContainer dataAccessorContainer) {
        this.requestingPeerStatus = requestingPeerStatus;
        this.dataAccessorContainer = dataAccessorContainer;
        this.synchError = new SynchError(SynchError.Type.UNDEFINED, null);
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
        if (!requestingPeerStatus.isFriend()) {
            ccp.write(outgoingChannel, SynchRequestAnswer.REQUEST_DENIED);
            return State.ERROR;
        }
        try {
            if (!(message instanceof DataSynchClientFSM.SynchRequest)) {
                // unrecognized class
                throw new ClassNotFoundException("");
            }
            DataSynchClientFSM.SynchRequest request = (DataSynchClientFSM.SynchRequest) message;
            dataAccessor = dataAccessorContainer.getAccessorForTransmitting(request.clientPeerID, request.dataAccessorName);
            ServerSynchRequestAnswer serverSynchRequestAnswer = dataAccessor.initiateListSynchronizationAsServer(request.clientPeerID);

            if (serverSynchRequestAnswer.type == ServerSynchRequestAnswer.Type.OK) {
                // valid request -> send ok and start synching
                dataAccessor.beginSynchProcess(DataAccessor.Mode.SERVER);
                ccp.write(outgoingChannel, SynchRequestAnswer.OK);
                progress = serverSynchRequestAnswer.progress;
                if (progress != null) {
                    progress.addNotification(0);
                }
                elementsToSend = dataAccessor.getElements(request.lastTimestamp);
                elementToSendIndex = 0;
                return sendElementPack(ccp);
            } else {
                ccp.write(outgoingChannel, SynchRequestAnswer.SERVER_BUSY);
                return State.ERROR;
            }
        } catch (ClassNotFoundException e) {
            // invalid class found, error
            ccp.write(outgoingChannel, SynchRequestAnswer.INVALID_REQUEST_FORMAT);
            return State.ERROR;
        } catch (UnavailablePeerException e) {
            ccp.write(outgoingChannel, SynchRequestAnswer.SERVER_BUSY);
            return State.ERROR;
        } catch (AccessorNotFoundException e) {
            ccp.write(outgoingChannel, SynchRequestAnswer.UNKNOWN_DATA_ACCESSOR);
            return State.ERROR;
        } catch (DataAccessException e) {
            ccp.write(outgoingChannel, ElementPacket.generateError());
            return State.ERROR;
        }
    }

    public State sendElementPack(ChannelConnectionPoint ccp) throws IllegalArgumentException {
        int packetSize = Math.min(elementsToSend.size() - elementToSendIndex, MAX_ELEMENT_PACKET_SIZE);
        List<Serializable> packet = new ArrayList<>();
        for (int index = elementToSendIndex; index < elementToSendIndex + packetSize; index++) {
            packet.add(elementsToSend.get(index));
        }
        elementToSendIndex += packetSize;
        ccp.write(outgoingChannel, new ElementPacket(packet, elementToSendIndex, elementsToSend.size()));
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
                dataAccessor.endSynchProcess(DataAccessor.Mode.SERVER, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
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
