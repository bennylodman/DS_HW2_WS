package blockchain.server.rest;

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
import blockchain.server.model.QueryResult;
import blockchain.server.model.Ship;
import blockchain.server.model.TransactionResult;

public class ShipR {
	
	private Gson gson = new Gson();

	@GET
	@Path("/{shipId}")
	public Response getShip(@PathParam("shipId") String shipId) {
		QueryResult qr = DsTechShipping.getShipState(shipId);
		if (qr.getStatus()) {
			String shipStr = gson.toJson(qr.getRequestedObjects().get(0));
			return Response.ok(shipStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{shipId}/dock")
	public Response getShipsDock(@PathParam("shipId") String shipId) {
		QueryResult qr = DsTechShipping.getShipState(shipId);
		if (qr.getStatus()) {
			Ship ship = (Ship)qr.getRequestedObjects().get(0);
			return Response.ok(ship.getDoc(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{shipId}/containers")
	public Response getShipsContainers(@PathParam("shipId") String shipId) {
		QueryResult qr = DsTechShipping.getShipState(shipId);
		if (qr.getStatus()) {
			Ship ship = (Ship)qr.getRequestedObjects().get(0);
			return Response.ok(String.join(",", ship.getContainers()), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{shipId}/history")
	public Response getShipsHistory(@PathParam("shipId") String shipId) {
		QueryResult qr = DsTechShipping.getShipState(shipId);
		if (qr.getStatus()) {
			History history = new History(qr.getRequestedObjects());
			String historyStr = gson.toJson(history);
			return Response.ok(historyStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@PUT
	@Path("/{shipId}/create")
	public Response creteShip(@PathParam("shipId") String shipId, @QueryParam("dst") String dst) {
		TransactionResult tr = DsTechShipping.createShip(shipId, dst);
		if (tr.getStatus()) {
			return Response.ok(shipId + " Created succesfully", MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
		}
	}
	
	@POST
	@Path("/{shipId}/transactions")
	public Response creteItem(
			@PathParam("shipId") String shipId, 
			@QueryParam("action") String action,
			@DefaultValue("") @QueryParam("src") String src,
			@DefaultValue("") @QueryParam("dst") String dst) {
		if(action.equals("move"))
		{
			if(src.isEmpty() || dst.isEmpty())
			{
				return Response.status( Response.Status.BAD_REQUEST).entity("Illegal src or dst was entered").build(); 
			}
			
			TransactionResult tr = DsTechShipping.moveSupplyChainObject(shipId, src, dst);
			if (tr.getStatus()) {
				return Response.ok(shipId + " Moved from " + src + " to " + dst, MediaType.TEXT_PLAIN).build();
			} else {
				return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
			}
		}
		if(action.equals("delete"))
		{
			TransactionResult tr = DsTechShipping.deleteSupplyChainObject(shipId);
			if (tr.getStatus()) {
				return Response.ok(shipId + " Deleted", MediaType.TEXT_PLAIN).build();
			} else {
				return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
			}
		}
		else
		{
			return Response.status( Response.Status.BAD_REQUEST).entity(action + " is illegal action (options: move / delete)").build(); 
		}

	}
	

}
