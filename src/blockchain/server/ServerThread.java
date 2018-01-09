package blockchain.server;

import blockchain.server.group.BlockHandler;
import blockchain.server.group.MessageType;
import blockchain.server.group.UpdateViewHandler;
import blockchain.server.model.BlockHeader;
import blockchain.server.model.SupplyChainMessage;
import blockchain.server.model.SupplyChainView;
import com.google.gson.Gson;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by benny on 04/01/2018.
 */
public class ServerThread extends Thread {

    static private Gson gson = new Gson();
    
    
    private void goToSleep()
    {
        /*Sleep for 1 second*/
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            assert(false);
        }
    }

    private boolean isNeededBlockInList_InsertToView(List<SupplyChainMessage> responseList)
    {
        for(SupplyChainMessage msg : responseList)
        {
            if(msg.getType() == MessageType.RESPONSE_BLOCK && msg.getBlock() !=null)
            {
                /*Found the needed block -> add it to view*/
                new UpdateViewHandler(DsTechShipping.view, msg, DsTechShipping.groupServers.getChannel(), DsTechShipping.groupServers.getServerName(), DsTechShipping.zkHandler).run();
                return true;
            }
        }
        return false;
    }

    /*The function receive sorted list of missing blocks */
    private void handleMissingBlock(List<String> missingBlockList) throws KeeperException, InterruptedException {
        BlockHeader block = null;
        List<SupplyChainMessage> responseList = null;
        List<String> serversNames = null;
        List<String> serversNamesBeforeRequest = null;
        Boolean waitForBlock = true;

        for(String blockString : missingBlockList)
        {
        	System.out.println(blockString);
            block = gson.fromJson(blockString, BlockHeader.class);

            /*Loop while server that created the block is alive or if got the message*/
            while ((DsTechShipping.zkHandler.checkIfServerExist(block.getServerName())) && (DsTechShipping.view.getFromBlockChain(block.getDepth()) == null) )
            {
                /*Busy wait*/
            }

            /*Check if already have this block*/
            if (DsTechShipping.view.getSystemObjects().containsKey(block))
            {
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
                }
                else
                {
                    /*Get current alive servers*/
                    serversNames = DsTechShipping.zkHandler.getServerNames();

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
                        DsTechShipping.zkHandler.removeBlockFromBlockChain(DsTechShipping.getBlockChainView().getKnownBlocksPath(), blockString, block.getDepth()-1);
                        return;
                    }

                }
            }
        }
        return;
    }

    /*Send to all servers the new block and wait to MaxServersCrushSupport + update yourself*/
    private void updateServersWithNewBlock( BlockHandler blockToAddTheChain) throws KeeperException, InterruptedException {
        SupplyChainMessage msg = blockToAddTheChain.getScMessage();
        Integer serversGotTheBlock = 0;
        List<String> serversName;
        List<SupplyChainMessage> responseList;

        /*Send publish message to all*/
        DsTechShipping.groupServers.publishBlock(msg);

        /*while we have not got the amount of ack needed  to continue */
        while(serversGotTheBlock < DsTechShipping.MaxServersCrushSupport)
        {
            serversName = DsTechShipping.getZooKeeperHandler().getServerNames();
            serversGotTheBlock = 0;
            responseList = DsTechShipping.groupServers.waitForResponse();

            if(serversName.size() < DsTechShipping.MaxServersCrushSupport)
            {
                assert (false);
                /*To many servers have failed and cant continue operate*/
            }

            for(SupplyChainMessage ackMsg : responseList)
            {
                if(serversName.contains(ackMsg.getSendersName()))
                {
                    serversGotTheBlock++;
                    /*todo: will current server will send ack message when he receive the message from himself??*/
                    serversName.remove(ackMsg.getSendersName());
                }
            }
        }

        /*There is minimal amount of servers that know the new block*/


    }
    public void run(){
        BlockHandler blockToAddTheChain = null;
        String path = new String();
        List<String> missingBlockList = null;

        while(true)
        {
            /*If handel new block*/
            if(blockToAddTheChain == null)
            {
                /*Close block and open new*/
                synchronized (DsTechShipping.blockHandlerLock)
                {
                    blockToAddTheChain = DsTechShipping.blocksHandler;
                    DsTechShipping.blocksHandler = new BlockHandler();
                }
            }

            /*If block is empty no job to do*/
            if(blockToAddTheChain.size() == 0)
            {
                blockToAddTheChain = null;
                goToSleep();
                continue;
            }
            /*Lock Global view for read - does not change during build of current view*/
            DsTechShipping.view.getRWLock().acquireRead();

            /*Get current system view as this server knows it*/
            SupplyChainView currentView = DsTechShipping.view.getCurrentView();

            /*Release Global view for read - */
            DsTechShipping.view.getRWLock().releaseRead();

            /*Verify that block is legal - after this function need to check that it is not empty*/
            blockToAddTheChain.verifyBlock(currentView);
            /*Check if block empty (All transactions were illegal) -> finish loop and wait for next cycle*/
            if(blockToAddTheChain.size() == 0)
            {
                blockToAddTheChain = null;
                goToSleep();
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
                /*BlockHeader was added to chain*/

                /*Update block depth and name*/
            	String currentNodeName = path.substring(path.lastIndexOf("/") + 1);
                blockToAddTheChain.getScMessage().getBlock().setDepth(currentView.getKnownBlocksDepth() + 1);
                blockToAddTheChain.getScMessage().getBlock().setBlockName(currentNodeName);

                /*Send to all servers the new block and wait to MaxServersCrushSupport + update yourself*/
                try {
                    updateServersWithNewBlock(blockToAddTheChain);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /*Update view to have the new block*/
                /*Will happen on its own but cant wake up the REST threads
                * until the data was updated*/
                new UpdateViewHandler(DsTechShipping.view, blockToAddTheChain.getScMessage(), DsTechShipping.groupServers.getChannel(), DsTechShipping.groupServers.getServerName(), DsTechShipping.zkHandler).run();

                /*Wakeup all REST threads and return that trnsactions happens*/
                blockToAddTheChain.notifySuccessToAll();
                blockToAddTheChain = null;
                goToSleep();

            }else
            {
                /*BlockHeader was not added to chain*/

                /*Need find out what are the missing blocks*/
                try {
                    missingBlockList = DsTechShipping.zkHandler.getAllTheNextBlocks(currentView.getKnownBlocksPath());
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    assert(false);
                }
                assert (missingBlockList.size() != 0);

                /*Request and handle all missing blocks*/
                try {
                    handleMissingBlock(missingBlockList);
                    assert(false);
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    assert(false);
                    e.printStackTrace();
                }

                /*Try again with new depth - next loop will do it*/
            }
        }
    }



}
