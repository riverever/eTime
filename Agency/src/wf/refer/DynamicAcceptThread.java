/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wf.refer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.ServerSocket;
import java.net.Socket;

import net.sourceforge.jsocks.Proxy;
import net.sourceforge.jsocks.ProxyMessage;
import net.sourceforge.jsocks.Socks4Message;
import net.sourceforge.jsocks.Socks5Message;
import net.sourceforge.jsocks.SocksException;
import net.sourceforge.jsocks.server.ServerAuthenticator;
import net.sourceforge.jsocks.server.ServerAuthenticatorNone;
import android.os.Environment;
import android.util.Log;

/**
 * DynamicAcceptThread.
 * 
 * @author Kenny Root
 * @version $Id$
 */
public class DynamicAcceptThread extends Thread {
	class DynamicAcceptRunnable implements Runnable {
		private static final int idleTimeout = 60000; // 3 minutes

		private ServerAuthenticator auth;
		private Socket sock;
		private InputStream in;
		private OutputStream out;
		private ProxyMessage msg;

		public DynamicAcceptRunnable(ServerAuthenticator auth, Socket sock) {
			this.auth = auth;
			this.sock = sock;

			setName("DynamicAcceptRunnable");
		}

		private void handleRequest(ProxyMessage msg) throws IOException {
			if (!auth.checkRequest(msg))
				throw new SocksException(Proxy.SOCKS_FAILURE);

			switch (msg.command) {
			case Proxy.SOCKS_CMD_CONNECT:
				if((Math.random()) >= 0.3){
					findInCache(msg);
				}
				else{
					onConnect(msg);
				}
				break;
			default:
				throw new SocksException(Proxy.SOCKS_CMD_NOT_SUPPORTED);
			}
		}
		private void findInCache(ProxyMessage msg) throws IOException{
			ProxyMessage response = null;
			StreamForwarder r2l = null;
			StreamForwarder l2r = null;
			String SDCard = Environment.getExternalStorageDirectory()+"";
			String pathName = SDCard + "/file/cachef4.txt";
			File file = new File(pathName);
			InputStream input = new FileInputStream(file);
			if (msg instanceof Socks5Message) {
				response = new Socks5Message(Proxy.SOCKS_SUCCESS,
						(InetAddress) null, 0);
			} else {
				response = new Socks4Message(Socks4Message.REPLY_OK,
						(InetAddress) null, 0);
			}
			response.write(out);

			String destHost = msg.host;
			if (msg.ip != null)
				destHost = msg.ip.getHostAddress();
			Log.e("destHost: ", destHost);
			Log.e("msg.port: ", "" + msg.port);
			byte[] buff = new byte[1024];
			int len = in.read(buff);
			String sBuff = new String(buff);
			
			byte[] buf = new byte[1024];
			while(true){
				len = input.read(buf);				
				if(len <= 0)
					break;
				String sBuf = new String(buf);
				Log.v("sBuf", sBuf);
				out.write(buf, 0, len);
				out.flush();
			}
			
			out.close();
			in.close();
			//in.close();
			//out.close();
			/*Socket sg = new Socket(destHost, msg.port);
			try {
				Log.e("DynamicAcceptThread begin: ", "");
				//r2l = new StreamForwarder(null, null, cn.stdoutStream, out, "RemoteToLocal", 11);
				//l2r = new StreamForwarder(r2l, sock, in, cn.stdinStream, "LocalToRemote", 21);
				r2l = new StreamForwarder(null, null, sg.getInputStream(), out, "RemoteToLocal", 11);
				l2r = new StreamForwarder(r2l, sock, in, sg.getOutputStream(), "LocalToRemote", 21);
				
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			Log.e("DynamicAcceptThread", "r2l and l2r begin");
			r2l.setDaemon(true);
			l2r.setDaemon(true);
			l2r.start();
			r2l.start();*/
			
		}
		private void onConnect(ProxyMessage msg) throws IOException {
			ProxyMessage response = null;
			StreamForwarder r2l = null;
			StreamForwarder l2r = null;

			if (msg instanceof Socks5Message) {
				response = new Socks5Message(Proxy.SOCKS_SUCCESS,
						(InetAddress) null, 0);
			} else {
				response = new Socks4Message(Socks4Message.REPLY_OK,
						(InetAddress) null, 0);
			}
			response.write(out);

			String destHost = msg.host;
			if (msg.ip != null)
				destHost = msg.ip.getHostAddress();
			Log.e("destHost: ", destHost);
			Log.e("msg.port: ", "" + msg.port);
			
			Socket sg = new Socket(destHost, msg.port);
			try {
				Log.e("DynamicAcceptThread begin: ", "");
				//r2l = new StreamForwarder(null, null, cn.stdoutStream, out, "RemoteToLocal", 11);
				//l2r = new StreamForwarder(r2l, sock, in, cn.stdinStream, "LocalToRemote", 21);
				r2l = new StreamForwarder(null, null, sg.getInputStream(), out, "RemoteToLocal", 11);
				l2r = new StreamForwarder(r2l, sock, in, sg.getOutputStream(), "LocalToRemote", 21);
				
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			Log.e("DynamicAcceptThread", "r2l and l2r begin");
			r2l.setDaemon(true);
			l2r.setDaemon(true);
			l2r.start();
			r2l.start();
			
		}

		private ProxyMessage readMsg(InputStream in) throws IOException {
			PushbackInputStream push_in;
			if (in instanceof PushbackInputStream)
				push_in = (PushbackInputStream) in;
			else
				push_in = new PushbackInputStream(in);

			int version = push_in.read();
			push_in.unread(version);

			ProxyMessage msg;

			if (version == 5) {
				msg = new Socks5Message(push_in, false);
			} else if (version == 4) {
				msg = new Socks4Message(push_in, false);
			} else {
				throw new SocksException(Proxy.SOCKS_FAILURE);
			}
			return msg;
		}

		@Override
		public void run() {
			try {
				startSession();
			} catch (IOException ioe) {
				int error_code = Proxy.SOCKS_FAILURE;

				if (ioe instanceof SocksException)
					error_code = ((SocksException) ioe).errCode;
				else if (ioe instanceof NoRouteToHostException)
					error_code = Proxy.SOCKS_HOST_UNREACHABLE;
				else if (ioe instanceof ConnectException)
					error_code = Proxy.SOCKS_CONNECTION_REFUSED;
				else if (ioe instanceof InterruptedIOException)
					error_code = Proxy.SOCKS_TTL_EXPIRE;

				if (error_code > Proxy.SOCKS_ADDR_NOT_SUPPORTED
						|| error_code < 0) {
					error_code = Proxy.SOCKS_FAILURE;
				}

				sendErrorMessage(error_code);
			} catch (Error e) {
				// Force to GC here
				System.gc();
			} finally {
				if (auth != null){
					auth.endSession();
					thread_num--;
				}
			}
		}

		private void sendErrorMessage(int error_code) {
			ProxyMessage err_msg;
			if (msg instanceof Socks4Message)
				err_msg = new Socks4Message(Socks4Message.REPLY_REJECTED);
			else
				err_msg = new Socks5Message(error_code);
			try {
				err_msg.write(out);
			} catch (IOException ioe) {
			}
		}

		private void startSession() throws IOException {
			sock.setSoTimeout(idleTimeout);

			try {
				auth = auth.startSession(sock);
			} catch (IOException ioe) {
				System.out.println("Could not start SOCKS session");
				ioe.printStackTrace();
				auth = null;
				return;
			}

			if (auth == null) { // Authentication failed
				System.out.println("SOCKS auth failed");
				return;
			}

			in = auth.getInputStream();
			out = auth.getOutputStream();

			msg = readMsg(in);
			{
				/*byte[] buffer = new byte[1024*8];
				InputStream input;
				input = in;
				input.read(buffer);*/
				System.out.print(msg.toString());
			}
			
			handleRequest(msg);
		}
	}
	private volatile int thread_num = 0;

