package blockchain.server.group;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

import blockchain.server.DsTechShipping;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;

public class GroupServers extends ReceiverAdapter {
	private static int RESPONSE_WAIT_TIME = 1;
	private static String BRODSCST = "ALL"; 
	private Gson gson = new Gson();
	
	private JChannel channel;
	private String serverName = System.getProperty("user.name", "n/a");
	private SupplyChainView view;
	private ResponseStack rStack;
	
	public GroupServers(SupplyChainView view) {
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
			
		} catch (Exception e) {
			System.out.println("requestBlock: failed to send message");
		}
	}
	
	public void publishBlock(SupplyChainMessage msg) {
		rStack.reset(msg.getBlock().getDepth(), MessageType.ACK);
		msg.setTargetName(BRODSCST);
		msg.setSendersName(serverName);
		try {
			this.channel.send(new Message(null, gson.toJson(msg)));
		} catch (Exception e) {
			System.out.println("publishBlock: failed to send message");
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
				System.out.println("@@@ receive PUBLISHE_BLOCK");
				if (!scMessage.getSendersName().equals(serverName))
					new UpdateViewHandler(view, scMessage, channel, serverName, DsTechShipping.zkHandler).start();
				break;
			}
			
			case REQUEST_BLOCK: {
				System.out.println("@@@ receive REQUEST_BLOCK");
				if (!scMessage.getSendersName().equals(serverName))
					new RequestBlockHandler(view, scMessage, channel, serverName).start();
				break;
			}
			
			case ACK: {
				System.out.println("@@@ receive ACK");
				rStack.addIfRelevant(scMessage);
				break;
			}
			
			case RESPONSE_BLOCK: {
				System.out.println("@@@ receive RESPONSE_BLOCK: " + gson.toJson(scMessage));
				rStack.addIfRelevant(scMessage);
				break;
			}
		}
	}
	
	
	public void viewAccepted(View new_view) {
		System.out.println("** view: " + new_view);
	}


	public void getState(OutputStream output) throws Exception {

	}

	public void setState(InputStream input) throws Exception {

	}
	
//	public void updateTcpConfigFile(String pathToXml) {
//
//		try {
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
//			Document doc = docBuilder.parse(pathToXml);
//			
//			
//			
//		} catch (SAXException | IOException | ParserConfigurationException e) {
//			e.printStackTrace();
//		}
//		
//		
//
//		
//	}
}
