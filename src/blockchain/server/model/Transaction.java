package blockchain.server.model;

import blockchain.server.group.Operation;

public class Transaction implements java.io.Serializable {
	private static final long serialVersionUID = -5336929627744054412L;
	
	private String objectId;
	private Operation operationType;
	private String source;
	private String target;
	private String[] args;
	
	
	public Transaction(String objectId, Operation op) {
		this(objectId, op, null);
	}
	
	public Transaction(String objectId, Operation op, String[] args) {
		this(objectId, op, null, args);
	}
	
	public Transaction(String objectId, Operation op, String trg, String[] args) {
		this.objectId = objectId;
		this.operationType = op;
		this.source = null;
		this.target = trg;
		this.args = args;
	}
	
	public Transaction(String objectId, Operation op, String src, String trg) {
		this.objectId = objectId;
		this.operationType = op;
		this.source = src;
		this.target = trg;
		this.args = null;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public Operation getOperationType() {
		return operationType;
	}

	public void setOperationType(Operation operationType) {
		this.operationType = operationType;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}
}
