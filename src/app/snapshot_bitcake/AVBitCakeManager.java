package app.snapshot_bitcake;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.AVDoneMessage;
import servent.message.snapshot.AVMarkerMessage;
import servent.message.snapshot.AVTerminateMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AVBitCakeManager implements BitcakeManager{

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    //mapa u kojoj pamtimo ko nam je poslao done poruke
    private Map<Integer, Boolean> closedChannels = new ConcurrentHashMap<>();

    private Map<Integer, Boolean> closedTerminateChannels = new ConcurrentHashMap<>();

    private Map<String, List<Integer>> allChannelTransactions = new ConcurrentHashMap<>();

    private Object allChannelTransactionsLock = new Object();
    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }
    public int recordedAmount = 0;
    public void markerEvent(int collectorId, SnapshotCollector snapshotCollector) {

        synchronized (AppConfig.colorLock) {
            AppConfig.timestampedStandardPrint("Going red");
            //dobio sam marker postavljam se na crvenu boju
            AppConfig.isWhite.set(false);
            //snimam trenutno stanje
            recordedAmount = getCurrentBitcakeAmount();

            AVSnapshotResult snapshotResult = new AVSnapshotResult(
                    AppConfig.myServentInfo.getId(), recordedAmount, allChannelTransactions);

            //ako sam inicijator cuvam trenutno stanje i cekam done poruke
            if (collectorId == AppConfig.myServentInfo.getId()) {
                snapshotCollector.addAVSnapshotInfo(
                        AppConfig.myServentInfo.getId(),
                        snapshotResult);
            }
//            else {
//                //sta se dogadja ako sam dobio marker a nisam inicijator
//                //saljem done poruku inicijatoru sa trenutnim stanjem kolacica
//                Message doneMessage = new AVDoneMessage(AppConfig.myServentInfo, AppConfig.getInfoById(collectorId), collectorId, snapshotResult, new HashMap<>());
//                MessageUtil.sendMessage(doneMessage);
//            }

            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                if(collectorId == AppConfig.myServentInfo.getId()){
                    closedChannels.put(neighbor, false);
                }
                Message avMaker = new AVMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, new HashMap<>());
                MessageUtil.sendMessage(avMaker);
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

    public void markerHandler(Message clientMessage, SnapshotCollector snapshotCollector, int collectorId){
        if(AppConfig.isWhite.get()){
            AppConfig.timestampedStandardPrint("Going red");
            AppConfig.isWhite.set(false);

            recordedAmount = getCurrentBitcakeAmount();

            AVSnapshotResult snapshotResult = new AVSnapshotResult(
                    AppConfig.myServentInfo.getId(), recordedAmount, allChannelTransactions);

            snapshotCollector.addAVSnapshotInfo(
                    AppConfig.myServentInfo.getId(),
                    snapshotResult);

            for(Integer neighbor: AppConfig.myServentInfo.getNeighbors()){
                Message avMaker = new AVMarkerMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, new HashMap<>());
                MessageUtil.sendMessage(avMaker);
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

    public void handleDone(Message clientMessage, SnapshotCollector snapshotCollector, int collectorId){
        synchronized (AppConfig.colorLock){

            if (AppConfig.isWhite.get()) {
                markerEvent(collectorId, snapshotCollector);
            }

            AppConfig.timestampedStandardPrint("Going done");
            AppConfig.isDone.set(true);

            closedChannels.put(clientMessage.getOriginalSenderInfo().getId(), true);

            //ako je done znaci da broadcastujemo terminate poruku i zavrsavamo collecting
            if(isDone()){
                //ako smo mi inicijator cuvamo svoj rezultat sa kanalnim porukama
                AVSnapshotResult snapshotResult = new AVSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount, allChannelTransactions);
                if(AppConfig.myServentInfo.getId() == collectorId){
                    snapshotCollector.addAVSnapshotInfo(collectorId, snapshotResult);

                    for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                        Message terminateMessage = new AVTerminateMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, new HashMap<>());
                        MessageUtil.sendMessage(terminateMessage);
                        try {
                            /**
                             * This sleep is here to artificially produce some white node -> red node messages
                             */
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    recordedAmount = 0;
                    allChannelTransactions.clear();
                    AppConfig.timestampedStandardPrint("Going white");
                    AppConfig.isWhite.set(true);

                }
//                for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
//                    Message terminateMessage = new AVTerminateMessage(AppConfig.myServentInfo, AppConfig.getInfoById(neighbor), collectorId, new HashMap<>());
//                    MessageUtil.sendMessage(terminateMessage);
//                    try {
//                        /**
//                         * This sleep is here to artificially produce some white node -> red node messages
//                         */
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
            }

        }
    }

    public void handleTerminate(Message clientMessage, SnapshotCollector snapshotCollector, int collectorId){
        synchronized (AppConfig.doneLock){

        }
    }

    private boolean isDone() {
        if (AppConfig.isWhite.get()) {
            return false;
        }

        AppConfig.timestampedStandardPrint(closedChannels.toString());

        //prolazim kroz done poruke i cekamo da sve dodju od sudjednih covorova
        for (Map.Entry<Integer, Boolean> closedChannel : closedChannels.entrySet()) {
            if (closedChannel.getValue() == false) {
                return false;
            }
        }

        return true;
    }

    private boolean isTerminated() {
        if (AppConfig.isWhite.get()) {
            return false;
        }

        AppConfig.timestampedStandardPrint(closedTerminateChannels.toString());

        //prolazim kroz done poruke i cekamo da sve dodju od sudjednih covorova
        for (Map.Entry<Integer, Boolean> closedChannel : closedTerminateChannels.entrySet()) {
            if (closedChannel.getValue() == false) {
                return false;
            }
        }

        return true;
    }

    //metoda kojom dodajemo poruke koje su u kanalu (ako su svi vratili done)
    public void addChannelMessage(Message clientMessage) {
        if (clientMessage.getMessageType() == MessageType.TRANSACTION) {
            synchronized (allChannelTransactionsLock) {
                String channelName = "channel " + AppConfig.myServentInfo.getId() + "<-" + clientMessage.getOriginalSenderInfo().getId();

                List<Integer> channelMessages = allChannelTransactions.getOrDefault(channelName, new ArrayList<>());
                channelMessages.add(Integer.parseInt(clientMessage.getMessageText()));
                allChannelTransactions.put(channelName, channelMessages);
            }
        }
    }

}
