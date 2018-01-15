package blockchain.server.group;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import com.google.gson.Gson;

import blockchain.server.DsTechShipping;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;

public class GroupServers extends ReceiverAdapter {
	private static int RESPONSE_WAIT_TIME = 1;
	private static String BRODSCST = "ALL"; 
	private Gson gson = new Gson();
	private JChannel channel;
	private String serverName;
	private SupplyChainView view;
	private ResponseStack rStack;
	
	public GroupServers(SupplyChainView view) {
		Random rand = new Random();		
		this.serverName = System.getProperty("user.name", "n/a") + String.valueOf(rand.nextInt(1000));
		this.rStack = new ResponseStack();
		this.view = view;
		try {
			channel = new JChannel("C:/workspace/Java/DS_HW2_WS/WebContent/WEB-INF/config/tcp.xml");
			channel.setReceiver(this);
			channel.connect("GroupServers");
			channel.getState(null, 10000);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public JChannel getChannel() {
		return channel;
	}

	public void setChannel(JChannel channel) {
		this.channel = channel;
	}

	public String getServerName() {
		return serverName;
	}
	
	public void requestBlock(int blockDepth) {
		rStack.reset(blockDepth, MessageType.RESPONSE_BLOCK);
		SupplyChainMessage scMessage = new SupplyChainMessage(MessageType.REQUEST_BLOCK);
		scMessage.setArgs(String.valueOf(blockDepth));
		scMessage.setTargetName(BRODSCST);
		scMessage.setSendersName(serverName);
		try {
			this.channel.send(new Message(null, gson.toJson(scMessage)));
			System.out.println("Log :: Network :: sent REQUEST_BLOCK message to all servers, request for blcok number: " + blockDepth);
			
		} catch (Exception e) {
			System.out.println("Log :: Network :: failed to send REQUEST_BLOCK message to all servers, request for blcok number: " + blockDepth);
		}
	}
	
	public void publishBlock(SupplyChainMessage msg) {
		rStack.reset(msg.getBlock().getDepth(), MessageType.ACK);
		msg.setTargetName(BRODSCST);
		msg.setSendersName(serverName);
		try {
			this.channel.send(new Message(null, gson.toJson(msg)));
			System.out.println("Log :: Network :: sent PUBLISHE_BLOCK message to all servers with block " + msg.getBlock().getBlockName());
		} catch (Exception e) {
			System.out.println("Log :: Network :: failed to send PUBLISHE_BLOCK message to all servers with block " +  msg.getBlock().getBlockName());
		}
	}
	
	public List<SupplyChainMessage> waitForResponse() {
		try {
			TimeUnit.SECONDS.sleep(RESPONSE_WAIT_TIME);
		} catch (InterruptedException e) {}
		return rStack.fetchStack();
	}

	public void receive(Message msg) {
		String msgStr = (String) msg.getObject();
		SupplyChainMessage scMessage = gson.fromJson(msgStr, SupplyChainMessage.class);
		
		switch (scMessage.getType()) {
			case PUBLISHE_BLOCK: {
				if (!scMessage.getSendersName().equals(serverName))
				{
					System.out.println("Log :: Network :: receive PUBLISHE_BLOCK message from "+   scMessage.getSendersName() + " with block" + scMessage.getBlock().getBlockName());
					new UpdateViewHandler(view, scMessage, channel, serverName, DsTechShipping.zkHandler, true).start();
				}
				break;
			}	
			case REQUEST_BLOCK: {
				if (!scMessage.getSendersName().equals(serverName))
				{
					System.out.println("Log :: Network :: receive REQUEST_BLOCK message from "+   scMessage.getSendersName() + "requesting block" + scMessage.getBlock().getBlockName());
					new RequestBlockHandler(view, scMessage, channel, serverName).start();
				}	
				break;
			}
			
			case ACK: {
				System.out.println("Log :: Network :: receive ACK message from "+   scMessage.getSendersName() + " that received blcok" );
				rStack.addIfRelevant(scMessage);
				break;
			}
			
			case RESPONSE_BLOCK: {
				System.out.println("Log :: Network :: receive RESPONSE_BLOCK message from "+   scMessage.getSendersName() + " with block" );
				rStack.addIfRelevant(scMessage);
				break;
			}
		}
	}
	
	
	public void viewAccepted(View new_view) {
		//TODO: Benny, Ask Ami what is this?
		System.out.println("** view: " + new_view);
	}

	public void getState(OutputStream output) throws Exception {

	}
	public void setState(InputStream input) throws Exception {
	}
}
