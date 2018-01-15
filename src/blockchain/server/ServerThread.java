package blockchain.server;

import blockchain.server.group.BlockHandler;
import blockchain.server.group.MessageType;
import blockchain.server.group.UpdateViewHandler;
import blockchain.server.model.BlockHeader;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;
import blockchain.server.zoo.ZookeeperUtils;

import com.google.gson.Gson;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by benny on 04/01/2018.
 */
public class ServerThread extends Thread {
	static public int SleepBetweenInsertingBlocks = 5;
	static public int SleepBetweenValidateBlockchain = 1;
	static public long WaitTimeForServerToRecieveBlock = 500; //in millisecond
	static private Gson gson = new Gson();


	private void goToSleep()
	{
		/*Sleep for 1 second*/
		try {
			TimeUnit.SECONDS.sleep(SleepBetweenValidateBlockchain);
		} catch (InterruptedException e) {
			e.printStackTrace();
			assert(false);
		}
	}

	private boolean isNeededBlockInList_InsertToView(List<SupplyChainMessage> responseList)
	{
		for(SupplyChainMessage msg : responseList)
		{
			if(msg.getType() == MessageType.RESPONSE_BLOCK && msg.getBlock() != null)
			{
				/*Found the needed block -> add it to view*/
				new UpdateViewHandler(DsTechShipping.view, msg, DsTechShipping.groupServers.getChannel(), DsTechShipping.groupServers.getServerName(), DsTechShipping.zkHandler, false).run();
				assert(DsTechShipping.getBlockChainView().getKnownBlocksDepth() == msg.getBlock().getDepth());
				return true;
			}
		}
		return false;
	}

	/*The function receive sorted list of missing blocks */
	private void handleMissingBlock(String knownBlocksPath, List<BlockHeader> missingBlockList, Boolean isNewServerFlag) throws KeeperException, InterruptedException {
		List<SupplyChainMessage> responseList = null;
		List<String> serversNames = null;
		List<String> serversNamesBeforeRequest = null;
		Boolean waitForBlock = true;
		long maxTime; 
		for(BlockHeader block : missingBlockList)
		{			
			maxTime = System.currentTimeMillis();
			maxTime += WaitTimeForServerToRecieveBlock; 
			/*Loop while server that created the block is alive or if got the message*/
			while ((maxTime > System.currentTimeMillis() ) && (!isNewServerFlag) && (DsTechShipping.zkHandler.checkIfServerExist(block.getServerName())) && (DsTechShipping.view.getFromBlockChain(block.getDepth()) == null) )
			{
				/*Busy wait*/
			}
			/*Check if already have this block*/
			if (DsTechShipping.view.getFromBlockChain(block.getDepth()) != null)
			{
				System.out.println("Log :: Update local data base :: block  " + block.getDepth() + "is up to date");
				continue;
			}
			/*Get servers list*/
			serversNamesBeforeRequest = DsTechShipping.zkHandler.getServerNames();

			/*Send request message with current block to all servers*/
			DsTechShipping.groupServers.requestBlock(block.getDepth());

			/*While(got the block || all servers returned dont have it || already have block in view*/
			while(waitForBlock)
			{
				/*Get response messages*/
				responseList = DsTechShipping.groupServers.waitForResponse();

				if (isNeededBlockInList_InsertToView(responseList) || (DsTechShipping.view.getFromBlockChain(block.getDepth()) != null))
				{
					/*The server got this block*/
					waitForBlock = false;
					System.out.println("Log :: Update local data base :: block  " + block.getDepth() + "is up to date");
				}
				else
				{
					/*Get current alive servers*/
					serversNames = DsTechShipping.zkHandler.getServerNames();
					serversNames.remove(DsTechShipping.groupServers.getServerName());

					/*Remove from current alive servers all the servers that joined after the request*/
					for(String name: serversNames)
					{
						if(!serversNamesBeforeRequest.contains(name))
						{
							serversNames.remove(name);
						}
					}

					for(SupplyChainMessage msg : responseList)
					{
						if(serversNames.contains(msg.getSendersName()))
						{
							serversNames.remove(msg.getSendersName());
						}
					}
					if(serversNames.isEmpty())
					{
						/*All optional servers returned negative ack
						 * Block does not exist eny more -> remove it from block chain*/
						//DsTechShipping.zkHandler.removeBlockFromBlockChain(DsTechShipping.getBlockChainView().getKnownBlocksPath());
						DsTechShipping.zkHandler.removeBlockFromBlockChain(knownBlocksPath + "/" + block.getBlockName());	
						System.out.println("Log :: Update local data base :: block  " + block.getBlockName() + "was removed from blockchain");
						return;
					}

				}
			}
		}
		return;
	}

