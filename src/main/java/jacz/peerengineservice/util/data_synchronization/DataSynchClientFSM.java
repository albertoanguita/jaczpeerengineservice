package jacz.peerengineservice.util.data_synchronization;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerClient;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.hash.CRC;
import jacz.util.hash.InvalidCRCException;
import jacz.util.identifier.UniqueIdentifier;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.log.ErrorLog;
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

    private UniqueIdentifier fsmID;

    private byte outgoingChannel;

    private final DataSynchEventsBridge dataSynchEventsBridge;

    private final DataAccessor dataAccessor;

    private final String dataAccessorName;

    private final PeerID serverPeerID;

    private final ProgressNotificationWithError<Integer, SynchError> progress;

    private SynchError synchError;


    public DataSynchClientFSM(DataSynchEventsBridge dataSynchEventsBridge, DataAccessor dataAccessor, String dataAccessorName, PeerID serverPeerID, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.dataSynchEventsBridge = dataSynchEventsBridge;
        this.dataAccessor = dataAccessor;
        this.dataAccessorName = dataAccessorName;
        this.serverPeerID = serverPeerID;
        this.progress = progress;
        this.synchError = new SynchError(SynchError.Type.UNDEFINED, null);
    }

    public void setID(UniqueIdentifier id) {
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
                    ErrorLog.reportError(PeerClient.ERROR_LOG, e.toString(), serverPeerID, dataAccessorName, fsmID);
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                }

            case WAITING_FOR_DATABASE_ID:
                try {
                    if (!(message instanceof String)) {
                        // unrecognized class
                        throw new ClassNotFoundException("");
                    }
                    String databaseID = (String) message;
                    dataAccessor.setDatabaseID(databaseID);
                    dataAccessor.beginSynchProcess(DataAccessor.Mode.CLIENT);
                    if (progress != null) {
                        progress.addNotification(0);
                    }
                    return State.SYNCHING;

                } catch (ClassNotFoundException e) {
                    // invalid class found, error
                    ErrorLog.reportError(PeerClient.ERROR_LOG, e.toString(), serverPeerID, dataAccessorName, fsmID);
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
                        progress.addNotification(NumericUtil.displaceInRange(elementPacket.elementsSent, 0, elementPacket.totalElementsToSend, 0, DataSynchronizer.PROGRESS_MAX));
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
                    ErrorLog.reportError(PeerClient.ERROR_LOG, e.toString(), serverPeerID, dataAccessorName, fsmID);
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                } catch (DataAccessException e) {
                    ErrorLog.reportError(PeerClient.ERROR_LOG, "Data access error in client synch FSM, setting element", serverPeerID, dataAccessorName, fsmID, elementPacket);
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
            ccp.write(outgoingChannel, new SynchRequest(dataAccessorName, dataAccessor.getDatabaseID(), dataAccessor.getLastTimestamp()));
            return State.WAITING_FOR_REQUEST_ANSWER;
        } catch (DataAccessException e) {
            ErrorLog.reportError(PeerClient.ERROR_LOG, "Data access error in client synch FSM, getting last timestamp", serverPeerID, dataAccessorName, fsmID);
            synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, "Could not access last timestamp");
            return State.ERROR;
        }
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS:
                dataSynchEventsBridge.clientSynchSuccess(serverPeerID, dataAccessorName, fsmID);
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
                dataSynchEventsBridge.clientSynchError(serverPeerID, dataAccessorName, fsmID, synchError);
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
                if (progress != null) {
                    progress.error(synchError);
                }
                return true;

            case REQUEST_DENIED:
                dataSynchEventsBridge.clientSynchRequestDenied(serverPeerID, dataAccessorName, fsmID, synchError);
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
        dataSynchEventsBridge.clientSynchError(serverPeerID, dataAccessorName, fsmID, new SynchError(SynchError.Type.DISCONNECTED, null));
        dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
        dataSynchEventsBridge.clientSynchTimeout(serverPeerID, dataAccessorName, fsmID);
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
        dataSynchEventsBridge.clientSynchError(serverPeerID, dataAccessorName, fsmID, new SynchError(SynchError.Type.REQUEST_DENIED, serverResponse.toString()));
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                if (progress != null) {
                    progress.error(new SynchError(SynchError.Type.REQUEST_DENIED, serverResponse.toString()));
                }
            }
        });
    }
}
