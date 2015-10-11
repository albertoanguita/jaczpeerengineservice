package jacz.peerengineservice.util.datatransfer;

/**
 * This class acts as a bypass of the client's provided ResourceTransferEvents implementation, logging all activity
 * <p/>
 * In addition, it takes care of generating a thread for each method call, so invoker does not have to worry about it
 */
public class ResourceTransferEventsBridge implements ResourceTransferEvents {

    // todo
}