	/*Send to all servers the new block and wait to MaxServersCrushSupport + update yourself*/
	private Boolean updateServersWithNewBlock( BlockHandler blockToAddTheChain) throws KeeperException, InterruptedException {
		SupplyChainMessage msg = blockToAddTheChain.getScMessage();
		Integer serversGotTheBlock = 0;
		List<String> serversName;
		List<SupplyChainMessage> responseList;
		
		serversName = DsTechShipping.getZooKeeperHandler().getServerNames();
		serversName.remove(DsTechShipping.groupServers.getServerName());
		
		if(serversName.size() < DsTechShipping.MaxServersCrushSupport)
		{
			System.out.println("Log :: New block :: System failure - Not enough servers in the system to continue");
			return false;
			/*To many servers have failed and cant continue operate*/
		}
		
		/*Send publish message to all*/
		DsTechShipping.groupServers.publishBlock(msg);

		/*If not enough servers - dont even try to send*/
			
		/*while we have not got the amount of ack needed  to continue */
		while(serversGotTheBlock < DsTechShipping.MaxServersCrushSupport)
		{
			serversName = DsTechShipping.getZooKeeperHandler().getServerNames();
			serversName.remove(DsTechShipping.groupServers.getServerName());
			
			serversGotTheBlock = 0;
			responseList = DsTechShipping.groupServers.waitForResponse();

			if(serversName.size() < DsTechShipping.MaxServersCrushSupport)
			{
				System.out.println("Log :: New block :: System failure - Not enough servers in the system to continue");
				return false;
				/*To many servers have failed and cant continue operate*/
			}

			for(SupplyChainMessage ackMsg : responseList)
			{
				if(serversName.contains(ackMsg.getSendersName()))
				{
					serversGotTheBlock++;

					serversName.remove(ackMsg.getSendersName());
				}
			}
		}

		/*There is minimal amount of servers that know the new block*/
		System.out.println("Log :: New block :: Block was added successfully");
		return true;
	}


	private void checkAndUpdateMissingBlocks(String knownBlocksPath, boolean notWaitForCraetor) {
		List<BlockHeader> missingBlockList = null;

		/*Need find out what are the missing blocks*/
		try {
			missingBlockList = DsTechShipping.zkHandler.getAllTheNextBlocks(knownBlocksPath);
			System.out.println("Log :: Update local data base :: check missing blocks");
			if(!missingBlockList.isEmpty())
			{
				System.out.println("Log :: Update local data base :: missing blocks are: " + missingBlockList);
				handleMissingBlock(knownBlocksPath, missingBlockList, notWaitForCraetor);	
			}
			else
			{
				System.out.println("Log :: Update local data base :: no missing blocks");
			}
					
		} catch (KeeperException | InterruptedException e) {
		}
	}


