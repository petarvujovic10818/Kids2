package app.snapshot_bitcake;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import servent.message.Message;
import servent.message.snapshot.ABMarkerMessage;
import servent.message.snapshot.ABTellMessage;
import servent.message.util.MessageUtil;

public class ABBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    public ABBitcakeManager() {
        for(Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    /*
     * This value is protected by AppConfig.colorLock.
     * Access it only if you have the blessing.
     */
    public int recordedAmount = 0;

    public void markerEvent(int collectorId, SnapshotCollector snapshotCollector) {
        synchronized (AppConfig.colorLock) {
            AppConfig.isWhite.set(false);
            recordedAmount = getCurrentBitcakeAmount();

            ABSnapshotResult snapshotResult = new ABSnapshotResult(
                    AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory);
            Map<Integer, Integer> myClock = new ConcurrentHashMap<Integer, Integer>();
            if (collectorId == AppConfig.myServentInfo.getId()) {
                snapshotCollector.addABSnapshotInfo(
                        AppConfig.myServentInfo.getId(),
                        snapshotResult);
            } else {
//                Map<Integer, Integer> myClock = new ConcurrentHashMap<Integer, Integer>();
                for (Map.Entry<Integer, Integer> entry : CausalBroadcastShared.getVectorClock().entrySet()) {
                    myClock.put(entry.getKey(), entry.getValue());
                }

                Message tellMessage = new ABTellMessage(
                        AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), snapshotResult, myClock);

                CausalBroadcastShared.commitCausalMessage(tellMessage);

                MessageUtil.sendMessage(tellMessage);
            }

            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                Message abMarker = new ABMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, myClock);
                MessageUtil.sendMessage(abMarker);
                try {
                    /**
                     * This sleep is here to artificially produce some white node -> red node messages
                     */
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {

        private int valueToAdd;

        public MapValueUpdater(int valueToAdd) {
            this.valueToAdd = valueToAdd;
        }

        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }

    public void recordGiveTransaction(int neighbor, int amount) {
        giveHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    public void recordGetTransaction(int neighbor, int amount) {
        getHistory.compute(neighbor, new MapValueUpdater(amount));
    }
}
