package blockchain.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import blockchain.server.model.SupplyChainObject;
import blockchain.server.utils.ReadersWritersLock;

public class SupplyChainView {
	public Map<String, List<SupplyChainObject>> getSystemObjects() {
		return systemObjects;
	}

	private Map<String, List<SupplyChainObject>> systemObjects;
	private String knownBlocksPath;
	private int knownBlocksDepth;
	private List<Block> blockChain;
	private ReadersWritersLock rwl;
	
	public SupplyChainView() {
		this(0, "/Blockchain");
	}
	
	public SupplyChainView(int knownBlocksDepth, String knownBlocksPath) {
		systemObjects = new HashMap<>();
		rwl = new ReadersWritersLock();
		this.knownBlocksPath = knownBlocksPath;
		this.knownBlocksDepth = knownBlocksDepth;
		blockChain = new ArrayList<>();
	}
	
	public ReadersWritersLock getRWLock() {
		return rwl;
	}

	public void setRWLock(ReadersWritersLock rwl) {
		this.rwl = rwl;
	}
	
	public String getKnownBlocksPath() {
		String res;
		rwl.acquireRead();
		res = knownBlocksPath;
		rwl.releaseRead();
		return res;
	}
 
	public int getKnownBlocksDepth() {
		int res;
		rwl.acquireRead();
		res = knownBlocksDepth;
		rwl.releaseRead();
		return res;
	}

	public void addToBlockChain(Block block) {
//		rwl.acquireWrite();
		if (block.getDepth() != this.knownBlocksDepth + 1) {
//			rwl.releaseWrite();
			return;
		}
			
		blockChain.add(block);
		knownBlocksPath = knownBlocksPath + "/" + block.getBlockName();
		this.knownBlocksDepth++;
//		rwl.releaseWrite();
	}
	
	
	public Block getFromBlockChain(int depth) {
		rwl.acquireRead();
		if (blockChain.size() < depth) {
			rwl.releaseRead();
			return null;
		}
			
		Block blok = blockChain.get(depth - 1);
		rwl.releaseRead();
		return blok;
	}
	
	public void createObject(SupplyChainObject scObject) {
		if (systemObjects.containsKey(scObject.getId())) {
			return;
		}
		
		List<SupplyChainObject> history = new LinkedList<SupplyChainObject>();
		history.add(scObject);
		systemObjects.put(scObject.getId(), history);
	}
	
	public boolean hasObject(String id) {
		return systemObjects.containsKey(id);
	}
	
	public SupplyChainObject getObjectState(String id) {
		if (!systemObjects.containsKey(id))
			return null;
			
		List<SupplyChainObject> history = systemObjects.get(id);
		return history.get(history.size() - 1);
	}
	
	public List<SupplyChainObject> getObjectHistory(String id) {
		if (!systemObjects.containsKey(id))
			return null;
			
		return systemObjects.get(id);
	}
	
	public void addNextState(SupplyChainObject obj) {
		if (!systemObjects.containsKey(obj.getId()))
			return;
		
		systemObjects.get(obj.getId()).add(obj);
	}
	
	public SupplyChainView getCurrentView() {
		final SupplyChainView currentView = new SupplyChainView(this.getKnownBlocksDepth(), this.getKnownBlocksPath());
		
		this.systemObjects.forEach((id, hist) -> {
			SupplyChainObject obj = hist.get(hist.size() - 1);
			currentView.createObject(obj.deepCopy());
		});
		
		return currentView;
	}
}
