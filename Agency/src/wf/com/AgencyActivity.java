package wf.com;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;


import com.flurry.android.FlurryAgent;
import wf.com.AppManager;

import wf.com.AgencyService;
import wf.com.R;
import wf.db.Profile;
import wf.utils.Utils;
import wf.utils.Constraints;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AgencyActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{
    /** Called when the activity is first created. */
    Button threadBegin;
    Button loadFile;
    TextView threadInfo;
    TextView showData;

    String fileN = "/sdcard/agency/packet.txt";
	private boolean isRoot = false;
	
	private Preference proxyedApps;
	private CheckBoxPreference isRunningCheck;
	private CheckBoxPreference isAutoSetProxyCheck;
	private CheckBoxPreference isDNSProxyCheck;
	private CheckBoxPreference isAutoReconnectCheck;
	private CheckBoxPreference isAutoConnectCheck;
	
	private final static String TAG = "AgencyActivity";
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.main_pre);
		proxyedApps = (Preference) findPreference("proxyedApps");
		isRunningCheck = (CheckBoxPreference) findPreference("isRunning");
		isAutoSetProxyCheck = (CheckBoxPreference) findPreference("isAutoSetProxy");
		isAutoConnectCheck = (CheckBoxPreference) findPreference("isAutoConnect");
		isAutoReconnectCheck = (CheckBoxPreference) findPreference("isAutoReconnect");
		isDNSProxyCheck = (CheckBoxPreference) findPreference("isDNSProxy");
		loadNetworkList();

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		Editor edit = settings.edit();
		
		if (Utils.isWorked()) {
			edit.putBoolean("isRunning", true);
		} else {
			if (settings.getBoolean("isRunning", false)) {
				// showAToast(getString(R.string.crash_alert));
				recovery();
			}
			edit.putBoolean("isRunning", false);
		}
		
		edit.commit();
		
		if (settings.getBoolean("isRunning", false)) {
			isRunningCheck.setChecked(true);
			disableAll();
		} else {
			isRunningCheck.setChecked(false);
			enableAll();
		}
        
        
    	if (!detectRoot()) {
			isRoot  = false;
		} else {
			isRoot = true;
			new Thread() {
				@Override
				public void run() {

					CopyAssets();
					runCommand("chmod 755 /data/data/wf.com/iptables");
					runCommand("chmod 755 /data/data/wf.com/redsocks");
					runCommand("chmod 755 /data/data/wf.com/proxy_http.sh");
					runCommand("chmod 755 /data/data/wf.com/proxy_socks.sh");

				}
			}.start();
		}
    }
	
	
	private void loadNetworkList() {
		// TODO Auto-generated method stub
		
	}

	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		if (preference.getKey() != null
				&& preference.getKey().equals("proxyedApps")) {
			Intent intent = new Intent(this, AppManager.class);
			startActivity(intent);
		} else if (preference.getKey() != null
				&& preference.getKey().equals("isRunning")) {
			if (!serviceStart()) {

				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(AgencyActivity.this);

				Editor edit = settings.edit();

				edit.putBoolean("isRunning", false);

				edit.commit();

				enableAll();
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	private boolean serviceStart() {
		// TODO Auto-generated method stub
		if (Utils.isWorked()) {

			try {
				stopService(new Intent(AgencyActivity.this, AgencyService.class));
			} catch (Exception e) {
				// Nothing
			}

			return false;
		}
		try{
			Intent it = new Intent(AgencyActivity.this, AgencyService.class);
			Bundle bundle = new Bundle();
			bundle.putInt(Constraints.ID, 10000);
			it.putExtras(bundle);
			startService(it);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		/*
		 * add()方法的四个参数，依次是： 1、组别，如果不分组的话就写Menu.NONE,
		 * 2、Id，这个很重要，Android根据这个Id来确定不同的菜单 3、顺序，那个菜单现在在前面由这个参数的大小决定
		 * 4、文本，菜单的显示文本
		 */
		menu.add(Menu.NONE, Menu.FIRST + 3, 1, getString(R.string.about))
				.setIcon(android.R.drawable.ic_menu_info_details);
		// return true才会起作用
		return true;

	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST + 3:
			String versionName = "";
			try {
				versionName = getPackageManager().getPackageInfo(
						getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				versionName = "";
			}
			showAToast(getString(R.string.about) + " (" + versionName + ")"
					+ getString(R.string.copy_rights));
			break;
		}

		return true;
	}
	
	private void showAToast(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msg)
				.setCancelable(false)
				.setNegativeButton(getString(R.string.ok_iknow),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	
	private void enableAll() {
		isRunningCheck.setEnabled(true);
		isAutoConnectCheck.setEnabled(true);
		isAutoReconnectCheck.setEnabled(true);
		isDNSProxyCheck.setEnabled(true);
		isAutoSetProxyCheck.setEnabled(true);
		if (!isAutoSetProxyCheck.isChecked())
			proxyedApps.setEnabled(true);
	}
	
	private void disableAll() {
		proxyedApps.setEnabled(false);
		isAutoSetProxyCheck.setEnabled(false);
		isAutoConnectCheck.setEnabled(false);
		isAutoReconnectCheck.setEnabled(false);
		isDNSProxyCheck.setEnabled(false);
	}
	
	
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
	
	private void CopyAssets() {
		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		for (int i = 0; i < files.length; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {
				// if (!(new File("/data/data/org.sshtunnel/" +
				// files[i])).exists()) {
				if((!files[i].equals("images")) && (!files[i].equals("sounds")) && (!files[i].equals("webkit"))){
					in = assetManager.open(files[i]);
					out = new FileOutputStream("/data/data/wf.com/"
						+ files[i]);
					copyFile(in, out);
					in.close();
					in = null;
					out.flush();
					out.close();
					out = null;
				}
			} catch (Exception e) {
				Log.e(TAG, "Assets error", e);
			}
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}
	
	public boolean detectRoot() {
		try {
			Process proc = Runtime.getRuntime().exec("su");
			if (proc == null)
				return false;
			proc.destroy();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	

	protected void loadStart() {
		// TODO Auto-generated method stub
		RandomAccessFile rf;
		try {
			rf = new RandomAccessFile(fileN, "rw");
			rf.seek(0);
			byte [] buffer2 = new byte[1024 * 8];
			rf.read(buffer2);
			String Sbuffer = new String(buffer2);
			showData.setText(Sbuffer);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
		.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onResume() {
		
		super.onResume();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (settings.getBoolean("isRunning", false)) {
			isRunningCheck.setChecked(true);
			disableAll();
		} else {
			isRunningCheck.setChecked(false);
			enableAll();
		}
		
		
		Editor edit = settings.edit();

		if (Utils.isWorked()) {
			if (settings.getBoolean("isConnecting", false))
				isRunningCheck.setEnabled(false);
			edit.putBoolean("isRunning", true);
		} else {
			if (settings.getBoolean("isRunning", false)) {
				showAToast(getString(R.string.crash_alert));
				recovery();
			}
			edit.putBoolean("isRunning", false);
		}

		edit.commit();
		getPreferenceScreen().getSharedPreferences()
		.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "MBY4JL18FQK1DPEJ5Y39");
	}

	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}
	
	private void recovery() {

		new Thread() {
			@Override
			public void run() {

				try {
					stopService(new Intent(AgencyActivity.this,
							AgencyService.class));
				} catch (Exception e) {
					// Nothing
				}

				try {
					File cache = new File(AgencyService.BASE
							+ "cache/dnscache");
					if (cache.exists())
						cache.delete();
				} catch (Exception ignore) {
					// Nothing
				}

				AgencyService.runRootCommand(AgencyService.BASE
						+ "iptables -t nat -F OUTPUT");

				AgencyService.runRootCommand(AgencyService.BASE + "proxy_http.sh stop");
			}
		}.start();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
		if (key.equals("isRunning")) {
			if (settings.getBoolean("isRunning", false)) {
				disableAll();
				isRunningCheck.setChecked(true);
			} else {
				enableAll();
				isRunningCheck.setChecked(false);
			}
		}
		
		if (key.equals("isAutoSetProxy")) {
			if (settings.getBoolean("isAutoSetProxy", false)) {
				isAutoSetProxyCheck.setChecked(true);
				proxyedApps.setEnabled(false);
			} 
			else{
				isAutoSetProxyCheck.setChecked(false);
				proxyedApps.setEnabled(true);
			}
			
		}
	}
	

}