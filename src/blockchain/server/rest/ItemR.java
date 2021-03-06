package blockchain.server.rest;


import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;

import blockchain.server.DsTechShipping;
import blockchain.server.model.History;
import blockchain.server.model.Item;
import blockchain.server.model.QueryResult;
import blockchain.server.model.TransactionResult;

@Path("/items")
public class ItemR {
	private Gson gson = new Gson();

	@GET
	@Path("/{itemId}")
	public Response getItem(@PathParam("itemId") String itemId) {
		QueryResult qr = DsTechShipping.getItemState(itemId);
		System.out.println("Log :: Rest :: Request for item with id:" + itemId + " was received");
		if (qr.getStatus()) {
			String itemStr = gson.toJson(qr.getRequestedObjects().get(0));
			return Response.ok(itemStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{itemId}/container")
	public Response getItemsContainer(@PathParam("itemId") String itemId) {
		QueryResult qr = DsTechShipping.getItemState(itemId);
		System.out.println("Log :: Rest :: Request for container of the item with id:" + itemId + " was received");
		if (qr.getStatus()) {
			Item item = (Item)qr.getRequestedObjects().get(0);
			return Response.ok(item.getContainer(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{itemId}/ship")
	public Response getItemsShip(@PathParam("itemId") String itemId) {
		QueryResult qr = DsTechShipping.getItemState(itemId);
		System.out.println("Log :: Rest :: Request for ship of the item with id:" + itemId + " was received");
		if (qr.getStatus()) {
			Item item = (Item)qr.getRequestedObjects().get(0);
			return Response.ok(item.getShip(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{itemId}/dock")
	public Response getItemsDock(@PathParam("itemId") String itemId) {
		QueryResult qr = DsTechShipping.getItemState(itemId);
		System.out.println("Log :: Rest :: Request for dock of the item with id:" + itemId + " was received");
		if (qr.getStatus()) {
			Item item = (Item)qr.getRequestedObjects().get(0);
			return Response.ok(item.getDoc(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{itemId}/history")
	public Response getItemsHistory(@PathParam("itemId") String itemId) {
		QueryResult qr = DsTechShipping.getItemHist(itemId);
		System.out.println("Log :: Rest :: Request for the history of the item with id:" + itemId + " was received");
		if (qr.getStatus()) {
			History history = new History(qr.getRequestedObjects());
			String historyStr = gson.toJson(history);
			return Response.ok(historyStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@PUT
	@Path("/{itemId}")
	public Response creteItem(@PathParam("itemId") String itemId, @QueryParam("dst") String dst) {
		if(dst == null)
			return Response.status(Response.Status.BAD_REQUEST).entity("Must insert destination").build();
		TransactionResult tr = DsTechShipping.createItem(itemId, dst);
		System.out.println("Log :: Rest :: Request to create item with id:" + itemId + " was received");
		if (tr.getStatus()) {
			return Response.ok(itemId + " Created succesfully", MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
		}
	}
	
	@DELETE
	@Path("/{itemId}")
	public Response deleteItem(@PathParam("itemId") String itemId) {
		TransactionResult tr = DsTechShipping.deleteSupplyChainObject(itemId);
		System.out.println("Log :: Rest :: Request to Delete item with id:" + itemId + " was received");
		if (tr.getStatus()) {
			return Response.ok(itemId + " Deleted", MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
		}
	}
	
	@POST
	@Path("/{itemId}/transactions")
	public Response itemTransaction(
			@PathParam("itemId") String itemId, 
			@QueryParam("action") String action,
			@DefaultValue("") @QueryParam("src") String src,
			@DefaultValue("") @QueryParam("dst") String dst) {
		if(action.equals("move"))
		{
			if(src.isEmpty() || dst.isEmpty())
			{
				return Response.status( Response.Status.BAD_REQUEST).entity("Illegal src or dst was entered").build(); 
			}
			
			TransactionResult tr = DsTechShipping.moveSupplyChainObject(itemId, src, dst);
			System.out.println("Log :: Rest :: Request to move item with id:" + itemId + " from: " + src + " to: " + dst + "was received");
			if (tr.getStatus()) {
				return Response.ok(itemId + " Moved from " + src + " to " + dst, MediaType.TEXT_PLAIN).build();
			} else {
				return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
			}
		}
		else
		{
			return Response.status( Response.Status.BAD_REQUEST).entity(action + " is illegal action (options: move)").build(); 
		}

	}
}
