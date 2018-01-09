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
    	Block block = view.getFromBlockChain(Integer.valueOf(message.getArgs()));
		SupplyChainMessage resopnse = new SupplyChainMessage(MessageType.RESPONSE_BLOCK);
		resopnse.setTargetName(message.getSendersName());
		resopnse.setSendersName(serverName);
		resopnse.setArgs(message.getArgs());
		
    	if (block == null) {
    		resopnse.setBlock(null);
    	} else {
    		resopnse.setBlock(block);
    	}
    	
    	try {
			synchronized (channel) {
				channel.send(new Message(null, gson.toJson(resopnse)));
			}
		} catch (Exception e) {
			System.out.println("RequestBlockHandler: failed to send message. error: " + e.getMessage());
		}
    }
}

