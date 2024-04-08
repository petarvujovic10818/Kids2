package servent.message.snapshot;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class AVTerminateMessage extends BasicMessage {

    private Map<Integer, Integer> senderVectorClock;
    public AVTerminateMessage(ServentInfo sender, ServentInfo receiver, int collectorId, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_MARKER, sender, receiver, String.valueOf(collectorId));

        this.senderVectorClock = senderVectorClock;
    }

    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }

}
