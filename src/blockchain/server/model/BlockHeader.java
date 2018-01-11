package blockchain.server.model;


public class BlockHeader {
	private Integer depth;
	private String serverName;
	private String blockName;

	
	public BlockHeader(Integer depth, String serverName) {
		this.depth = depth;
		this.serverName = serverName;
		this.blockName = null;
	}
	
	public BlockHeader(Integer depth, String serverName, String blockName) {
		this.depth = depth;
		this.serverName = serverName;
		this.blockName = blockName;
	}


	public Integer getDepth() {
		return depth;
	}

	public String getServerName() {
		return serverName;
	}

	public void setDepth(Integer depth) {
		this.depth = depth;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getBlockName() {
		return blockName;
	}

	public void setBlockName(String blockName) {
		this.blockName = blockName;
	}
}
