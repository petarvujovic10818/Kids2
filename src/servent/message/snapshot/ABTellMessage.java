package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.ABSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.util.List;
import java.util.Map;

public class ABTellMessage extends BasicMessage {

    private ABSnapshotResult abSnapshotResult;
    private Map<Integer, Integer> senderVectorClock;

    public ABTellMessage(ServentInfo sender, ServentInfo receiver, ABSnapshotResult abSnapshotResult, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AB_TELL, sender, receiver);

        this.abSnapshotResult = abSnapshotResult;
        this.senderVectorClock = senderVectorClock;
    }

    private ABTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
                          boolean white, List<ServentInfo> routeList, String messageText, int messageId,
                          ABSnapshotResult abSnapshotResult) {
        super(messageType, sender, receiver, white, routeList, messageText, messageId);
        this.abSnapshotResult = abSnapshotResult;
    }

    public ABSnapshotResult getABSnapshotResult() {
        return abSnapshotResult;
    }

    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }

    @Override
    public Message setRedColor() {
        Message toReturn = new ABTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
                false, getRoute(), getMessageText(), getMessageId(), getABSnapshotResult());
        return toReturn;
    }

}
