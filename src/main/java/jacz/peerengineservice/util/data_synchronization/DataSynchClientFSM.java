package jacz.peerengineservice.util.data_synchronization;

import jacz.commengine.channel.ChannelConnectionPoint;
import jacz.peerengineservice.PeerID;
import jacz.peerengineservice.client.PeerFSMServerResponse;
import jacz.peerengineservice.client.PeerTimedFSMAction;
import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.hash.CRC;
import jacz.util.hash.InvalidCRCException;
import jacz.util.io.object_serialization.MutableOffset;
import jacz.util.io.object_serialization.Serializer;
import jacz.util.notification.ProgressNotificationWithError;
import jacz.util.numeric.NumericUtil;

import java.io.Serializable;

/**
 * Created by Alberto on 17/09/2015.
 */
public class DataSynchClientFSM implements PeerTimedFSMAction<DataSynchClientFSM.State> {

    enum State {
        // Initial state: the initial request to the other peer has been sent
        // We expect the result of this request (accept or deny)
        WAITING_FOR_REQUEST_ANSWER,

        // We are waiting for the server to send us more packages
        SYNCHING,

        // successfully synched all data
        // FINAL STATE
        SUCCESS,

        // error during the communication
        // FINAL STATE
        ERROR,

        // if the request answer was OK, we start the index and hash synch process, so the server can calculate what elements we need
        // we are expecting hash queries from the server to determine which hashes the server must send us. We reply according to the
        // hashes that we have, and wait for the server to say that he is done with this process
        // we leave this state when the server says we are done. We will then check if we must request elements, or we are done
        // TYPE: BYTE_ARRAY
//        INDEX_AND_HASH_SYNCH_PROCESS,

        // the index and hash synch process just finished
        // waiting for server to send the number of hashes that we need. After we get it, we will wait for the hashes themselves
        // TYPE: BYTE_ARRAY
//        WAITING_FOR_INDEX_AND_HASHES_SIZE_TO_REQUEST,

        // the index and hash synch process just finished
        // waiting for server to send the list of hashes that we need. With these data, we initialize the elementTransferSynchClient
        // with it, and execute its initiateDataTransferProcess, and start requesting elements
        // TYPE: BYTE_ARRAY
//        WAITING_FOR_INDEX_AND_HASHES_TO_REQUEST,

        // the requested elements are objects
        // waiting for server to send the last requested element by the elementTransferSynchClient
        // received data is passed to the elementTransferSynchClient
        // TYPE: OBJECT
//        DATA_TRANSMISSION_PROCESS_OBJECT,

        // the requested elements are byte arrays
        // the server must send us the name of the resource store for requesting the data
        // TYPE: BYTE_ARRAY
//        WAITING_FOR_BYTE_ARRAY_RESOURCE_STORE_NAME,

        // process complete (final state). In addition, notify the completion to the progress element
//        SUCCESS_NOTIFY_COMPLETE,

        // process complete (final state). Do not notify the completion of the progress
//        SUCCESS_NOT_NOTIFY_COMPLETE,

        // error due to element modified in server during synchronization
//        ERROR_ELEMENT_CHANGED_IN_SERVER
    }

    /**
     * Request sent from the client FSM to the server FSM to initiate the synch process
     */
    static class SynchRequest implements Serializable {

        final PeerID clientPeerID;

        final String dataAccessorName;

        final Integer lastTimestamp;

        SynchRequest(PeerID clientPeerID, String dataAccessorName, Integer lastTimestamp) {
            this.clientPeerID = clientPeerID;
            this.dataAccessorName = dataAccessorName;
            this.lastTimestamp = lastTimestamp;
        }
    }


    private byte outgoingChannel;

    private final DataAccessor dataAccessor;

    private final String dataAccessorName;

    private final PeerID ownPeerID;

    private final ProgressNotificationWithError<Integer, SynchError> progress;

    private SynchError synchError;


