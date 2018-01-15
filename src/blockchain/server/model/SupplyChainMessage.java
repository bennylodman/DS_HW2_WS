package blockchain.server.model;

import blockchain.server.group.MessageType;
import blockchain.server.utils.GeneralUtilities;

public class SupplyChainMessage implements java.io.Serializable {
	private static final long serialVersionUID = 1645562847145162541L;
	
	private String sendersName;
	private String targetName;
	private MessageType type;
	private Block block;
	private String args;
	private String blockName;
	
	public String getBlockName() {
		return blockName;
	}

	public void setBlockName(String blockName) {
		this.blockName = blockName;
	}

	public SupplyChainMessage(MessageType type) {
		this(null, type);
	}
	
	public SupplyChainMessage(String blockName, MessageType type) {
		this.type = type;
		this.block = new Block(blockName, null);
	}
	
	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public MessageType getType() {
		return type;
	}
	
	public void setType(MessageType type) {
		this.type = type;
	}
	
	public String getSendersName() {
		return sendersName;
	}

	public void setSendersName(String sendersName) {
		this.sendersName = sendersName;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}
	
	public SupplyChainMessage deepCopy() {
		return GeneralUtilities.deepCopy(this, SupplyChainMessage.class);
	}
	
}
