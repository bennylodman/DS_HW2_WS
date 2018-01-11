package blockchain.server.group;

import org.apache.zookeeper.KeeperException;
import org.jgroups.JChannel;
import org.jgroups.Message;

import com.google.gson.Gson;

import blockchain.server.DsTechShipping;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;
import blockchain.server.zoo.ZooKeeperHandler;

public class UpdateViewHandler extends Thread {
	private SupplyChainView view;
	private SupplyChainMessage message;
	private JChannel channel;
	private String serverName;
	private ZooKeeperHandler zkh;
	private boolean sendAck;
	
	private Gson gson = new Gson();
	private static Object waitingPoint = new Object();
	
	public UpdateViewHandler(SupplyChainView view, SupplyChainMessage message, JChannel channel, String serverName, ZooKeeperHandler zkh, boolean sendAck) {
		this.view = view;
		this.message = message;
		this.channel = channel;
		this.serverName = serverName;
		this.zkh = zkh;
		this.sendAck = sendAck;
	}
	
    public void run() {
    	if (!view.addToWaitingBlocks(message.getBlock()))
    		return;
    		
    	// send Ack
    	if (sendAck) {
    		SupplyChainMessage resopnse = new SupplyChainMessage(MessageType.ACK);
    		resopnse.setTargetName(message.getSendersName());
    		resopnse.setSendersName(serverName);
    		resopnse.setArgs(String.valueOf(message.getBlock().getDepth()));
    		
        	try {
    			synchronized (channel) {
    				System.out.println("@@@ Send ACK");
    				channel.send(new Message(null, gson.toJson(resopnse)));
    			}
    		} catch (Exception e) {
    			System.out.println("RequestBlockHandler: failed to send message. error: " + e.getMessage());
    		}
    	}

    	
    	synchronized(waitingPoint) {
    		// update local view.
    		System.out.println("@@@UpdateViewHandler - sync");
        	if(message.getBlock().getDepth() > view.getKnownBlocksDepth())
            {
        		System.out.println("@@@UpdateViewHandler - sync 1");
        		while (message.getBlock().getDepth() != view.getKnownBlocksDepth() + 1) {
        			System.out.println("@@@UpdateViewHandler - sync2");
                    try {
                    	waitingPoint.wait();
                    	if(message.getBlock().getDepth() <= view.getKnownBlocksDepth())
                    	{
                    		return;
                    	}
                    	System.out.println("@@@UpdateViewHandler - sync3");
                    } catch (InterruptedException e) {}
                }
        		System.out.println("@@@UpdateViewHandler - sync4");
                view.getRWLock().acquireWrite();
//                System.out.println("@@@ WRITE LOCKED 1");
//                boolean isExsit;
//                try {
//                	System.out.println("@@@ message.getSendersName(): " + message.getSendersName());
//                	System.out.println("@@@ DsTechShipping.groupServers.getServerName(): " + DsTechShipping.groupServers.getServerName());
//    				isExsit = zkh.checkIfServerExist(message.getSendersName());
//    				System.out.println("@@@ WRITE LOCKED 2");
//    			} catch (KeeperException | InterruptedException e) {
//    				isExsit = false;
//    			}
                System.out.println("@@@ WRITE LOCKED 3");
//                if (isExsit) {
                	System.out.println("@@@ WRITE LOCKED 4");
                	message.getBlock().applyTransactions(view);
                	System.out.println("@@@ WRITE LOCKED 5");
                    view.addToBlockChain(message.getBlock());
                    System.out.println("@@@ WRITE LOCKED 6");
//                }
                System.out.println("@@@ WRITE LOCKED 7");
                waitingPoint.notifyAll();
                System.out.println("@@@ WRITE LOCKED 8");
                view.getRWLock().releaseWrite();
            }
    	}
    	view.removeFromWaitingBlocks(message.getBlock());
    }
}