	public void run(){
		BlockHandler blockToAddTheChain = null;
		String path = new String();
		int counterForBlockInsertion = 0;
		try {
			DsTechShipping.zkHandler.addServer(DsTechShipping.groupServers.getServerName());
		} catch (KeeperException | InterruptedException e1) {}
		
		System.out.println("Log :: Server :: Initialization :: check missing blocks");
		checkAndUpdateMissingBlocks(DsTechShipping.view.getKnownBlocksPath(), true);
		System.out.println("Log :: Server :: Initialization :: local server data updated");
		
		while(true)
		{
			/*If handel new block*/
			if(blockToAddTheChain == null)
			{
				if (counterForBlockInsertion >= SleepBetweenInsertingBlocks) {
					counterForBlockInsertion = 0;
					/*Close block and open new*/
					synchronized (DsTechShipping.blockHandlerLock)
					{
						blockToAddTheChain = DsTechShipping.blocksHandler;
						DsTechShipping.blocksHandler = new BlockHandler();
					}
				}
			}

			/*If block is empty no job to do*/
			if(blockToAddTheChain == null || blockToAddTheChain.size() == 0)
			{
				blockToAddTheChain = null;
				counterForBlockInsertion += SleepBetweenValidateBlockchain;
				System.out.println("Log :: Server :: Periodic local data base update");
				checkAndUpdateMissingBlocks(DsTechShipping.view.getKnownBlocksPath(), true);
				goToSleep();
				continue;
			}
			
			/*Lock Global view for read - does not change during build of current view*/
			DsTechShipping.view.getRWLock().acquireRead();

			System.out.println("Log :: Server :: Start block insertion procedure, block depth: " + DsTechShipping.view.getKnownBlocksDepth() + 1);
			
			/*Get current system view as this server knows it*/
			SupplyChainView currentView = DsTechShipping.view.getCurrentView();

			/*Release Global view for read - */
			DsTechShipping.view.getRWLock().releaseRead();

			/*Verify that block is legal - after this function need to check that it is not empty*/
			System.out.println("Log :: Server :: Start block number: " + blockToAddTheChain.getScMessage().getBlock().getDepth() + " verification");
			
			blockToAddTheChain.verifyBlock(currentView);
			
			
			/*Check if block empty (All transactions were illegal) -> finish loop and wait for next cycle*/
			if(blockToAddTheChain.size() == 0)
			{
				continue;
			}

			/*Create block header to insert to Znode*/
			BlockHeader blckToZnode = new BlockHeader(currentView.getKnownBlocksDepth() + 1,DsTechShipping.groupServers.getServerName());

			/*Try to add block to the block chain*/
			try {
				path = DsTechShipping.zkHandler.addBlockToBlockChain(currentView.getKnownBlocksPath(), gson.toJson(blckToZnode), currentView.getKnownBlocksDepth() + 1);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
				assert(false);
			} 

			if(path != null)
			{

				/*Update block depth and name*/
				String currentNodeName = path.substring(path.lastIndexOf("/") + 1);
				blockToAddTheChain.getScMessage().getBlock().setDepth(currentView.getKnownBlocksDepth() + 1);
				blockToAddTheChain.getScMessage().getBlock().setBlockName(currentNodeName);
				blockToAddTheChain.getScMessage().setSendersName(DsTechShipping.getGroupServers().getServerName());
				
				/*BlockHeader was added to chain*/
				System.out.println("Log :: Server :: Block number: " + blockToAddTheChain.getScMessage().getBlock().getDepth() + " was added to global blockcahin");
				
				/*Send to all servers the new block and wait to MaxServersCrushSupport + update yourself*/
				try {
					if(!updateServersWithNewBlock(blockToAddTheChain))
					{
						System.out.println("Log :: Server :: error - Not enough servers in the system");
						blockToAddTheChain.notifyFailureToAll("error - Not enough servers in the system");
						DsTechShipping.getZooKeeperHandler().removeBlockFromBlockChain(path);
						blockToAddTheChain = null;
						continue;
						
					}
				} catch (KeeperException | InterruptedException e) {
					e.printStackTrace();
				}
				
				/*Update view to have the new block*/
				/*Will happen on its own but cant wake up the REST threads
				 * until the data was updated*/
				new UpdateViewHandler(DsTechShipping.view, blockToAddTheChain.getScMessage(), DsTechShipping.groupServers.getChannel(), DsTechShipping.groupServers.getServerName(), DsTechShipping.zkHandler, false).run();
				
				System.out.println("Log :: Server :: Block number: " + blockToAddTheChain.getScMessage().getBlock().getDepth() + " is accepted");
				
				/*Wakeup all REST threads and return that trnsactions happens*/
				blockToAddTheChain.notifySuccessToAll();
				blockToAddTheChain = null;

			}else
			{
				checkAndUpdateMissingBlocks(currentView.getKnownBlocksPath(), false);
			}
		}
	}



}
