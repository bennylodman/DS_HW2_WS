package blockchain.server.model;


public class BlockHeader {
	private Integer depth;
	private String serverName;

	
	public BlockHeader(Integer depth, String serverName) {
		this.depth = depth;
		this.serverName = serverName;
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
}
