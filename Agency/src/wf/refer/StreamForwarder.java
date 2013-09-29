package wf.refer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.net.Socket;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;

/**
 * A StreamForwarder forwards data between two given streams. If two
 * StreamForwarder threads are used (one for each direction) then one can be
 * configured to shutdown the underlying channel/socket if both threads have
 * finished forwarding (EOF).
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: StreamForwarder.java,v 1.1 2007/10/15 12:49:56 cplattne Exp $
 */
public class StreamForwarder extends Thread {
	OutputStream os;
	InputStream is;
	
	private static String encoding = "GBK";
	StreamForwarder sibling;
	Socket s;
	String mode;
	int invokeResource;

	StreamForwarder(StreamForwarder sibling, Socket s,
			InputStream is, OutputStream os, String mode, int invokeResource) throws IOException {
		this.is = is;
		this.os = os;
		this.mode = mode;

		this.sibling = sibling;
		this.s = s;
		this.invokeResource = invokeResource;
	}
	private static String readLine(InputStream is, int contentLe) throws IOException {
		ArrayList lineByteList = new ArrayList();
		byte readByte;
		int total = 0;
		if (contentLe != 0) {
			do {
				readByte = (byte) is.read();
				lineByteList.add(Byte.valueOf(readByte));
				total++;
			} while (total < contentLe);//消息体读还未读完
		} else {
			do {
				readByte = (byte) is.read();
				if(readByte == -1)
					break;
				lineByteList.add(Byte.valueOf(readByte));
			} while (readByte != 10);
		}

		byte[] tmpByteArr = new byte[lineByteList.size()];
		for (int i = 0; i < lineByteList.size(); i++) {
			tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();
		}
		lineByteList.clear();

		return new String(tmpByteArr, encoding);
	}
	
	String SDCard = Environment.getExternalStorageDirectory()+"";
	String pathName = SDCard + "/file/replyCathe";
	String pathRequestName = SDCard + "/file/requestCache";
	OutputStream oput;
	OutputStream oputR;
	
	@Override
	public void run() {
		File fileR = new File(pathRequestName);
		File file = new File(pathName);
		try {
			oput = new FileOutputStream(file, true);
			oputR = new FileOutputStream(fileR, true);
			int i = 0;
			while (true) {
				i++;
				if(invokeResource == 11 || invokeResource == 21 ){
					Log.e("Resource is ", "from Dynamic" + i);
				}else if(invokeResource == 10 || invokeResource == 20){
					Log.e("Resource is ", "from Local");
				}else if(invokeResource == 12 || invokeResource == 22){
					Log.e("Resource is", "from RAT");
				}else if(invokeResource == 13 || invokeResource == 23){
					Log.e("Resource is", "From RX11");
				}
				if(mode.equals("LocalToRemote")){
					Log.d("Direction:", "Local to remote,request" + i);
					byte[] buffer = new byte[1024*1];
					int len = is.read(buffer);
					Log.d("buffer: ", "length:"+len);
					if (len <= 0){
						String s = "\r\n=====================NEW REQUEST END=========================\r\n\r\n";
						oputR.write(s.getBytes(), 0 ,s.length());
						oputR.flush();
						break;
					}
					if (len > 0){
							//StringBuffer sb = new StringBuffer();
							//sb.append("GET /status/weibo_queue.php HTTP/1.1\r\n");
							//sb.append("Host: weiboagency3.sinaapp.com\r\n");
							//sb.append("Connection: Keep-Alive\r\n");
							//sb.append("User-Agent: Apache-HttpClient/UNAVAILABLE (java 1.4)\r\n");
							//注，这是关键的关键，忘了这里让我搞了半个小时。这里一定要一个回车换行，表示消息头完，不然服务器会等待
							//sb.append("\r\n");
							//buffer = sb.toString().getBytes();
							String Sbuffer = new String(buffer);
							Log.e("LTOR:" + i + "Sbuffer: " , Sbuffer);	
							oputR.write(buffer, 0 , len);
							oputR.flush();
							os.write(buffer, 0, len);
							os.flush();
					}
				}
				else{
					if(mode.equals("RemoteToLocal")){
						Log.d("Direction:", "remote to local,reply" + i);
						byte[] buffer = new byte[1024*4];
						int len = is.read(buffer);
						Log.d("buffer: ", "length:"+len);
						if (len <= 0){
							String s = "\r\n=====================NEW REPLY END!!!!=========================\r\n";
							oput.write(s.getBytes(), 0 ,s.length());
							oput.flush();
							break;
						}
						if (len > 0){
								//StringBuffer sb = new StringBuffer();
								//sb.append("GET /status/weibo_queue.php HTTP/1.1\r\n");
								//sb.append("Host: weiboagency3.sinaapp.com\r\n");
								//sb.append("Connection: Keep-Alive\r\n");
								//sb.append("User-Agent: Apache-HttpClient/UNAVAILABLE (java 1.4)\r\n");
								//注，这是关键的关键，忘了这里让我搞了半个小时。这里一定要一个回车换行，表示消息头完，不然服务器会等待
								//sb.append("\r\n");
								//buffer = sb.toString().getBytes();
								String Sbuffer = new String(buffer);
								Log.e("RToL:" + i + "Sbuffer: ", Sbuffer);
								oput.write(buffer, 0 , len);
								oput.flush();
								os.write(buffer, 0, len);
								os.flush();
						}
					}	
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			try {
				os.close();
				oput.close();
				oputR.close();
			} catch (IOException e1) {
			}
			try {
				is.close();
			} catch (IOException e2) {
			}

			if (sibling != null) {
				while (sibling.isAlive()) {
					try {
						sibling.join();
					} catch (InterruptedException e) {
					}
				}

				try {
					if (s != null)
						s.close();
				} catch (IOException e1) {
				}
			}
		}
	}
}