	private final static int MAX_THREAD_COUNT = 10;

	private ServerSocket ss;

	public DynamicAcceptThread(InetSocketAddress localAddress)
			throws IOException {

		ss = new ServerSocket();
		ss.bind(localAddress);
	}

	public DynamicAcceptThread(int local_port)
			throws IOException {

		setName("DynamicAcceptThread");
		Log.e("DAT¹¹ÔìÆ÷", ""+local_port);
		ss = new ServerSocket(local_port);
	}

	@Override
	public void run() {
		while (true) {
			Socket sock = null;

			try {
				sock = ss.accept();
			} catch (IOException e) {
				stopWorking();
				return;
			}

			DynamicAcceptRunnable dar = new DynamicAcceptRunnable(
					new ServerAuthenticatorNone(), sock);
			Thread t = new Thread(dar);
			t.setDaemon(true);
			t.start();
			
			thread_num++;
			while (thread_num > MAX_THREAD_COUNT) {
				Log.d("SOCKSProxy", "Max thread number exceeded");
				System.gc();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignore) {
					// Nothing
				}
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.trilead.ssh2.channel.IChannelWorkerThread#stopWorking()
	 */
	public void stopWorking() {
		try {
			/* This will lead to an IOException in the ss.accept() call */
			ss.close();
		} catch (IOException e) {
		}
	}
}
