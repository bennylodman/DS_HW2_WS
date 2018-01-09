package blockchain.server.model;

public abstract class SupplyChainObject {
	public static String ITEM = "Item";
	public static String CONTAINER = "Container";
	public static String SHIP = "Ship";
	
	protected String id;
	protected boolean deleted;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	abstract public SupplyChainObject deepCopy();
	abstract public void move(String src, String trg, SupplyChainView currentView);
	abstract public void delete(SupplyChainView currentView);
	abstract public TransactionResult verifyMove(String src, String trg, SupplyChainView currentView);
	abstract public TransactionResult verifyDelete(SupplyChainView currentView);
}
