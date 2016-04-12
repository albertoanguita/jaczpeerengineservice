package jacz.peerengineservice.util.data_synchronization;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerId;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.util.concurrency.task_executor.ThreadExecutor;
import jacz.util.hash.CRC;
import jacz.util.hash.InvalidCRCException;
import jacz.util.io.serialization.MutableOffset;
import jacz.util.io.serialization.Serializer;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.io.Serializable;

/**
 * Data synch client FSM
 */
public class DataSynchClientFSM implements PeerTimedFSMAction<DataSynchClientFSM.State> {

    enum State {
        // Initial state: the initial request to the other peer has been sent
        // We expect the result of this request (accept or deny)
        WAITING_FOR_REQUEST_ANSWER,

        WAITING_FOR_DATABASE_ID,

        // We are waiting for the server to send us more packages
        SYNCHING,

        // successfully synched all data
        // FINAL STATE
        SUCCESS,

        // error during the communication
        // FINAL STATE
        ERROR,

        // the server denied our synch request
        // FINAL STATE
        REQUEST_DENIED,
    }

    /**
     * Request sent from the client FSM to the server FSM to initiate the synch process
     */
    static class SynchRequest implements Serializable {

        final String dataAccessorName;

        final String databaseID;

        final Long lastTimestamp;

        SynchRequest(String dataAccessorName, String databaseID, Long lastTimestamp) {
            this.dataAccessorName = dataAccessorName;
            this.databaseID = databaseID;
            this.lastTimestamp = lastTimestamp;
        }
    }

    private String fsmID;

    private byte outgoingChannel;

    private final DataAccessor dataAccessor;

    private String dataAccessorDatabaseID;

    private final PeerId serverPeerId;

    private final ProgressNotificationWithError<Integer, SynchError> progress;

    private SynchError synchError;


    public DataSynchClientFSM(DataAccessor dataAccessor, PeerId serverPeerId, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.dataAccessor = dataAccessor;
        this.serverPeerId = serverPeerId;
        this.progress = progress;
        this.synchError = new SynchError(SynchError.Type.UNDEFINED, null);
    }

    public void setID(String id) {
        this.fsmID = id;
    }

