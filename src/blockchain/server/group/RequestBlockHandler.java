package blockchain.server.group;

import org.jgroups.JChannel;
import org.jgroups.Message;

import com.google.gson.Gson;

import blockchain.server.model.Block;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;

public class RequestBlockHandler extends Thread {
	private SupplyChainView view;
	private SupplyChainMessage message;
	private JChannel channel;
	private String serverName;
	private Gson gson = new Gson();
	
	public RequestBlockHandler(SupplyChainView view, SupplyChainMessage message, JChannel channel, String serverName) {
		this.view = view;
		this.message = message;
		this.channel = channel;
		this.serverName = serverName;
	}
	
    public void run() {
    	Block block = view.getFromBlockChainAndWaitinqQueue(Integer.valueOf(message.getArgs()));
    	SupplyChainMessage resopnse = new SupplyChainMessage(MessageType.RESPONSE_BLOCK);
    	String log = new String();
		resopnse.setTargetName(message.getSendersName());
		resopnse.setSendersName(serverName);
		resopnse.setArgs(message.getArgs());
    	if (block == null) {
    		resopnse.setBlock(null);
    		log = "do not have block number " + Integer.valueOf(message.getArgs());
    	} else {
    		resopnse.setBlock(block);
    		log = "with block number " + Integer.valueOf(message.getArgs());
    	}    	
    	try {
			synchronized (channel) {
				
				System.out.println("Log :: send RESPONSE_BLOCK message to " + resopnse.getTargetName() + log);			
				channel.send(new Message(null, gson.toJson(resopnse)));
			}
		} catch (Exception e) {			
			System.out.println("Log :: failed to send RESPONSE_BLOCK message. error: " + e.getMessage());
		}
    }
}

