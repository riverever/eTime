package wf.refer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * LocalAcceptThread.
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: LocalAcceptThread.java,v 1.1 2007/10/15 12:49:56 cplattne Exp $
 */
public class LocalAcceptThread extends Thread{
	String host_to_connect;
	int port_to_connect;
	Socket sg;
	final ServerSocket ss;

	public LocalAcceptThread(InetSocketAddress localAddress,
			String host_to_connect, int port_to_connect) throws IOException {

		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket();
		ss.bind(localAddress);
	}

	public LocalAcceptThread(int local_port,
			String host_to_connect, int port_to_connect) throws IOException {

		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		ss = new ServerSocket(local_port);
		Log.e("SSocket local Port", local_port +"");
	}

	@Override
	public void run() {
		while (true) {
			Socket s = null;
			Log.i("Mark", "LocalAccept");
			try {
				s = ss.accept();
			} catch (IOException e) {
				stopWorking();
				return;
			}

			try {
				sg = new Socket("46.82.174.68", 8053);
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			StreamForwarder r2l = null;
			StreamForwarder l2r = null;
			try {
				
				Log.e("Port: ", s.getPort()+"");
				
				//r2l = new StreamForwarder(null, null, cn.stdoutStream,
				//		s.getOutputStream(), "RemoteToLocal", 10);
				//l2r = new StreamForwarder(r2l, s, s.getInputStream(),
				//		cn.stdinStream, "LocalToRemote", 20);
				l2r = new StreamForwarder(r2l, s, s.getInputStream(),
						sg.getOutputStream(), "LocalToRemote", 20);
				r2l = new StreamForwarder(null, null, sg.getInputStream(),
						s.getOutputStream(), "RemoteToLocal", 10);
				
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}

			r2l.setDaemon(true);
			l2r.setDaemon(true);
			r2l.start();
			l2r.start();
		}
	}

	public void stopWorking() {
		try {
			/* This will lead to an IOException in the ss.accept() call */
			ss.close();
		} catch (IOException e) {
		}
	}
}
