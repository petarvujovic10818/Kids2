package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.AVSnapshotResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;

public class AVDoneMessage extends BasicMessage {

    private Map<Integer, Integer> senderVectorClock;
    private AVSnapshotResult avSnapshotResult;

    public AVDoneMessage(ServentInfo sender, ServentInfo receiver, int collectorId, AVSnapshotResult avSnapshotResult, Map<Integer, Integer> senderVectorClock) {
        super(MessageType.AV_DONE, sender, receiver, String.valueOf(collectorId));

        this.senderVectorClock = senderVectorClock;
        this.avSnapshotResult = avSnapshotResult;
    }

    public Map<Integer, Integer> getSenderVectorClock() {
        return senderVectorClock;
    }

    public AVSnapshotResult getAvSnapshotResult() {
        return avSnapshotResult;
    }
}
