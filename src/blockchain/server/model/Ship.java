package blockchain.server.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import blockchain.server.utils.GeneralUtilities;

public class Ship extends SupplyChainObject {
	public static String PREFIX = "S";
	
	private Set<String> containers;
	private String doc;
	
	public Ship(String shipId, String docId) {
		super.id = shipId;
		super.deleted = false;
		this.doc = docId;
		this.containers = new HashSet<String>();
	}
	
	public  List<String> getContainers() {
		List<String> list = new ArrayList<String>();
		list.addAll(containers);
		return list;
	}
	
	public void addContainer(String containerId) {
		containers.add(containerId);
	}
	
	public void removeContainer(String containerId) {
		containers.remove(containerId);
	}
	
	public boolean hasContainer(String containerId) {
		return containers.contains(containerId);
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}
	
	public boolean isEmpty() {
		return this.containers.isEmpty();
	}
	
	public Ship deepCopy() {
		return GeneralUtilities.deepCopy(this, Ship.class);
	}
	
	public static TransactionResult verifyCreate(String id, String docId, SupplyChainView view) {
		if (id == null || !id.startsWith(Ship.PREFIX))
			return new TransactionResult(false, Response.Status.BAD_REQUEST,  "ERROR: " + id + " is invalid ship ID");
		
		if (view.hasObject(id))
			return new TransactionResult(false,Response.Status.BAD_REQUEST, "ERROR: The system already contain an object with ID: " + id);
		
		return new TransactionResult(true,Response.Status.OK, "OK");
	}
	
	public static void create(String id, String docId, SupplyChainView view) {
		Ship ship = new Ship(id, docId);
		view.createObject(ship);
	}
	
	public TransactionResult verifyDelete(SupplyChainView view) {
		if (this.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN,"ERROR: The object " + this.id + " has already been deleted");
		
		if (!this.isEmpty())
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: The ship " + this.id + " is not empty");
		
		return new TransactionResult(true, Response.Status.OK, "OK");
	}
	
	public void delete(SupplyChainView view) {
		Ship shipNextState = this.deepCopy(); 
		shipNextState.setDoc("None");
		shipNextState.setDeleted(true);
		view.addNextState(shipNextState);
	}
	
	public TransactionResult verifyMove(String src, String trg, SupplyChainView currentView) {
		if (!this.doc.equals(src))
			return new TransactionResult(false, Response.Status.BAD_REQUEST, "ERROR: The ship " + this.getId() + " is not anchored in the dock " + src);
		
		if (this.isDeleted())
			return new TransactionResult(false, Response.Status.FORBIDDEN, "ERROR: The object " + this.id + " has been deleted");
		
		return new TransactionResult(true, Response.Status.OK, "OK");
	}
	
	public void move(String src, String trg, SupplyChainView currentView) {
		Ship shipNextState = this.deepCopy();
		shipNextState.setDoc(trg);
		
		for (String containerId : this.containers) {
			Container container = (Container) currentView.getObjectState(containerId).deepCopy();
			container.setDoc(trg);
			container.updateItemsState(currentView, null, trg);
			currentView.addNextState(container);
		}
		
		currentView.addNextState(shipNextState);
	}
}
