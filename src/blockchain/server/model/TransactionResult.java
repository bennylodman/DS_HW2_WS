package blockchain.server.model;

import javax.ws.rs.core.Response;

public class TransactionResult {
	private boolean status;
	private String message;
	private Response.Status errorCode;
	
	public TransactionResult() {
		this.status = false;
		this.errorCode = Response.Status.OK;
		this.message = "";
	}
	
	public TransactionResult(boolean status, Response.Status errorCode, String message) {
		this.status = status;
		this.errorCode = errorCode;
		this.message = message;
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

	public Response.Status getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(Response.Status errorCode) {
		this.errorCode = errorCode;
	}
	
	
}
