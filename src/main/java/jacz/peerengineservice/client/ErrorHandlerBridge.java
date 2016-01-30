package jacz.peerengineservice.client;

import jacz.util.concurrency.task_executor.ParallelTask;
import jacz.util.concurrency.task_executor.ParallelTaskExecutor;
import jacz.util.log.ErrorHandler;

/**
 * Bridge fatal for error events. Stops the peer client.
 */
public class ErrorHandlerBridge implements ErrorHandler {

    private final PeerClient peerClient;

    private final ErrorHandler errorHandler;

    public ErrorHandlerBridge(PeerClient peerClient, ErrorHandler errorHandler) {
        this.peerClient = peerClient;
        this.errorHandler = errorHandler;
    }

    @Override
    public void errorRaised(final String errorMessage) {
        ParallelTaskExecutor.executeTask(new ParallelTask() {
            @Override
            public void performTask() {
                peerClient.stop();
                if (errorHandler != null) {
                    errorHandler.errorRaised(errorMessage);
                } else {
                    System.err.println(errorMessage);
                }
            }
        });
    }
}
