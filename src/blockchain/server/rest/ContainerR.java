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
import blockchain.server.model.Container;
import blockchain.server.model.History;
import blockchain.server.model.QueryResult;
import blockchain.server.model.TransactionResult;

@Path("/containers")
public class ContainerR {	
	private Gson gson = new Gson();

	@GET
	@Path("/{containerId}")
	public Response getContainer(@PathParam("containerId") String containerId) {
		QueryResult qr = DsTechShipping.getContainerState(containerId);
		System.out.println("Log :: Rest :: Request for container with id:" + containerId + " was received");
		if (qr.getStatus()) {
			String containerStr = gson.toJson(qr.getRequestedObjects().get(0));
			return Response.ok(containerStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{containerId}/ship")
	public Response getContainersShip(@PathParam("containerId") String containerId) {
		QueryResult qr = DsTechShipping.getContainerState(containerId);
		System.out.println("Log :: Rest :: Request for ship of container with id:" + containerId + " was received");
		if (qr.getStatus()) {
			Container container = (Container)qr.getRequestedObjects().get(0);
			return Response.ok(container.getShip(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{containerId}/items")
	public Response getContainersItems(@PathParam("containerId") String containerId) {
		QueryResult qr = DsTechShipping.getContainerState(containerId);
		System.out.println("Log :: Rest :: Request for items of container with id:" + containerId + " was received");
		if (qr.getStatus()) {
			Container container = (Container)qr.getRequestedObjects().get(0);
			return Response.ok(String.join(",", container.getItems()), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	
	@GET
	@Path("/{containerId}/dock")
	public Response getContainersDock(@PathParam("containerId") String containerId) {
		QueryResult qr = DsTechShipping.getContainerState(containerId);
		System.out.println("Log :: Rest :: Request for dock of container with id:" + containerId + " was received");
		if (qr.getStatus()) {
			Container container = (Container)qr.getRequestedObjects().get(0);
			return Response.ok(container.getDoc(), MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@GET
	@Path("/{containerId}/history")
	public Response getContainersHistory(@PathParam("containerId") String containerId) {
		QueryResult qr = DsTechShipping.getContainerHist(containerId); 
		System.out.println("Log :: Rest :: Request for history container with id:" + containerId + " was received");
		if (qr.getStatus()) {
			History history = new History(qr.getRequestedObjects());
			String historyStr = gson.toJson(history);
			return Response.ok(historyStr, MediaType.APPLICATION_JSON).build();
		} else {
			return Response.status(qr.getErrorCode()).entity(qr.getMessage()).build();
		}
	}
	
	@PUT
	@Path("/{containerId}")
	public Response creteContainer(@PathParam("containerId") String containerId, @QueryParam("dst") String dst) {
		if(dst == null)
			return Response.status(Response.Status.BAD_REQUEST).entity("Must insert destination").build();
		TransactionResult tr = DsTechShipping.createContainer(containerId, dst);
		System.out.println("Log :: Rest :: Request to create container with id:" + containerId + " was received");
		if (tr.getStatus()) {
			return Response.ok(containerId + " Created succesfully", MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
		}
	}
	
	@DELETE
	@Path("/{containerId}")
	public Response deleteContainer(@PathParam("containerId") String containerId) {
		TransactionResult tr = DsTechShipping.deleteSupplyChainObject(containerId);
		System.out.println("Log :: Rest :: Request to Delete container with id:" + containerId + " was received");
		if (tr.getStatus()) {
			return Response.ok(containerId + " Deleted", MediaType.TEXT_PLAIN).build();
		} else {
			return Response.status(tr.getErrorCode()).entity(tr.getMessage()).build();
		}
	}
	
	@POST
	@Path("/{containerId}/transactions")
	public Response itemTransaction(
			@PathParam("containerId") String containerId, 
			@QueryParam("action") String action,
			@DefaultValue("") @QueryParam("src") String src,
			@DefaultValue("") @QueryParam("dst") String dst) {
		if(action.equals("move"))
		{
			if(src.isEmpty() || dst.isEmpty())
			{
				return Response.status( Response.Status.BAD_REQUEST).entity("Illegal src or dst was entered").build(); 
			}
			
			TransactionResult tr = DsTechShipping.moveSupplyChainObject(containerId, src, dst);
			System.out.println("Log :: Rest :: Request to move container with id:" + containerId + " from: " + src + " to: " + dst + "was received");
			if (tr.getStatus()) {
				return Response.ok(containerId + " Moved from " + src + " to " + dst, MediaType.TEXT_PLAIN).build();
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
