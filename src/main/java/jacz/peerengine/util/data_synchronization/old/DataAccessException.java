package jacz.peerengine.util.data_synchronization.old;

/**
 * Exception generated when a list accessor implementation cannot access its underlying data. It will cause the corresponding FSM to finish
 * with an error, without notifying the other end (the other end is expected to die by a timeout eventually)
 */
public class DataAccessException extends Exception {
}
