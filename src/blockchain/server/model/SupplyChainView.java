package blockchain.server.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import blockchain.server.DsTechShipping;
import blockchain.server.model.SupplyChainObject;
import blockchain.server.utils.ReadersWritersLock;

public class SupplyChainView {
	private Map<String, List<SupplyChainObject>> systemObjects;
	private String knownBlocksPath;
	private int knownBlocksDepth;
	private List<Block> blockChain;
	private ReadersWritersLock rwl;
	private Map<Integer, Block> waitingBlocks;
	
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
	
	public boolean addToWaitingBlocks(Block block) {
		rwl.acquireWrite();
		boolean res = false;
		if (!waitingBlocks.containsKey(block.getDepth())) {
			waitingBlocks.put(block.getDepth(), block);
			res = true;
		}
		rwl.releaseWrite();
		return res;
	}
	
	public void removeFromWaitingBlocks(Block block) {
		rwl.acquireWrite();
		if (waitingBlocks.containsKey(block.getDepth())) {
			waitingBlocks.remove(block.getDepth());
		}
		rwl.releaseWrite();
	}

	// Assumes that called when write lock acquired
	public void addToBlockChain(Block block) {
		if (block.getDepth() != this.knownBlocksDepth + 1) {
			return;
		}
		if (waitingBlocks.containsKey(block.getDepth())) {
			waitingBlocks.remove(block.getDepth());
		}
		blockChain.add(block.deepCopy());
		knownBlocksPath = knownBlocksPath + "/" + block.getBlockName();
		this.knownBlocksDepth++;
	}
	
	
	public Block getFromBlockChainAndWaitinqQueue(int depth)
	{
		rwl.acquireRead();
		if ((blockChain.size() < depth) && waitingBlocks.containsKey(depth)) {
			rwl.releaseRead();
			return null;
		}
			
		Block blok = blockChain.get(depth - 1);
		if(blok == null)
		{
			blok = waitingBlocks.get(depth);
		}
		rwl.releaseRead();
		return blok;
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
		rwl.acquireRead();
		boolean res = systemObjects.containsKey(id);
		rwl.releaseRead();
		return res;
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
