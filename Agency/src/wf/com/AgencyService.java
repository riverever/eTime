package wf.com;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;


import wf.com.DNSServer;
import wf.com.AgencyActivity;


import wf.utils.Constraints;

import com.flurry.android.FlurryAgent;



import wf.refer.DynamicPortForwarder;
import wf.refer.LocalPortForwarder;

import wf.refer.Connection;
import wf.ui.ProxyedApp;
import wf.com.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.res.AssetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class AgencyService extends Service {
	private final static String TAG = "AgencyService";
	protected static final String BASE = "/data/data/wf.com/";
	private Connection connection;
	private Intent intent;
	// Port forwarding
	private LocalPortForwarder lpf = null;
	private LocalPortForwarder dnspf = null;
	private DynamicPortForwarder dpf = null;
	private boolean isSocks = true;
	private boolean hasRedirectSupport;
	private DNSServer dnsServer;
	private boolean connected;
	private boolean isConnecting;
	private boolean enableDNSProxy;
	private int dnsPort;
	private ProxyedApp apps[] = null;
	private String tordAppString;
	private Notification notification;
	private SharedPreferences settings = null;
	private PendingIntent pendIntent;
	private NotificationManager notificationManager;
	
	final static String CMD_IPTABLES_REDIRECT_ADD = BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp --dport 80 -j REDIRECT --to 8123\n"
			+ BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp --dport 443 -j REDIRECT --to 8124\n";

	final static String CMD_IPTABLES_DNAT_ADD = BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp --dport 80 -j DNAT --to-destination 127.0.0.1:8123\n"
			+ BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp --dport 443 -j DNAT --to-destination 127.0.0.1:8124\n";

	final static String CMD_IPTABLES_REDIRECT_ADD_SOCKS = BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp -j REDIRECT --to 8123\n";

	final static String CMD_IPTABLES_DNAT_ADD_SOCKS = BASE
			+ "iptables -t nat -A MIDDLEWARE -p tcp -j DNAT --to-destination 127.0.0.1:8123\n";

	final static String CMD_IPTABLES_RETURN = BASE
			+ "iptables -t nat -A OUTPUT -p tcp -d 0.0.0.0 -j RETURN\n";

	public static boolean runCommand(String command) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}

	public static boolean runRootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			Log.e("runRootCommand in T: ", command);
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}
		return true;
	}
	
	public boolean enablePortForward() {

		/*
		 * DynamicPortForwarder dpf = null;
		 * 
		 * try { dpf = connection.createDynamicPortForwarder(new
		 * InetSocketAddress( InetAddress.getLocalHost(), 1984)); } catch
		 * (Exception e) { Log.e(TAG, "Could not create dynamic port forward",
		 * e); return false; }
		 */

		// LocalPortForwarder lpf1 = null;
		try {

			dnspf = connection.createLocalPortForwarder(8053, "www.google.com", 80);

			if (isSocks) {
				dpf = connection.createDynamicPortForwarder(1985);
				Log.e("dpf: ", "动态端口转发");
			} else {
				Log.e("local port:", "profile.getLocalPort");

				Log.e("lpf: ", "本地端口转发");
			}

		} catch (Exception e) {
			Log.e(TAG, "Could not create local port forward", e);
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	private void finishConnection() {

		Log.e(TAG, "Forward Successful");

		if (isSocks)
			runRootCommand(BASE + "proxy_socks.sh start "
					+ 1985);
		else
			runRootCommand(BASE + "proxy_http.sh start "
					+ 1985);



		StringBuffer cmd = new StringBuffer();
        cmd.append("/data/data/wf.com/iptables -t nat -N MIDDLEWARE\n");
        cmd.append("/data/data/wf.com/iptables -t nat -F MIDDLEWARE\n");
        cmd.append("/data/data/wf.com/iptables -t nat -N MIDDLEWAREDNS\n");
        cmd.append("/data/data/wf.com/iptables -t nat -F MIDDLEWAREDNS\n");
        //cmd.append("/data/data/wf.com/iptables -t nat -A MIDDLEWAREDNS -p udp --dport 53 -j REDIRECT --to " + dnsPort + "\n");
        cmd.append("/data/data/wf.com/iptables -t nat -A OUTPUT -p udp -j MIDDLEWAREDNS\n");
        cmd.append("/data/data/wf.com/iptables -t nat -A MIDDLEWARE -p tcp -j REDIRECT --to 8123\n");
        //cmd.append("/data/data/wf.com/iptables -t nat -A OUTPUT -p tcp -d 127.0.0.1 -j RETURN\n");
        //cmd.append("/data/data/wf.com/iptables -t nat -m owner --uid-owner 10043 -A OUTPUT -p tcp -j MIDDLEWARE\n");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		tordAppString = prefs.getString(Constraints.PROXYED_APPS, "");
		if (apps == null || apps.length <= 0)
			apps = AppManager
					.getProxyedApps(this, tordAppString);

		for (int i = 0; i < apps.length; i++) {
			if (apps[i].isProxyed()) {
				cmd.append(BASE + "iptables "
						+ "-t nat -m owner --uid-owner " + apps[i].getUid()
						+ " -A OUTPUT -p tcp -j MIDDLEWARE\n");
				
			}
		}
        //cmd.append("/data/data/wf.com/iptables -t nat -m owner --uid-owner 10047 -A OUTPUT -p tcp -j MIDDLEWARE\n");
        //cmd.append("/data/data/wf.com/iptables -t nat -m owner --uid-owner 10049 -A OUTPUT -p tcp -j MIDDLEWARE\n");
        //cmd.append("/data/data/wf.com/iptables -t nat -m owner --uid-owner 10038 -A OUTPUT -p tcp -j MIDDLEWARE\n");
        String rules = cmd.toString();


		if (isSocks)
			runRootCommand(rules.replace("8124", "8123"));
		else
			runRootCommand(rules);
		Log.e("Run rules", rules);

	}

	private void flushIptables() {

		StringBuffer cmd = new StringBuffer();

		cmd.append(BASE + "iptables -t nat -F MIDDLEWARE\n");
		cmd.append(BASE + "iptables -t nat -X MIDDLEWARE\n");

		cmd.append((CMD_IPTABLES_RETURN.replace("0.0.0.0", "127.0.0.1"))
				.replace("-A", "-D"));


		cmd.append(BASE + "iptables -t nat -F MIDDLEWAREDNS\n");
		cmd.append(BASE + "iptables -t nat -X MIDDLEWAREDNS\n");
		cmd.append(BASE+ "iptables -t nat -D OUTPUT -p udp -j MIDDLEWAREDNS\n");
		if (apps == null || apps.length <= 0)
			apps = AppManager
					.getProxyedApps(this, tordAppString);

		for (int i = 0; i < apps.length; i++) {
			if (apps[i].isProxyed()) {
				cmd.append(BASE + "iptables "
						+ "-t nat -m owner --uid-owner " + apps[i].getUid()
						+ " -D OUTPUT -p tcp -j MIDDLEWARE\n");
			}
		}


		String rules = cmd.toString();

		runRootCommand(rules);

		if (isSocks)
			runRootCommand(BASE + "proxy_socks.sh stop");
		else
			runRootCommand(BASE + "proxy_http.sh stop");

	}

	private void initHasRedirectSupported() {
		Process process = null;
		DataOutputStream os = null;
		DataInputStream es = null;

		String command;
		String line = null;

		command = BASE
				+ "iptables -t nat -A OUTPUT -p udp --dport 54 -j REDIRECT --to 8154";

		try {
			process = Runtime.getRuntime().exec("su");
			es = new DataInputStream(process.getErrorStream());
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();

			while (null != (line = es.readLine())) {
				Log.d("mark", "line input");
				Log.d(TAG, line);
				if (line.contains("No chain/target/match")) {
					this.hasRedirectSupport = false;
					break;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (es != null)
					es.close();
				process.destroy();
			} catch (Exception e) {
				// nothing
			}
		}

		// flush the check command
		runRootCommand(command.replace("-A", "-D"));
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		notificationManager = (NotificationManager) this
				.getSystemService(NOTIFICATION_SERVICE);
		intent = new Intent(this, AgencyActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		pendIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification = new Notification();
		

	}

	/** Called when the activity is closed. */
	@Override
	public void onDestroy() {


		FlurryAgent.onEndSession(this);
		if (connected){
			notifyAlert(getString(R.string.forward_stop), getString(R.string.service_stopped),
				Notification.FLAG_AUTO_CANCEL);
		}
		if (enableDNSProxy) {
			try {
				if (dnsServer != null) {
					dnsServer.close();
					dnsServer = null;
				}
			} catch (Exception e) {
				Log.e(TAG, "DNS Server close unexpected");
			}
		}

		new Thread() {
			private boolean isStopping;

			@Override
			public void run() {

				// Make sure the connection is closed, important here
				onDisconnect();

				isStopping = false;


			}
		}.start();

		flushIptables();

		super.onDestroy();

		markServiceStopped();
	}

	private void onDisconnect() {
		connected = false;

		try {
			if (lpf != null) {
				lpf.close();
				lpf = null;
			}
		} catch (IOException ignore) {
			// Nothing
		}
		try {
			if (dpf != null) {
				dpf.close();
				dpf = null;
			}
		} catch (IOException ignore) {
			// Nothing
		}
		try {
			if (dnspf != null) {
				dnspf.close();
				dnspf = null;
			}
		} catch (IOException ignore) {
			// Nothing
		}


	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);

		FlurryAgent.onStartSession(this, "MBY4JL18FQK1DPEJ5Y39");

		Log.d(TAG, "Service Start");

		Bundle bundle = intent.getExtras();
		int id = bundle.getInt(Constraints.ID);


		new Thread(new Runnable() {


			@Override
			public void run() {

				isConnecting = true;

				enableDNSProxy = true;

				try {
					URL url = new URL("http://gae-ip-country.appspot.com/");
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setConnectTimeout(2000);
					conn.setReadTimeout(5000);
					conn.connect();
					InputStream is = conn.getInputStream();
					BufferedReader input = new BufferedReader(
							new InputStreamReader(is));
					String code = input.readLine();
					if (code != null && code.length() > 0 && code.length() < 3) {
						Log.d(TAG, "Location: " + code);
						if (!code.contains("CN") && !code.contains("ZZ"))
							enableDNSProxy = false;
					}
				} catch (Exception e) {
					Log.d(TAG, "Cannot get country code");
					// Nothing
				}

				if (enableDNSProxy) {
					if (dnsServer == null) {
						//dnsServer = new DNSServer("DNS Server", "8.8.4.4",53, AgencyService.this);
						dnsServer = new DNSServer("DNS Server", "127.0.0.1", 8053, AgencyService.this);
						dnsServer.setBasePath("/data/data/wf.com");
						dnsPort = dnsServer.init();
					}
				}

				// Test for Redirect Support
				initHasRedirectSupported();

				if (connect()) {

					// Connection and forward successful
					finishConnection();

					if (enableDNSProxy) {
						// Start DNS Proxy

						Thread dnsThread = new Thread(dnsServer);
						dnsThread.setDaemon(true);
						dnsThread.start();
					}
					notifyAlert(getString(R.string.forward_success),
							getString(R.string.service_running), Notification.FLAG_ONGOING_EVENT);
					// for widget, maybe exception here


				} else {

					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignore) {
						// Nothing
					}

					connected = false;
					stopSelf();
				}

				isConnecting = false;
			}
		}).start();

		markServiceStarted();
	}

	
	private void notifyAlert(String title, String info, int flags) {
		notification.tickerText = title;
		notification.flags = flags;
		initSoundVibrateLights(notification);
		notification.setLatestEventInfo(this, getString(R.string.app_name), info, pendIntent);
		notificationManager.cancel(0);
		notificationManager.notify(0, notification);
	}

	private void initSoundVibrateLights(Notification notification) {
		final String ringtone = settings.getString(
				"settings_key_notif_ringtone", null);
		AudioManager audioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
			notification.sound = null;
		} else if (ringtone != null)
			notification.sound = Uri.parse(ringtone);
		else
			notification.defaults |= Notification.DEFAULT_SOUND;

		if (settings.getBoolean("settings_key_notif_icon", true)) {
			notification.icon = R.drawable.ic_stat;
		} else {
			notification.icon = R.drawable.ic_stat_trans;
		}

		if (settings.getBoolean("settings_key_notif_vibrate", false)) {
			long[] vibrate = { 0, 1000, 500, 1000, 500, 1000 };
			notification.vibrate = vibrate;
		}

		notification.defaults |= Notification.DEFAULT_LIGHTS;
		
	}

	protected boolean connect() {
		// TODO Auto-generated method stub
		connection = new Connection("",22);
		enablePortForward();
		connected = true;
		return true;
	}
	
	private static WeakReference<AgencyService> sRunningInstance = null;
	
	public final static boolean isServiceStarted() {
		final boolean isServiceStarted;
		if (sRunningInstance == null) {
			isServiceStarted = false;
		} else if (sRunningInstance.get() == null) {
			isServiceStarted = false;
			sRunningInstance = null;
		} else {
			isServiceStarted = true;
		}
		return isServiceStarted;
	}
	
	
	private void markServiceStarted() {
		sRunningInstance = new WeakReference<AgencyService>(this);
	}

	private void markServiceStopped() {
		sRunningInstance = null;
	}
	
	
	
}