    @Override
    public State processMessage(State currentState, byte channel, Object message, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (currentState) {
            case WAITING_FOR_REQUEST_ANSWER:
                try {
                    if (!(message instanceof DataSynchServerFSM.SynchRequestAnswer)) {
                        // unrecognized class
                        throw new ClassNotFoundException("");
                    }
                    DataSynchServerFSM.SynchRequestAnswer synchRequestAnswer = (DataSynchServerFSM.SynchRequestAnswer) message;
                    if (synchRequestAnswer != DataSynchServerFSM.SynchRequestAnswer.OK) {
                        switch (synchRequestAnswer) {

                            case INVALID_REQUEST_FORMAT:
                                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "There was an error in the request format");
                                return State.REQUEST_DENIED;
                            case UNKNOWN_DATA_ACCESSOR:
                                synchError = new SynchError(SynchError.Type.UNKNOWN_ACCESSOR, "Server does not recognize accessor");
                                return State.REQUEST_DENIED;
                            case REQUEST_DENIED:
                                synchError = new SynchError(SynchError.Type.REQUEST_DENIED, null);
                                return State.REQUEST_DENIED;
                            case SERVER_BUSY:
                                synchError = new SynchError(SynchError.Type.SERVER_BUSY, null);
                                return State.REQUEST_DENIED;
                            default:
                                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unrecognized server request answer");
                                return State.REQUEST_DENIED;
                        }
                    }
                    return State.WAITING_FOR_DATABASE_ID;

                } catch (ClassNotFoundException e) {
                    // invalid class found, error
                    PeerClient.reportError(e.toString(), serverPeerId, dataAccessor.getName(), fsmID);
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                }

            case WAITING_FOR_DATABASE_ID:
                try {
                    if (!(message instanceof String) && message != null) {
                        // unrecognized class
                        throw new ClassNotFoundException("");
                    }
                    String receivedDatabaseID = message != null ? (String) message : null;
                    if ((dataAccessorDatabaseID == null && receivedDatabaseID != null) ||
                            (dataAccessorDatabaseID != null && !dataAccessorDatabaseID.equals(receivedDatabaseID))) {
                        dataAccessor.setDatabaseID(receivedDatabaseID);
                    }
                    dataAccessor.beginSynchProcess(DataAccessor.Mode.CLIENT);
//                    if (progress != null) {
//                        progress.addNotification(0);
//                    }
                    return State.SYNCHING;

                } catch (ClassNotFoundException e) {
                    // invalid class found, error
                    PeerClient.reportError(e.toString(), serverPeerId, dataAccessor.getName(), fsmID);
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                }

            default:
                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unexpected object data at state " + currentState);
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State currentState, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (currentState) {
            case SYNCHING:
                DataSynchServerFSM.ElementPacket elementPacket = null;
                try {
                    data = CRC.extractDataWithCRC(data);
                    Object message = Serializer.deserializeObject(data, new MutableOffset());
                    if (!(message instanceof DataSynchServerFSM.ElementPacket)) {
                        // unrecognized class
                        throw new ClassNotFoundException("");
                    }
                    elementPacket = (DataSynchServerFSM.ElementPacket) message;
                    if (elementPacket.SERVER_ERROR) {
                        // there was an error in the server
                        synchError = new SynchError(SynchError.Type.SERVER_ERROR, null);
                        return State.ERROR;
                    }
                    for (Serializable element : elementPacket.elementPacket) {
                        dataAccessor.setElement(element);
                    }
                    if (progress != null) {
                        progress.addNotification(NumericUtil.displaceInRange(elementPacket.elementsSent, 0, elementPacket.totalElementsToSend, 0, DataSynchronizer.PROGRESS_MAX, NumericUtil.AmbiguityBehavior.MAX));
                    }
                    if (elementPacket.elementsSent < elementPacket.totalElementsToSend) {
                        // ask for more elements
                        ccp.write(outgoingChannel, true);
                        return State.SYNCHING;
                    } else {
                        return State.SUCCESS;
                    }
                } catch (ClassNotFoundException e) {
                    // invalid class found, error
                    PeerClient.reportError(e.toString(), serverPeerId, dataAccessor.getName(), fsmID);
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                } catch (DataAccessException e) {
                    PeerClient.reportError("Data access error in client synch FSM, setting element", serverPeerId, dataAccessor.getName(), fsmID, elementPacket);
                    synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, "Error adding element to data accessor");
                    return State.ERROR;
                } catch (InvalidCRCException e) {
                    synchError = new SynchError(SynchError.Type.TRANSMISSION_ERROR, "CRC check failed");
                    return State.ERROR;
                }

            default:
                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unexpected byte array data at state " + currentState);
                return State.ERROR;
        }
    }

    @Override
    public State init(ChannelConnectionPoint ccp) {
        try {
            if (progress != null) {
                progress.beginTask();
            }
            dataAccessorDatabaseID = dataAccessor.getDatabaseID();
            ccp.write(outgoingChannel, new SynchRequest(dataAccessor.getName(), dataAccessorDatabaseID, dataAccessor.getLastTimestamp()));
            return State.WAITING_FOR_REQUEST_ANSWER;
        } catch (DataAccessException e) {
            PeerClient.reportError("Data access error in client synch FSM, getting last timestamp", serverPeerId, dataAccessor.getName(), fsmID);
            synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, "Could not access last timestamp");
            return State.ERROR;
        }
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS:
                DataSynchronizer.logger.info("CLIENT SYNCH SUCCESS. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID);
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
                DataSynchronizer.logger.info("CLIENT SYNCH ERROR. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID + ". synchError: " + synchError);
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
                if (progress != null) {
                    progress.error(synchError);
                }
                return true;

            case REQUEST_DENIED:
                DataSynchronizer.logger.info("CLIENT SYNCH REQUEST DENIED. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID + ". synchError: " + synchError);
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
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
        DataSynchronizer.logger.info("CLIENT SYNCH ERROR. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID + ". synchError: " + new SynchError(SynchError.Type.DISCONNECTED, null));
        dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        DataSynchronizer.logger.info("CLIENT SYNCH TIMEOUT. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID);
        dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.timeout();
        }
    }

    @Override
    public void setOutgoingChannel(byte channel) {
        outgoingChannel = channel;
    }

    @Override
    public void errorRequestingFSM(final PeerFSMServerResponse serverResponse) {
        DataSynchronizer.logger.info("CLIENT SYNCH ERROR. serverPeer: " + serverPeerId + ". dataAccessorName: " + dataAccessor.getName() + ". fsmID: " + fsmID + ". synchError: " + new SynchError(SynchError.Type.REQUEST_DENIED, serverResponse.toString()));
        // we register at the thread executor just for submitting this task. We unregister immediately after
        ThreadExecutor.registerClient(this.getClass().getName());
        ThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (progress != null) {
                    progress.error(new SynchError(SynchError.Type.REQUEST_DENIED, serverResponse.toString()));
                }
            }
        });
        ThreadExecutor.shutdownClient(this.getClass().getName());
    }
}
