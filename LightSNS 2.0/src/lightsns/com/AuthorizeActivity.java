package lightsns.com;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;

import android.content.Intent;

import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import lightsns.db.AccessTokenKeeper;

import com.weibo.sdk.android.api.*;
import com.weibo.sdk.android.net.RequestListener;
import com.weibo.sdk.android.sso.SsoHandler;
import com.weibo.sdk.android.util.Utility;

public class AuthorizeActivity extends Activity {
	private Weibo mWeibo;
    private static final String CONSUMER_KEY = "708957066";// 替换为开发者的appkey，例如"1646212860";
    private static final String REDIRECT_URL = "https://api.weibo.com/oauth2/default.html";
    private Dialog dialog;
    private Button authorizeBtn, welcomeBtn;

    public static Oauth2AccessToken accessToken;
    public static final String TAG = "sinasdk";
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authorize);
		mWeibo = Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);
		View diaView=View.inflate(this, R.layout.dialog, null);
		dialog=new Dialog(AuthorizeActivity.this,R.style.dialog);
		dialog.setContentView(diaView);
		dialog.show();
		authorizeBtn=(Button)diaView.findViewById(R.id.button1);
		welcomeBtn=(Button)findViewById(R.id.welcomeBtn);
		welcomeBtn.setVisibility(View.INVISIBLE);
		authorizeBtn.setOnClickListener(new OnClickListener(){

	            @Override
	            public void onClick(View arg0) {
	            	 mWeibo.authorize(AuthorizeActivity.this, new AuthDialogListener());
	            }
	            
	        });
		
		welcomeBtn.setOnClickListener(new OnClickListener(){
			
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
           	 	intent.setClass(AuthorizeActivity.this, HomeActivity.class);
           	 	startActivity(intent);
           	 	finish();
			}
		});
		
	}
	
    class AuthDialogListener implements WeiboAuthListener {

        @Override
        public void onComplete(Bundle values) {
            String token = values.getString("access_token");
            String expires_in = values.getString("expires_in");
            AuthorizeActivity.accessToken = new Oauth2AccessToken(token, expires_in);
            if (AuthorizeActivity.accessToken.isSessionValid()) {
                String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                        .format(new java.util.Date(AuthorizeActivity.accessToken
                                .getExpiresTime()));
                Toast.makeText(AuthorizeActivity.this, "认证成功: \r\n access_token: " + token + "\r\n"
                        + "expires_in: " + expires_in + "\r\n有效期：" + date, Toast.LENGTH_SHORT)
                        .show();


                AccessTokenKeeper.keepAccessToken(AuthorizeActivity.this,
                        accessToken);
                dialog.dismiss();
       		 	welcomeBtn.setVisibility(View.VISIBLE);
                Toast.makeText(AuthorizeActivity.this, "认证成功", Toast.LENGTH_SHORT)
                        .show();
                AccountAPI keepuserinfo = new AccountAPI (AuthorizeActivity.accessToken);
                keepuserinfo.getUid(new RequestListener(){
                	public void onComplete(String response){
                		;//将uid和token发送给云端；
                	}

                	public void onIOException(IOException e){}

                	public void onError(WeiboException e){}
                });
                
                
            }
        }

        @Override
        public void onError(WeiboDialogError e) {
            Toast.makeText(getApplicationContext(),
                    "Auth error : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCancel() {
            Toast.makeText(getApplicationContext(), "Auth cancel",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(getApplicationContext(),
                    "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
                    .show();
        }

    }

}
