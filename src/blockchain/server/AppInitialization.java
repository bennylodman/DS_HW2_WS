package blockchain.server;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import blockchain.server.group.GroupServers;

@ApplicationPath("/")
public class AppInitialization extends ResourceConfig {

	public AppInitialization() {
		DsTechShipping.groupServers = new GroupServers(DsTechShipping.view);
		new ServerThread().start();
	}
}
