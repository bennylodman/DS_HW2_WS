package blockchain.server.group;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import blockchain.server.DsTechShipping;
import blockchain.server.model.Container;
import blockchain.server.model.Item;
import blockchain.server.model.Ship;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainObject;
import blockchain.server.model.SupplyChainView;
import blockchain.server.model.Transaction;
import blockchain.server.model.TransactionResult;



public class BlockHandler {
	private SupplyChainMessage scMessage;
	private List<WaitingObject> waitingThreadObjects;
	
	public BlockHandler() {
		this.scMessage = new SupplyChainMessage(MessageType.PUBLISHE_BLOCK);
		this.waitingThreadObjects = new ArrayList<>();
	}
	
	public TransactionResult addTransaction(Transaction trans) {
		WaitingObject waitingObj;
		synchronized (DsTechShipping.blockHandlerLock) {
			scMessage.getBlock().addTransaction(trans);
			waitingObj = new WaitingObject();
			waitingThreadObjects.add(waitingObj);
		}
		
		while (!waitingObj.isDone()) {
			waitingObj.lock();
		}
		return waitingObj.getResult();
	}
	
	public void notifyTransaction(int transIndex, boolean resStatus, String resMessage, Status errorCode) {
		scMessage.getBlock().removeTransactione(transIndex);
		WaitingObject waitingObj = waitingThreadObjects.get(transIndex);
		waitingThreadObjects.remove(transIndex);
		waitingObj.setResult(resStatus, resMessage, errorCode);
		waitingObj.notifyWaitingThread();
	}
	
	public void notifyFailureToAll(String msg){
		System.out.println("Log :: Server :: Notify all clients of transaction faile");
		for (int i = waitingThreadObjects.size() - 1; i >= 0; i--) {
			this.notifyTransaction(i, false, msg, Status.INTERNAL_SERVER_ERROR);
		}
	}

	public void notifySuccessToAll() {
		System.out.println("Log :: Server :: Notify all clients of transaction success");
		for (int i = waitingThreadObjects.size() - 1; i >= 0; i--) {
			this.notifyTransaction(i, true, "O.K", Status.OK);
		}
	}

	public int size() {
		return this.scMessage.getBlock().size();
	}
	
	
	//TODO: delete 2 functions
	public List<WaitingObject> getWaitingThreadObjects() {
		return waitingThreadObjects;
	}

	public void setWaitingThreadObjects(List<WaitingObject> waitingThreadObjects) {
		this.waitingThreadObjects = waitingThreadObjects;
	}

	//This operation not lock the view database because this view is a local copy of the current view, which accessed only by on thread.
	public void verifyBlock(SupplyChainView view) {
		List<Transaction> transList = this.scMessage.getBlock().getTransactions();
		int transListSize = transList.size();
		int discountSize = 0;
		
		for (int i = 0; i < transListSize - discountSize; i++) {
			Transaction trans = transList.get(i);
			
			if (!view.hasObject(trans.getObjectId()) && trans.getOperationType() != Operation.CREATE) {
				notifyTransaction(i, false, "ERROR: The system does not contain an object with ID: " + trans.getObjectId(), Status.NOT_FOUND);
				i--;
				discountSize++;
				continue;
			}
			
			TransactionResult res = null;
			switch (trans.getOperationType()) {
				case MOVE: {
					SupplyChainObject obj = view.getObjectState(trans.getObjectId());
					res = obj.verifyMove(trans.getSource(), trans.getTarget(), view);
					if (res.getStatus()) {
						obj.move(trans.getSource(), trans.getTarget(), view);
					}
					break;
				}
				
				case CREATE: {
					if (trans.getArgs()[0].equals(SupplyChainObject.ITEM)) {
						res = Item.verifyCreate(trans.getObjectId(), trans.getTarget(), view);
						if (res.getStatus()) {
							Item.create(trans.getObjectId(), trans.getTarget(), view);
						}
					} else if (trans.getArgs()[0].equals(SupplyChainObject.CONTAINER)) {
						res = Container.verifyCreate(trans.getObjectId(), trans.getTarget(), view);
						if (res.getStatus()) {
							Container.create(trans.getObjectId(), trans.getTarget(), view);
						}
					} else if (trans.getArgs()[0].equals(SupplyChainObject.SHIP)) {
						res = Ship.verifyCreate(trans.getObjectId(), trans.getTarget(), view);
						if (res.getStatus()) {
							Ship.create(trans.getObjectId(), trans.getTarget(), view);
						}
					}
					break;
				}
				
				case DELETE: {
					SupplyChainObject obj = view.getObjectState(trans.getObjectId());
					res = obj.verifyDelete(view);
					if (res.getStatus()) {
						obj.delete(view);
					}
					break;
				}
			}
			
			if (!res.getStatus()) {
				notifyTransaction(i, res.getStatus(), res.getMessage(), res.getErrorCode());
				i--;
				discountSize++;
			}
		}
	}

	public SupplyChainMessage getScMessage() {
		return scMessage;
	}
}


class WaitingObject {
	private boolean done;
	private Object lock;
	private TransactionResult result;
	
	public WaitingObject() {
		this.lock = new Object();
		this.result = new TransactionResult();
		this.done = false;
	}
	
	public TransactionResult getResult() {
		return result;
	}
	
	public void notifyWaitingThread() {
		synchronized (this.lock) {
			this.done = true;
			this.lock.notifyAll();
		}
	}
	
	public void lock() {
		synchronized (this.lock) {
			try {
				if (this.done){
					return;
				}
				
				this.lock.wait();
			} catch (InterruptedException e) {}
		}
	}
	
	public void setResult(boolean status, String message, Status errorCode) {
		this.result.setStatus(status);
		this.result.setMessage(message);
		this.result.setErrorCode(errorCode);
	}
	
	public void setResultStatus(boolean status) {
		this.result.setStatus(status);
	}
	
	public void setResultMessage(String message) {
		this.result.setMessage(message);
	}

	public boolean getResultStatus() {
		return this.result.getStatus();
	}

	public String getResultMessage() {
		return this.result.getMessage();
	}
	
//	public void done() {
//		synchronized (lock) {
//			this.done = true;
//		}
//	}
	
	public boolean isDone() {
		synchronized (lock) {
			return this.done;
		}
	}

}
