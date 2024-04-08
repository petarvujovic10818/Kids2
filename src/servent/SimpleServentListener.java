package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.snapshot_bitcake.ABBitcakeManager;
import app.snapshot_bitcake.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.*;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	private List<Message> redMessages = new ArrayList<>();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;

				if (AppConfig.isWhite.get() == false && redMessages.size() > 0){
					clientMessage = redMessages.remove(0);
				} else {
					/*
					 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
					 */
					Socket clientSocket = listenerSocket.accept();

					//GOT A MESSAGE! <3
					clientMessage = MessageUtil.readMessage(clientSocket);
				}

				synchronized (AppConfig.colorLock) {
					 if (AppConfig.SNAPSHOT_TYPE == SnapshotType.AB) {
						if (clientMessage.isWhite() == false && AppConfig.isWhite.get()) {
							/*
							 * If the message is red, we are white, and the message isn't a marker,
							 * then store it. We will get the marker soon, and then we will process
							 * this message. The point is, we need the marker to know who to send
							 * our info to, so this is the simplest way to work around that.
							 */
							if (clientMessage.getMessageType() != MessageType.AB_MARKER) {
								redMessages.add(clientMessage);
								CausalBroadcastShared.addPendingMessage(clientMessage);
								CausalBroadcastShared.checkPendingMessages();
								continue;
							} else {
								ABBitcakeManager abBitcakeManager =
										(ABBitcakeManager) snapshotCollector.getBitcakeManager();
								abBitcakeManager.markerEvent(
										Integer.parseInt(clientMessage.getMessageText()), snapshotCollector);
							}
						}
					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case AB_MARKER:
					messageHandler = new ABMarkerHandler();
					break;
				case AB_TELL:
					messageHandler = new ABTellHandler(clientMessage, snapshotCollector);
					break;
				case TRANSACTION:
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case AV_MARKER:
					messageHandler = new AVMarkerHandler(clientMessage, snapshotCollector);
					break;
				case AV_DONE:
					messageHandler = new AVDoneHandler(clientMessage, snapshotCollector);
					break;
				case AV_TERMINATE:
					messageHandler = new AVTerminateHandler(clientMessage, snapshotCollector);
					break;
				case POISON:
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
