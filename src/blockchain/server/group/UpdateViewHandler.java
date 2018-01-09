package blockchain.server.group;

import org.apache.zookeeper.KeeperException;
import org.jgroups.JChannel;
import org.jgroups.Message;

import com.google.gson.Gson;

import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;
import blockchain.server.zoo.ZooKeeperHandler;

public class UpdateViewHandler extends Thread {
	private SupplyChainView view;
	private SupplyChainMessage message;
	private JChannel channel;
	private String serverName;
	private ZooKeeperHandler zkh;
	private Gson gson = new Gson();
	private static Object waitingPoint = new Object();
	
	public UpdateViewHandler(SupplyChainView view, SupplyChainMessage message, JChannel channel, String serverName, ZooKeeperHandler zkh) {
		this.view = view;
		this.message = message;
		this.channel = channel;
		this.serverName = serverName;
		this.zkh = zkh;
	}
	
    public void run() {
    	
    	// send Ack
		SupplyChainMessage resopnse = new SupplyChainMessage(MessageType.ACK);
		resopnse.setTargetName(message.getSendersName());
		resopnse.setSendersName(serverName);
		resopnse.setArgs(String.valueOf(message.getBlock().getDepth()));
		
    	try {
			synchronized (channel) {
				channel.send(new Message(null, gson.toJson(resopnse)));
			}
		} catch (Exception e) {
			System.out.println("RequestBlockHandler: failed to send message. error: " + e.getMessage());
		}
    	
    	synchronized(waitingPoint) {
    		// update local view.
        	if(message.getBlock().getDepth() > view.getKnownBlocksDepth())
            {
                while (message.getBlock().getDepth() != view.getKnownBlocksDepth() + 1) {
                    try {
                    	waitingPoint.wait();
                    } catch (InterruptedException e) {}
                }
                
                view.getRWLock().acquireWrite();
                boolean isExsit;
                try {
    				isExsit = zkh.checkIfServerExist(message.getSendersName());
    			} catch (KeeperException | InterruptedException e) {
    				isExsit = false;
    			}
                
                if (isExsit) {
                	message.getBlock().applyTransactions(view);
                    view.addToBlockChain(message.getBlock());
                }
                waitingPoint.notifyAll();
                view.getRWLock().releaseWrite();
            }
    	}
    }
}
