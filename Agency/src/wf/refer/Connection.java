package wf.refer;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Vector;

import wf.refer.DynamicPortForwarder;

import wf.refer.LocalPortForwarder;

import android.util.Log;


public class Connection {
	String hostname;
	int port;
	
	public Connection(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	public synchronized LocalPortForwarder createLocalPortForwarder(
			int local_port, String host_to_connect, int port_to_connect) throws IOException{

			Log.e("local_port in CLPF", local_port+"");
			return new LocalPortForwarder(local_port, host_to_connect,
					port_to_connect);
	}
	
	public synchronized DynamicPortForwarder createDynamicPortForwarder(
			int local_port) throws IOException {
		return new DynamicPortForwarder(local_port);
	}
}
