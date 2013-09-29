package lightsns.com;

import java.text.SimpleDateFormat;
import java.util.List;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;

import com.weibo.sdk.android.api.*;
import com.weibo.sdk.android.net.RequestListener;
import com.weibo.sdk.android.sso.SsoHandler;
import com.weibo.sdk.android.util.Utility;

import lightsns.db.AccessTokenKeeper;
import lightsns.db.DataHelper;
import lightsns.model.UserInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {
	
	public static Oauth2AccessToken accessToken ;
	public static final String TAG = "LightSNS";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		DataHelper dbHelper=new DataHelper(this);
	    
	    Start(dbHelper);
	}
	

	public void Start(final DataHelper db) {
		new Thread() {
//			List<UserInfo> userList= db.GetUserList(true);
			public void run() {
				Looper.prepare(); 
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Log.e(TAG, "test1");
				MainActivity.accessToken=AccessTokenKeeper.readAccessToken(MainActivity.this);
				
				if (MainActivity.accessToken.isSessionValid()) {
					String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date(MainActivity.accessToken.getExpiresTime()));
					Log.e(TAG, "test2");
					Util.showToast(MainActivity.this, "认证成功 有效期至："+date);
					Log.e(TAG, "test3");
					Intent intent = new Intent();
					Log.e(TAG, "test4");
					intent.setClass(MainActivity.this, HomeActivity.class);
					Log.e(TAG, "test5");
					startActivity(intent);
					db.Close();
					finish();
				}
				else{
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, AuthorizeActivity.class);
					startActivity(intent);
					db.Close();
					finish();
					}

			}
		}.start();
	}

}

