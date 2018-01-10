package blockchain.server.model;

import javax.ws.rs.core.Response;

import blockchain.server.utils.GeneralUtilities;

public class Item extends SupplyChainObject {
	public static String PREFIX = "I";
	
	private String container;
	private String ship;
	private String doc;
	
	public Item(String itemId) {
		this(itemId, null, null, null);
	}
	
	public Item(String itemId, String containerId, String shipId, String docId) {
		super.id = itemId;
		super.deleted = false;
		this.container = containerId;
		this.ship = shipId;
		this.doc = docId;
	}
	
	public String getContainer() {
		return container;
	}
	
	public void setContainer(String container) {
		this.container = container;
	}
	
	public String getShip() {
		return ship;
	}
	
	public void setShip(String ship) {
		this.ship = ship;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}
	
	public Item deepCopy() {
		return GeneralUtilities.deepCopy(this, Item.class);
	}
	
	public static TransactionResult verifyCreate(String id, String containerId, SupplyChainView view) {
		if (id == null || !id.startsWith(Item.PREFIX))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: " + id + " is invalid item ID");
		
		if (containerId == null || !containerId.startsWith(Container.PREFIX))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: " + containerId + " is invalid container ID");
		
		if (view.hasObject(id))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: The system already contain an object with ID: " + id);
		
		if (!view.hasObject(containerId))
			return new TransactionResult(false,Response.Status.NOT_FOUND, "ERROR: The system does not contain an object with ID: " + containerId);
		
		Container trgContainer = (Container) view.getObjectState(containerId);
		
		if (trgContainer.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + containerId + " has been deleted");
		
		return new TransactionResult(true, Response.Status.OK, "OK");
	}
	
	public static void create(String id, String containerId, SupplyChainView view) {
		Container container = ((Container) view.getObjectState(containerId)).deepCopy();
		Item item = new Item(id, container.getId(), container.getShip(), container.getDoc());
		
		container.addItem(id);
		
		view.createObject(item);;
		view.addNextState(container);
	}
	
	
	public TransactionResult verifyDelete(SupplyChainView view) {
		if (this.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + this.id + " has already been deleted");
		
		return new TransactionResult(true,Response.Status.OK, "OK");
	}
	
	public void delete(SupplyChainView view) {
		Container container = ((Container) view.getObjectState(this.container)).deepCopy();
		Item itemNextState = this.deepCopy(); 
		
		itemNextState.setContainer("None");
		itemNextState.setShip("None");
		itemNextState.setDoc("None");
		itemNextState.setDeleted(true);
		container.removeItem(itemNextState.getId());
		
		view.addNextState(itemNextState);
		view.addNextState(container);
	}
	
	public TransactionResult verifyMove(String src, String trg, SupplyChainView view) {
		if (src == null || !src.startsWith(Container.PREFIX))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: " + src + " is invalid container ID");
		
		if (trg == null || !trg.startsWith(Container.PREFIX))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: " + trg + " is invalid container ID");
		
		if (!view.hasObject(src))
			return new TransactionResult(false,Response.Status.NOT_FOUND, "ERROR: The system does not contain an object with ID: " + src);
		
		if (!view.hasObject(trg))
			return new TransactionResult(false,Response.Status.NOT_FOUND, "ERROR: The system does not contain an object with ID: " + trg);
		
		Container srcContainer = (Container) view.getObjectState(src);
		Container trgContainer = (Container) view.getObjectState(trg);
		
		if (this.isDeleted())
			return new TransactionResult(false,Response.Status.FORBIDDEN, "ERROR: The object " + this.id + " has been deleted"); 
		
		if (srcContainer.isDeleted())
			return new TransactionResult(false,Response.Status.FORBIDDEN, "ERROR: The object " + srcContainer.getId() + " has been deleted");
		
		if (trgContainer.isDeleted())
			return new TransactionResult(false,Response.Status.FORBIDDEN, "ERROR: The object " + trgContainer.getId() + " has been deleted");
		
		if (!this.container.equals(src))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: The item " + this.getId() + " is not in the container " + src);
		
		if (srcContainer.getDoc() != trgContainer.getDoc())
			return new TransactionResult(false,Response.Status.BAD_REQUEST,  "ERROR: " + src + " and " + trg + " are not on the same dock");
		
		return new TransactionResult(true,Response.Status.OK, "OK");
	}
	
	public void move(String src, String trg, SupplyChainView view) {
		Container srcContainer = ((Container) view.getObjectState(src)).deepCopy();
		Container trgContainer = ((Container) view.getObjectState(trg)).deepCopy();
		Item itemNextState = this.deepCopy(); 
		
		itemNextState.setContainer(trgContainer.getId());
		srcContainer.removeItem(getId());
		trgContainer.addItem(getId());
		
		view.addNextState(itemNextState);
		view.addNextState(srcContainer);
		view.addNextState(trgContainer);
	}
}	
