package blockchain.server.model;

import java.util.ArrayList;
import java.util.List;

public class Block implements java.io.Serializable {
	private static final long serialVersionUID = 6612886098841491544L;
	
	private String blockName;
	private Integer depth;
	private List<Transaction> transactions;
	
	public Block() {
		this(null, null);
	}
	
	public Block(String name, Integer depth) {
		this.blockName = name;
		this.depth = depth;
		this.transactions = new ArrayList<>();
	}
	
	public String getBlockName() {
		return blockName;
	}
	
	public void setBlockName(String blockName) {
		this.blockName = blockName;
	}
	
	public void addTransaction(Transaction trans) {
		this.transactions.add(trans);
	}
	
	public void removeTransactione(int index) {
		this.transactions.remove(index);
	}
	
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public List<Transaction> getTransactions() {
		return transactions;
	}

	public int size() {
		return transactions.size();
	}
	
	public void applyTransactions(SupplyChainView view) {
//		view.getRWLock().acquireWrite();
		for (Transaction trans : transactions) {
			switch (trans.getOperationType()) {
				case MOVE: {
					SupplyChainObject obj = view.getObjectState(trans.getObjectId());
					obj.move(trans.getSource(), trans.getTarget(), view);
					break;
				}
				
				case CREATE: {
					if (trans.getArgs()[0].equals(SupplyChainObject.ITEM)) {
						Item.create(trans.getObjectId(), trans.getTarget(), view);
					} else if (trans.getArgs()[0].equals(SupplyChainObject.CONTAINER)) {
						Container.create(trans.getObjectId(), trans.getTarget(), view);
					} else if (trans.getArgs()[0].equals(SupplyChainObject.SHIP)) {
						Ship.create(trans.getObjectId(), trans.getTarget(), view);
					}
					break;
				}
				
				case DELETE: {
					SupplyChainObject obj = view.getObjectState(trans.getObjectId());
					obj.delete(view);
					break;
				}
			}
		}
//		view.getRWLock().releaseWrite();
	}
	
}
