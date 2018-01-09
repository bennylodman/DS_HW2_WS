package blockchain.server.model;

import java.util.List;

public class History {
	private List<SupplyChainObject> history;

	public History(List<SupplyChainObject> history) {
		this.history = history;
	}

	public List<SupplyChainObject> getRequestedObjects() {
		return history;
	}

	public void setRequestedObjects(List<SupplyChainObject> history) {
		this.history = history;
	}
	
	
}
