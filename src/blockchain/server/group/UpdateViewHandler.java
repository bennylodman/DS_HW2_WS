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
    	synchronized (DsTechShipping.getBlockChainView().newBlockLock) {
        	if(DsTechShipping.getBlockChainView().getFromNewBlocks(message.getBlock().getDepth()) != null )
        	{
        		DsTechShipping.getBlockChainView().removeFromNewBlocks(message.getBlock().getDepth());
        	}     		
		}

    	
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
    				channel.send(new Message(null, gson.toJson(resopnse)));
    			}
    		} catch (Exception e) {
    			System.out.println("RequestBlockHandler: failed to send message. error: " + e.getMessage());
    		}
    	}

    	
    	synchronized(waitingPoint) {
    		// update local view.
        	if(message.getBlock().getDepth() > view.getKnownBlocksDepth())
            {
        		while (message.getBlock().getDepth() != view.getKnownBlocksDepth() + 1) {
                    try {
                    	waitingPoint.wait();
                    	if(message.getBlock().getDepth() <= view.getKnownBlocksDepth())
                    	{
                    		return;
                    	}
                    } catch (InterruptedException e) {}
                }
                view.getRWLock().acquireWrite();
                message.getBlock().applyTransactions(view);
                view.addToBlockChain(message.getBlock());
                waitingPoint.notifyAll();
                view.getRWLock().releaseWrite();
            }
    	}
    	view.removeFromWaitingBlocks(message.getBlock());
    }
}
