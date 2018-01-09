package blockchain.server.zoo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;


// this class will handle all work with the zookeeper server
public class ZooKeeperHandler implements Watcher {
	public static String ZK_ADDR = "192.168.1.13:2181,192.168.1.21:2181";
	public static int ZK_PORT = 2181;

	private static ZooKeeper zk;
	private static Object mutex;
	
	public ZooKeeperHandler() {
		try {
			zk = new ZooKeeper(ZK_ADDR, ZK_PORT, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mutex = new Object();
	}
	
    synchronized public void process(WatchedEvent event) {
        synchronized (mutex) {
            mutex.notify();
        }
    }

	/**
	 * add new block to chain
	 * - verify that the path is to the end of the chain and that it is the smallest son added
	 *
	 * @param path - path to the last znode in the block chain.
	 * @param data - json string of BlockHeader Header (Server Id, BlockHeader id).
	 * @param depth - current block chain length
	 *
	 * @return the actual path of the created node
	 * if null -> not the smallest node, need to update it's
	 */
    public String addBlockToBlockChain(String path, String data, int depth)throws KeeperException, InterruptedException
	{
		/*If has node in current path -> the chain is already longer*/
		if(ZookeeperUtils.hasNextBlock(zk,path))
		{
			return null;
		}

		/*Add the new Znode to chain block*/
		String znodePath = ZookeeperUtils.addPersistentSequentialZNode(zk, path.concat("/" + Integer.toString(depth)), data);

		/*Get all the Znodes sones*/
		String smallestZnodePath = ZookeeperUtils.returnSmallestSonOfZnode(zk, path);
		/*Check if the node added was the smallest one*/
		if (znodePath.equals(smallestZnodePath))
		{
			return znodePath;
		}
		/*Was not the smalles -> remove the son*/
		ZookeeperUtils.removeZNode(zk, znodePath);
		return null;
	}

	/**
	 * add new block to chain
	 * - verify that the path is to the end of the chain and that it is the smallest son added
	 *
	 * @param path - path to the father of the znode in the block chain.
	 * @param data - json string of BlockHeader Header (Server Id, BlockHeader id) to compare that this is the right one.
	 * @param depth - current block chain length
	 *
	 */
	public void removeBlockFromBlockChain(String path, String data, int depth)throws KeeperException, InterruptedException
	{
		List<String> sonList = ZookeeperUtils.getAllChildrens(zk, path);
		String blocksData = null;
		for (String son : sonList)
		{
			blocksData = ZookeeperUtils.getNodeData(zk, son);
			/*It is necessary in order to make sure that some one else didn't removed it and added new one
			* must check block id + server that created him is matching*/
			if (data.equals(blocksData))
			{
				ZookeeperUtils.removeZNode(zk, son);
				return;
			}
		}
		return;
	}

	/**
	 * add new server zNode
	 * - create EPHEMERAL znode with servers name in "Server" subTree
	 * - this way other servers can now is some server still exists.
	 *
	 *
	 * @param serversName - server name
	 */
	public void addServer(String serversName) throws KeeperException, InterruptedException
	{
			String path = "/Servers";
			String serversPath = path + "/" + serversName;
			if (ZookeeperUtils.getAllChildrens(zk, path).contains(serversPath))
			{
				/*Server with same name already in the system - not allowed*/
				assert(false);
			}

			ZookeeperUtils.addEephemeralZNode(zk,serversPath,serversName);
	}

	/**
	 * the function receive a path to znode in the block chain and return the delta from
	 * this path to the end of the block chain
	 *
	 * @param path - 
	 */
	public String getCahinSuffix(String path)throws KeeperException, InterruptedException
	{
		String currentPath = path;
		while(ZookeeperUtils.hasNextBlock(zk, currentPath))
		{
			currentPath = currentPath + "/" + getSmallestZnodeName(ZookeeperUtils.getAllChildrens(zk,currentPath));
		}
		currentPath = currentPath.replace(path, "");
		return currentPath;
	}

	/**
	 * the function receive a path to znode in the block chain and return in a list all the
	 * data contained in the next Znode
	 * (Need it in order to have the missing blocks and the servers created them and the depth)
	 *
	 * @param path - server name
	 */
	public List<String> getAllTheNextBlocks(String path)throws KeeperException, InterruptedException
	{
		List<String> blockList = new ArrayList<>();
		String currentPath = new String(path);

		String suffixPath = getCahinSuffix(path);
		if(suffixPath.equals(""))
		{
			return blockList;
		}
		String[] parts = suffixPath.split("/");
		for(int i=1; i<parts.length; i++)
		{
			currentPath.concat("/");
			currentPath.concat(parts[i]);
			blockList.add(ZookeeperUtils.getNodeData(zk, currentPath));
		}
		return blockList;
	}

	/**
	 * the function receive a servers name and check if this server exist
	 *
	 * @param serverName - server name
	 * @return true if exist, false if not
	 */
	public boolean checkIfServerExist(String serverName)throws KeeperException, InterruptedException
	{
		return (zk.exists("/Servers/" + serverName, null) != null);

	}

	/**
	 * the function return servers amount
	 *
	 * @return servers count
	 */
	public Integer getServerAmount()throws KeeperException, InterruptedException
	{
		return ZookeeperUtils.getAllChildrens(zk,"/Servers").size();
	}

	/**
	 * the function return servers names
	 *
	 * @return servers count
	 */
	public List<String> getServerNames()throws KeeperException, InterruptedException
	{
		List<String> list =  ZookeeperUtils.getAllChildrens(zk,"/Servers");
		List<String> namesList = new ArrayList<>();
		for(String str : list)
		{
			namesList.add(str.replaceFirst("/Servers/", ""));
		}
		return namesList;
	}


	private String getSmallestZnodeName(List<String> list)
	{
		assert(!list.isEmpty());
		String small = list.get(0);
		for (String child : list)
		{
			if(small.compareTo(child) > 0)
			{
				small = child;
			}
		}
		return small;
	}




}