    public DataSynchClientFSM(DataAccessor dataAccessor, String dataAccessorName, PeerID ownPeerID, ProgressNotificationWithError<Integer, SynchError> progress) {
        this.dataAccessor = dataAccessor;
        this.dataAccessorName = dataAccessorName;
        this.ownPeerID = ownPeerID;
        this.progress = progress;
        this.synchError = new SynchError(SynchError.Type.UNDEFINED, null);
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
                                return State.ERROR;
                            case UNKNOWN_DATA_ACCESSOR:
                                synchError = new SynchError(SynchError.Type.UNKNOWN_ACCESSOR, "Server does not recognize accessor");
                                return State.ERROR;
                            case REQUEST_DENIED:
                                synchError = new SynchError(SynchError.Type.REQUEST_DENIED, null);
                                return State.ERROR;
                            case SERVER_BUSY:
                                synchError = new SynchError(SynchError.Type.SERVER_BUSY, null);
                                return State.ERROR;
                            default:
                                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unrecognized server request answer");
                                return State.ERROR;
                        }
                    }
                    dataAccessor.beginSynchProcess(DataAccessor.Mode.CLIENT);
                    if (progress != null) {
                        progress.addNotification(0);
                    }
                    return State.SYNCHING;

                } catch (ClassNotFoundException e) {
                    // invalid class found, error
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                }

//            case SYNCHING:
//                try {
//                    if (!(message instanceof DataSynchServerFSM.ElementPacket)) {
//                        // unrecognized class
//                        throw new ClassNotFoundException("");
//                    }
//                    DataSynchServerFSM.ElementPacket elementPacket = (DataSynchServerFSM.ElementPacket) message;
//                    if (elementPacket.SERVER_ERROR) {
//                        // there was an error in the server
//                        synchError = new SynchError(SynchError.Type.SERVER_ERROR, null);
//                        return State.ERROR;
//                    }
//                    for (Serializable element : elementPacket.elementPacket) {
//                        dataAccessor.setElement(element);
//                    }
//                    if (progress != null) {
//                        progress.addNotification(NumericUtil.displaceInRange(elementPacket.elementsSent, 0, elementPacket.totalElementsToSend, 0, DataSynchronizer.PROGRESS_MAX));
//                    }
//                    if (elementPacket.elementsSent < elementPacket.totalElementsToSend) {
//                        // ask for more elements
//                        ccp.write(outgoingChannel, true);
//                        return State.SYNCHING;
//                    } else {
//                        return State.SUCCESS;
//                    }
//                } catch (ClassNotFoundException e) {
//                    // invalid class found, error
//                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
//                    return State.ERROR;
//                } catch (DataAccessException e) {
//                    synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, "Error adding element to data accessor");
//                    return State.ERROR;
//                }

            default:
                synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Unexpected object data at state " + currentState);
                return State.ERROR;
        }
    }

    @Override
    public State processMessage(State currentState, byte channel, byte[] data, ChannelConnectionPoint ccp) throws IllegalArgumentException {
        switch (currentState) {
            case SYNCHING:
                try {
                    data = CRC.extractDataWithCRC(data);
                    Object message = Serializer.deserializeObject(data, new MutableOffset());
                    if (!(message instanceof DataSynchServerFSM.ElementPacket)) {
                        // unrecognized class
                        throw new ClassNotFoundException("");
                    }
                    DataSynchServerFSM.ElementPacket elementPacket = (DataSynchServerFSM.ElementPacket) message;
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
                    synchError = new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, "Received request object not recognized in state: " + currentState);
                    return State.ERROR;
                } catch (DataAccessException e) {
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
            ccp.write(outgoingChannel, new SynchRequest(ownPeerID, dataAccessorName, dataAccessor.getLastTimestamp()));
            return State.WAITING_FOR_REQUEST_ANSWER;
        } catch (DataAccessException e) {
            synchError = new SynchError(SynchError.Type.DATA_ACCESS_ERROR, "Could not access last timestamp");
            return State.ERROR;
        }
    }

    @Override
    public boolean isFinalState(State state, ChannelConnectionPoint ccp) {
        switch (state) {
            case SUCCESS:
                dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, true);
                if (progress != null) {
                    progress.completeTask();
                }
                return true;

            case ERROR:
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
        dataAccessor.endSynchProcess(DataAccessor.Mode.CLIENT, false);
        if (progress != null) {
            progress.error(new SynchError(SynchError.Type.DISCONNECTED, null));
        }
    }

    @Override
    public void timedOut(State state) {
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
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                if (progress != null) {
                    progress.error(new SynchError(SynchError.Type.ERROR_IN_PROTOCOL, serverResponse.toString()));
                }
            }
        });
    }


}
