package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AVSnapshotResult implements Serializable {

    private final int serventId;
    private final int recordedAmount;
    private final Map<String, List<Integer>> allChannelMessages;

    public AVSnapshotResult(int serventId, int recordedAmount, Map<String, List<Integer>> allChannelMessages) {
        this.serventId = serventId;
        this.recordedAmount = recordedAmount;
        this.allChannelMessages = new ConcurrentHashMap<>(allChannelMessages);
    }
    public int getServentId() {
        return serventId;
    }
    public int getRecordedAmount() {
        return recordedAmount;
    }
    public Map<String, List<Integer>> getAllChannelMessages() {
        return allChannelMessages;
    }

}
