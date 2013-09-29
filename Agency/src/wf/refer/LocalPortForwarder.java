package wf.refer;

import java.io.IOException;
import java.net.InetSocketAddress;

import wf.refer.LocalAcceptThread;

/**
 * A <code>LocalPortForwarder</code> forwards TCP/IP connections to a local port
 * via the secure tunnel to another host (which may or may not be identical to
 * the remote SSH-2 server). Checkout
 * {@link Connection#createLocalPortForwarder(int, String, int)} on how to
 * create one.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: LocalPortForwarder.java,v 1.1 2007/10/15 12:49:56 cplattne Exp
 *          $
 */
public class LocalPortForwarder {


	String host_to_connect;

	int port_to_connect;

	LocalAcceptThread lat;

	LocalPortForwarder(InetSocketAddress addr,
			String host_to_connect, int port_to_connect) throws IOException {
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(addr, host_to_connect, port_to_connect);
		lat.setDaemon(true);
		lat.start();
	}

	LocalPortForwarder(int local_port,
			String host_to_connect, int port_to_connect) throws IOException {
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(local_port, host_to_connect,
				port_to_connect);
		lat.setDaemon(true);
		lat.start();
	}

	/**
	 * Stop TCP/IP forwarding of newly arriving connections.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		lat.stopWorking();
	}
}
