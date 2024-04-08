package servent.handler.snapshot;

import app.snapshot_bitcake.AVBitCakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;

public class AVDoneHandler implements MessageHandler {

    private Message clientMessage;
    private AVBitCakeManager bitCakeManager;
    private SnapshotCollector snapshotCollector;

    public AVDoneHandler(Message clientMessage, SnapshotCollector snapshotCollector){
        this.clientMessage = clientMessage;
        this.bitCakeManager = (AVBitCakeManager)snapshotCollector.getBitcakeManager();
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        bitCakeManager.handleDone(clientMessage, snapshotCollector, clientMessage.getOriginalSenderInfo().getId());
    }
}
