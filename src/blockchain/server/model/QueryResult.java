package blockchain.server.model;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

public class QueryResult {
	private boolean status;
	private String message;
	private List<SupplyChainObject> requestedObjects;
	private Response.Status errorCode;
	
	public Response.Status getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(Response.Status errorCode) {
		this.errorCode = errorCode;
	}

	public QueryResult(boolean status,Response.Status errorCode, String message) {
		this.status = status;
		this.message = message;
		this.requestedObjects = null;
		this.errorCode = errorCode;
	}
	
	public QueryResult(boolean status, Response.Status errorCode, String message, SupplyChainObject obj) {
		this.status = status;
		this.message = message;
		this.requestedObjects = new ArrayList<>();
		this.requestedObjects.add(obj);
		this.errorCode = errorCode;
	}
	
	public QueryResult(boolean status,Response.Status errorCode, String message, List<SupplyChainObject> requestedObjects) {
		this.status = status;
		this.message = message;
		this.requestedObjects = requestedObjects;
		this.errorCode = errorCode;
	}
	
	public boolean getStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<SupplyChainObject> getRequestedObjects() {
		return requestedObjects;
	}
	public void setRequestedObjects(List<SupplyChainObject> requestedObjects) {
		this.requestedObjects = requestedObjects;
	}
	
	
}
