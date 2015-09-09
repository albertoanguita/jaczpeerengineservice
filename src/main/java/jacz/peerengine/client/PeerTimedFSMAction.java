package jacz.peerengine.client;

/**
 * Created by IntelliJ IDEA.
 * User: Alberto
 * Date: 10-may-2010
 * Time: 15:19:38
 * To change this template use File | Settings | File Templates.
 */
public interface PeerTimedFSMAction<T> extends PeerFSMAction<T> {

    public void timedOut(T state);
}
