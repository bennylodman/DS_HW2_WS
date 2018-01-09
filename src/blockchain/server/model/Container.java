package blockchain.server.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import blockchain.server.utils.GeneralUtilities;

public class Container extends SupplyChainObject {
	public static String PREFIX = "C";
	
	private String ship;
	private Set<String> items;
	private String doc;
	
	public Container(String containerId) {
		this(containerId, null, null);
	}
	
	public Container(String containerId, String shipId, String docId) {
		super.id = containerId;
		super.deleted = false;
		this.ship = shipId;
		this.doc = docId;
		this.items = new HashSet<>();
	}
	
	public  List<String> getItems() {
		List<String> list = new ArrayList<String>();
		list.addAll(items);
		return list;
	}
	
	public String getShip() {
		return ship;
	}
	
	public void setShip(String ship) {
		this.ship = ship;
	}
	
	public void addItem(String itemsId) {
		items.add(itemsId);
	}
	
	public void removeItem(String itemsId) {
		items.remove(itemsId);
	}
	
	public boolean hasItem(String itemsId) {
		return items.contains(itemsId);
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}
	
	public boolean isOnDoc() {
		return this.ship.isEmpty();
	}
	
	public boolean isEmpty() {
		return this.items.isEmpty();
	}
	
	public Container deepCopy() {
		return GeneralUtilities.deepCopy(this, Container.class);
	}
	
	public void updateItemsState(SupplyChainView view, String newShipId, String newDocId) {
		for (String itemId : this.items) {
			Item itemNextState = ((Item) view.getObjectState(itemId)).deepCopy();
			
			if (newShipId != null)
				itemNextState.setShip(newShipId);
			
			if (newDocId != null)
				itemNextState.setDoc(newDocId);
			
			view.addNextState(itemNextState);
		}
	}
	
	public static TransactionResult verifyCreate(String id, String shipId, SupplyChainView view) {
		if (id == null || !id.startsWith(Container.PREFIX))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: " + id + " is invalid container ID");
		
		if (shipId == null || !shipId.startsWith(Ship.PREFIX))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: " + shipId + " is invalid container ID");
		
		if (view.hasObject(id))
			return new TransactionResult(false,Response.Status.BAD_REQUEST,  "ERROR: The system already contain an object with ID: " + id);
		
		if (!view.hasObject(shipId))
			return new TransactionResult(false,Response.Status.NOT_FOUND, "ERROR: The system does not contain an object with ID: " + shipId);
		
		return new TransactionResult(true,Response.Status.OK, "OK");
	}
	
	public static void create(String id, String shipId, SupplyChainView view) {
		Ship ship = ((Ship) view.getObjectState(shipId)).deepCopy();
		Container container = new Container(id, ship.getId(), ship.getDoc());
		
		ship.addContainer(id);
		
		view.createObject(container);;
		view.addNextState(ship);
	}
	
	public TransactionResult verifyDelete(SupplyChainView view) {
		if (this.isDeleted())
			return new TransactionResult(false,Response.Status.FORBIDDEN, "ERROR: The object " + this.id + " has already been deleted");
		
		if (!this.isEmpty())
			return new TransactionResult(false,Response.Status.FORBIDDEN, "ERROR: The container " + this.id + " is not empty");
		
		return new TransactionResult(true,Response.Status.OK, "OK");
	}
	
	public void delete(SupplyChainView view) {
		Ship ship = ((Ship) view.getObjectState(this.ship)).deepCopy();
		Container containerNextState = this.deepCopy(); 
		
		containerNextState.setShip("None");
		containerNextState.setDoc("None");
		containerNextState.setDeleted(true);
		ship.removeContainer(containerNextState.getId());
		
		view.addNextState(containerNextState);
		view.addNextState(ship);
	}
	
	public TransactionResult verifyMove(String src, String trg, SupplyChainView currentView) {
		if (src == null || !src.startsWith(Ship.PREFIX))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: " + src + " is invalid ship ID");
		
		if (trg == null || !trg.startsWith(Ship.PREFIX))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: " + trg + " is invalid ship ID");
		
		if (!currentView.hasObject(src))
			return new TransactionResult(false, Response.Status.NOT_FOUND, "ERROR: The system does not contain an object with ID: " + src);
		
		if (!currentView.hasObject(trg))
			return new TransactionResult(false, Response.Status.NOT_FOUND,  "ERROR: The system does not contain an object with ID: " + trg);
		
		Ship srcShip = (Ship) currentView.getObjectState(src);
		Ship trgShip = (Ship) currentView.getObjectState(trg);
		
		if (this.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + this.id + " has been deleted"); 
		
		if (srcShip.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + srcShip.getId() + " has been deleted");
		
		if (trgShip.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + trgShip.getId() + " has been deleted");
		
		if (!this.ship.equals(src))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: The container " + this.getId() + " is not on the ship " + src);
		
		if (srcShip.getDoc() != trgShip.getDoc())
			return new TransactionResult(false, Response.Status.BAD_REQUEST,   "ERROR: " + src + " and " + trg + " are not on the same dock");
		
		return new TransactionResult(true, Response.Status.OK, "OK");
	}
	
	public void move(String src, String trg, SupplyChainView view) {
		Ship srcShip = ((Ship) view.getObjectState(src)).deepCopy();
		Ship trgShip = ((Ship) view.getObjectState(trg)).deepCopy();
		Container containerNextState = this.deepCopy();
		
		containerNextState.setShip(trgShip.getId());
		containerNextState.updateItemsState(view, trgShip.getId(), null);
		srcShip.removeContainer(getId());
		trgShip.addContainer(getId());
		
		view.addNextState(containerNextState);
		view.addNextState(srcShip);
		view.addNextState(trgShip);
	}
}

