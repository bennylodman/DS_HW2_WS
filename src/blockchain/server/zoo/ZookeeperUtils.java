package blockchain.server.zoo;

import com.google.gson.Gson;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ZookeeperUtils {
	public static String TEMP_NODE_PREFIX = "__";
	public static Gson gson = new Gson();
	
	
	/**
	 * create the path to node at given depth
	 * 
	 * @param depthStr - the depth as string.
	 * 
	 * @return return the path to the node at /1/2/.../int(depthStr)
	 */
	public static String createPath(String depthStr) {
		String path = "/";
		int depth = Integer.parseInt(depthStr);
		for (int i = 1; i < depth; i++) {
			path += String.valueOf(i) + "/";
		}
		return path + String.valueOf(depth);
	}
	
	/**
	 * check if the znode which specified at 'path' has child
	 * 
	 * @param path - path to the znode which we want to check
	 * 
	 */
	public static boolean hasNextBlock(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		List<String> childrenList = zk.getChildren(path, false);
		return !childrenList.isEmpty();
	}

	/**
	 * created  znode after the node which specified at 'path'
	 * 
	 * @param path - path to the znode which we want to add block after it.
	 * @param data - json string of BlockHeader class header.
	 * 
	 * @return the actual path of the created node
	 */
	public static String addPersistentSequentialZNode(ZooKeeper zk, String path, String data) throws KeeperException, InterruptedException {
		byte[] dataAsBytes = data.getBytes(StandardCharsets.UTF_8);

		return zk.create(path, dataAsBytes, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
	}

	/**
	 * created  znode after the node which specified at 'path'
	 *
	 * @param path - path to the znode which we want to remove
	 *
	 */
	public static void removeZNode(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		zk.delete(path, zk.exists(path,true).getVersion());
	}

	/**
	 * return the name of the smallest son of the node in the path
	 *
	 * @param path - path to the znode that we want to find his smallest son.
	 *
	 * @return the actual path of the created node
	 */
	public static String returnSmallestSonOfZnode(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		List<String> childrenList = zk.getChildren(path, false);
		System.out.println(childrenList);
		String smallest = childrenList.get(0);
		for (String child : childrenList)
		{
			if(smallest.compareTo(child) > 0)
			{
				smallest = child;
			}
		}
		System.out.println(smallest);
		return path + "/" + smallest;
	}

	/**
	 * return znode od type EPHEMERAL
	 * @param path - path to the znode that we want to find his smallest son.
	 *
	 * @return the actual path of the created node
	 */
	public static String addEephemeralZNode(ZooKeeper zk, String path, String data) throws KeeperException, InterruptedException {
		byte[] dataAsBytes = data.getBytes(StandardCharsets.UTF_8);
		return zk.create(path, dataAsBytes, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
	}

	/**
	 * get sons of znode
	 * * @param path - path to the znode that we want to find his sons.
	 *
	 * @return list of sons (if path does not exist will return empty list)
	 */

	public static List<String> getAllChildrens(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		if (null != zk.exists(path, null))
		{
			return zk.getChildren(path, false);
		}
		return new ArrayList<>();
	}

	public static int getNodeVersion(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		Stat stat = new Stat();
		zk.getData(path, false, stat);
		return stat.getAversion();
	}


	/**
	 * @param path - path to the znode which we want to read it's data
	 * 
	 * @return the data as string from the given znode
	 */
	public static String getNodeData(ZooKeeper zk, String path) throws KeeperException, InterruptedException {
		Stat stat = new Stat();
		byte[] dataAsBytes = zk.getData(path, false, stat);
		String dataAsString = new String(dataAsBytes, StandardCharsets.UTF_8);
		return dataAsString;
	} 

}
