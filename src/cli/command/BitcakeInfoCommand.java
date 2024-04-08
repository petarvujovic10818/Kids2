package cli.command;

import app.snapshot_bitcake.CausalBroadcastShared;
import app.snapshot_bitcake.SnapshotCollector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BitcakeInfoCommand implements CLICommand {

	private SnapshotCollector collector;
	
	public BitcakeInfoCommand(SnapshotCollector collector) {
		this.collector = collector;
	}
	
	@Override
	public String commandName() {
		return "bitcake_info";
	}

	@Override
	public void execute(String args) {
//		Map<Integer, Integer> myClock = new ConcurrentHashMap<Integer, Integer>();
//		for (Map.Entry<Integer, Integer> entry : CausalBroadcastShared.getVectorClock().entrySet()) {
//			myClock.put(entry.getKey(), entry.getValue());
//		}


		collector.startCollecting();

	}

}
