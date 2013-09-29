package lightsns.com;

import java.util.List;

import lightsns.com.AsyncImageLoader;
import lightsns.com.R;
import lightsns.com.WeiBoHolder;
import lightsns.com.AsyncImageLoader.ImageCallback;

import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import lightsns.db.*;
import lightsns.model.UserInfo;
import lightsns.model.WeiBoInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.security.KeyStore;
import java.security.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.api.AccountAPI;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.api.UsersAPI;
import com.weibo.sdk.android.api.WeiboAPI;
import com.weibo.sdk.android.WeiboException;





public class HomeActivity extends Activity {
	
	public static Oauth2AccessToken accessToken;
	public WeiBoRequestListener weiboRequestListener = new WeiBoRequestListener();
	public StatusesAPI getStatuses;
	public AccountAPI getAccount;
	public UsersAPI getUser;
	private List<WeiBoInfo> wbList;
	private LinearLayout loadingdingLayout;
	public long since_id = 0;
	public long userID = 0;
	public String userICON;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(MainActivity.TAG, "test6");
		setContentView(R.layout.home);
		HomeActivity.accessToken=AccessTokenKeeper.readAccessToken(HomeActivity.this);
		getStatuses = new StatusesAPI(accessToken);
		
		loadingdingLayout = (LinearLayout) findViewById(R.id.loadingdingLayout);
		
		ImageView usericon = (ImageView)findViewById(R.id.usericon);
		TextView username = (TextView)findViewById(R.id.textView1);
		TextView usermotto = (TextView)findViewById(R.id.textView2);
		{
		
			getAccount = new AccountAPI(accessToken);
			getUser = new UsersAPI(accessToken);
			getAccount.getUid(weiboRequestListener);
			String s = weiboRequestListener.getResponse();
			Log.e("uid", s);
			JSONObject sJson;
			try {
				sJson = new JSONObject(s);
				userID = Long.parseLong(sJson.getString("uid"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			getUser.show(userID, weiboRequestListener);
			s = weiboRequestListener.getResponse();
			Log.e("user info", s);
			try {
				sJson = new JSONObject(s);
				username.setText(sJson.getString("name"));
				usermotto.setText(sJson.getString("description"));
				userICON = sJson.getString("profile_image_url");
				AsyncImageLoader asyncImageLoader = new AsyncImageLoader();
				Drawable cachedImage = asyncImageLoader.loadDrawable(
						userICON, usericon, new ImageCallback() {

							@Override
							public void imageLoaded(Drawable imageDrawable,
									ImageView imageView, String imageUrl) {
								imageView.setImageDrawable(imageDrawable);
							}

						});
				if (cachedImage == null) {
					usericon.setImageResource(R.drawable.r2);
				} else {
					usericon.setImageDrawable(cachedImage);
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		ImageButton writeBtn = (ImageButton)findViewById(R.id.writeBtn);
		writeBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {			
				Intent it = new Intent(HomeActivity.this, ShareActivity.class);
	            it.putExtra(ShareActivity.EXTRA_ACCESS_TOKEN, HomeActivity.accessToken.getToken());
	            it.putExtra(ShareActivity.EXTRA_EXPIRES_IN, HomeActivity.accessToken.getExpiresTime());
	            startActivity(it);
			}
		});
		
		ImageButton refreshBtn = (ImageButton)findViewById(R.id.refreshBtn);
		refreshBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//wbList.clear();
				loadingdingLayout.setVisibility(View.VISIBLE);
				loadList();
			}
		});
		
		ImageButton cancelBtn = (ImageButton)findViewById(R.id.cancelBtn);
		cancelBtn.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				AccessTokenKeeper.clear(HomeActivity.this);
				Intent intent = new Intent();
				intent.setClass(HomeActivity.this, AuthorizeActivity.class);
				startActivity(intent);
			}
		});
		
		loadList();
	}
	
	private void loadList(){
		new Thread(){
			@Override
			public void run(){
				getStatuses.friendsTimeline(since_id, 0, 10, 1, false, WeiboAPI.FEATURE.ALL, false, weiboRequestListener);
				String s = weiboRequestListener.getResponse();
				//for(; s.equals("1"); s = weiboRequestListener.getResponse());
				Log.e("Statues", s);	
				try {
					JSONObject sJson = new JSONObject(s);
					
					JSONArray data = sJson.getJSONArray("statuses");
					for(int i = 0; i < data.length(); i++){
						Boolean haveImg = false;
						WeiBoInfo w = new WeiBoInfo();
						JSONObject d = data.getJSONObject(i);
						JSONObject userJson = d.getJSONObject("user");
						String weiboImage = "";
						String text = d.getString("text");
						if (d.has("thumbnail_pic")) {
							weiboImage = d.getString("thumbnail_pic");
							haveImg = true;
						}
						if (d.has("retweeted_status")) {
							JSONObject retweet = d.getJSONObject("retweeted_status");
							JSONObject retweetUser = retweet.getJSONObject("user");
							text = text+"//@"+retweetUser.getString("name")+retweet.getString("text");
							if (retweet.has("thumbnail_pic")) {
								weiboImage = retweet.getString("thumbnail_pic");
								haveImg = true;
							}
						}
						
						
						String id = d.getString("id");
						
						if(i == 1) since_id = Long.parseLong(id);
						
						String time = "";
						try {
							Date date = parseDate(d.getString("created_at"), "EEE MMM dd HH:mm:ss z yyyy");
							SimpleDateFormat sdf  =   new  SimpleDateFormat( " yyyy年MM月dd日HH时mm分 " );
							String datestr = sdf.format(date); 
							time = datestr.toString();
						} catch (WeiboException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						String userId = userJson.getString("id");
						String userName = userJson.getString("name");
						String userIcon = userJson.getString("profile_image_url");
						//String imageUrl = d.getString("thumbnail_pic");
						Log.v("head_url", "---->"+userIcon);
						Log.v("user_name","---->"+userName);
						
						if (wbList == null) {
							wbList = new ArrayList<WeiBoInfo>();
						}
						
						w.setId(id);
						w.setUserId(userId);
						w.setUserName(userName);
						w.setTime(time);
						w.setText(text);
						w.setHaveImage(haveImg);
						w.setUserIcon(userIcon);
						w.setImage(weiboImage);
						wbList.add(w);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Message message = handler.obtainMessage(0);
				handler.sendMessage(message);
			}
		}.start();
		
		
		
	}
	
	Handler handler = new Handler() {
		public void handleMessage(Message message) {
			if (wbList != null) {
				WeiBoAdapater adapater = new WeiBoAdapater();
				ListView Msglist = (ListView) findViewById(R.id.Msglist);
				Msglist.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View view,
							int arg2, long arg3) {
						Object obj = view.getTag();
						if (obj != null) {
							String id = obj.toString();
							Intent intent = new Intent(HomeActivity.this,
									ViewActivity.class);
							Bundle b = new Bundle();
							b.putString("key", id);
							intent.putExtras(b);
							startActivity(intent);
						}
					}

				});

				/*
				 * adapater.notifyDataSetChanged();
				 * Msglist.clearDisappearingChildren();
				 */
				Msglist.setAdapter(adapater);
				loadingdingLayout.setVisibility(View.GONE);
			}
		}
	};
	
	
	public class WeiBoAdapater extends BaseAdapter {

		private AsyncImageLoader asyncImageLoader;

		@Override
		public int getCount() {
			return wbList.size();
		}

		@Override
		public Object getItem(int position) {
			return wbList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			asyncImageLoader = new AsyncImageLoader();
			convertView = LayoutInflater.from(getApplicationContext()).inflate(
					R.layout.weibo, null);
			WeiBoHolder wh = new WeiBoHolder();
			wh.wbicon = (ImageView) convertView.findViewById(R.id.wbicon);
			wh.wbimage = (ImageView) convertView.findViewById(R.id.wbimage);
			wh.wbtext = (TextView) convertView.findViewById(R.id.wbtext);
			wh.wbtime = (TextView) convertView.findViewById(R.id.wbtime);
			wh.wbuser = (TextView) convertView.findViewById(R.id.wbuser);
			
			wh.layout = (LinearLayout)convertView.findViewById(R.id.weiboitem);
			wh.Source = (ImageView) convertView.findViewById(R.id.wbsource);
			WeiBoInfo wb = wbList.get(position);
			if (wb != null) {
				convertView.setTag(wb.getId());
				wh.wbuser.setText(wb.getUserName());
				wh.wbtime.setText(wb.getTime());
				wh.wbtext.setText(wb.getText(), TextView.BufferType.SPANNABLE);
				// textHighlight(wh.wbtext,new char[]{'#'},new char[]{'#'});
				// textHighlight(wh.wbtext,new char[]{'@'},new char[]{':',' '});
				// textHighlight2(wh.wbtext,"http://"," ");
				Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.sinalogo);
			    Bitmap bMapScaled = Bitmap.createScaledBitmap(bMap, 23, 21, true);
				wh.Source.setImageBitmap(bMapScaled);
				//wh.layout.setBackgroundColor(Color.rgb(255, 160, 98));		
				if (wb.getHaveImage()) {
					Drawable cachedWeiboImage = asyncImageLoader.loadDrawable(
							wb.getImage(), wh.wbimage, new ImageCallback() {

								@Override
								public void imageLoaded(Drawable imageDrawable,
										ImageView imageView, String imageUrl) {
									imageView.setImageDrawable(imageDrawable);
								}

							});
					if(cachedWeiboImage == null){
						wh.wbimage.setImageResource(R.drawable.samllbig);
					}
					else{
						wh.wbimage.setImageDrawable(cachedWeiboImage);
					}
						
				}
				Drawable cachedImage = asyncImageLoader.loadDrawable(
						wb.getUserIcon(), wh.wbicon, new ImageCallback() {

							@Override
							public void imageLoaded(Drawable imageDrawable,
									ImageView imageView, String imageUrl) {
								imageView.setImageDrawable(imageDrawable);
							}

						});
				if (cachedImage == null) {
					wh.wbicon.setImageResource(R.drawable.r2);
				} else {
					wh.wbicon.setImageDrawable(cachedImage);
				}
			}

			return convertView;
		}
	}
	
	protected static Date parseDate(String str, String format) throws WeiboException{
		Map<String,SimpleDateFormat> formatMap = new HashMap<String,SimpleDateFormat>();
		
		if(str==null||"".equals(str)){
        	return null;
        }
    	SimpleDateFormat sdf = formatMap.get(format);
        if (null == sdf) {
            sdf = new SimpleDateFormat(format, Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            formatMap.put(format, sdf);
        }
        try {
            synchronized(sdf){
                // SimpleDateFormat is not thread safe
                return sdf.parse(str);
            }
        } catch (ParseException pe) {
            throw new WeiboException("Unexpected format(" + str + ") returned from sina.com.cn");
        }
    }
	
	
	
}
	
	
	
//
//	
//	private void loadList(){
//        if(ConfigHelper.nowUser==null)
//        {
//            
//        }
//        else
//        {
//            user=ConfigHelper.nowUser;
//            //��ʾ��ǰ�û����
//            TextView showName=(TextView)findViewById(R.id.showName);
//            showName.setText(user.getUserName());
//            
//            OAuth auth=new OAuth();
//            String url = "http://api.t.sina.com.cn/statuses/friends_timeline.json";
//            List params=new ArrayList();
//            params.add(new BasicNameValuePair("source", auth.consumerKey)); 
//            HttpResponse response =auth.SignRequest(user.getToken(), user.getTokenSecret(), url, params);
//            if (200 == response.getStatusLine().getStatusCode()){
//                try {
//                    InputStream is = response.getEntity().getContent();
//                    Reader reader = new BufferedReader(new InputStreamReader(is), 4000);
//                    StringBuilder buffer = new StringBuilder((int) response.getEntity().getContentLength());
//                    try {
//                        char[] tmp = new char[1024];
//                        int l;
//                        while ((l = reader.read(tmp)) != -1) {
//                            buffer.append(tmp, 0, l);
//                        }
//                    } finally {
//                        reader.close();
//                    }
//                    String string = buffer.toString();
//                    //Log.e("json", "rs:" + string);
//                    response.getEntity().consumeContent();
//                    JSONArray data=new JSONArray(string);
//                    for(int i=0;i<data.length();i++)
//                    {
//                        JSONObject d=data.getJSONObject(i);
//                        //Log.e("json", "rs:" + d.getString("created_at"));
//                        if(d!=null){
//                            JSONObject u=d.getJSONObject("user");
//                            if(d.has("retweeted_status")){
//                                JSONObject r=d.getJSONObject("retweeted_status");
//                            }
//                            
//                            //΢��id
//                            String id=d.getString("id");
//                            String userId=u.getString("id");
//                            String userName=u.getString("screen_name");
//                            String userIcon=u.getString("profile_image_url");
//                            Log.e("userIcon", userIcon);
//                            String time=d.getString("created_at");
//                            String text=d.getString("text");
//                            Boolean haveImg=false;
//                            if(d.has("thumbnail_pic")){
//                                haveImg=true;
//                                //String thumbnail_pic=d.getString("thumbnail_pic");
//                                //Log.e("thumbnail_pic", thumbnail_pic);
//                            }
//                            
//                            Date date=new Date(time);
//                            time=ConvertTime(date);
//                            if(wbList==null){
//                                wbList=new ArrayList<WeiBoInfo>();
//                            }
//                            WeiBoInfo w=new WeiBoInfo();
//                            w.setId(id);
//                            w.setUserId(userId);
//                            w.setUserName(userName);
//                            w.setTime(time);
//                            w.setText(text);
//                            
//                            w.setHaveImage(haveImg);
//                            w.setUserIcon(userIcon);
//                            wbList.add(w);
//                        }
//                    }
//                    
//                }catch (IllegalStateException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                } 
//            }
//            
//            if(wbList!=null)
//            {
//                WeiBoAdapater adapater = new WeiBoAdapater();
//                ListView Msglist=(ListView)findViewById(R.id.Msglist);
//                Msglist.setOnItemClickListener(new OnItemClickListener(){
//                    @Override
//                    public void onItemClick(AdapterView<?> arg0, View view,int arg2, long arg3) {
//                        Object obj=view.getTag();
//                        if(obj!=null){
//                            String id=obj.toString();
//                            Intent intent = new Intent(HomeActivity.this,ViewActivity.class);
//                            Bundle b=new Bundle();
//                            b.putString("key", id);
//                            intent.putExtras(b);
//                            startActivity(intent);
//                        }
//                    }
//                    
//                });
//                Msglist.setAdapter(adapater);
//            }
//        }
//        loadingLayout.setVisibility(View.GONE);
//    }	
//	
//
//	private List<WeiBoInfo> wbList;
//
//    public class WeiBoAdapater extends BaseAdapter{
//
//        private AsyncImageLoader asyncImageLoader;
//        
//        @Override
//        public int getCount() {
//            return wbList.size();
//        }
//
//        @Override
//        public Object getItem(int position) {
//            return wbList.get(position);
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            asyncImageLoader = new AsyncImageLoader();
//            convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.weibo, null);
//            WeiBoHolder wh = new WeiBoHolder();
//            wh.wbicon = (ImageView) convertView.findViewById(R.id.wbicon);
//            wh.wbtext = (TextView) convertView.findViewById(R.id.wbtext);
//            wh.wbtime = (TextView) convertView.findViewById(R.id.wbtime);
//            wh.wbuser = (TextView) convertView.findViewById(R.id.wbuser);
//            wh.wbimage=(ImageView) convertView.findViewById(R.id.wbimage);
//            WeiBoInfo wb = wbList.get(position);
//            if(wb!=null){
//                convertView.setTag(wb.getId());
//                wh.wbuser.setText(wb.getUserName());
//                wh.wbtime.setText(wb.getTime());
//                wh.wbtext.setText(wb.getText(), TextView.BufferType.SPANNABLE);
//                textHighlight(wh.wbtext,new char[]{'#'},new char[]{'#'});
//                textHighlight(wh.wbtext,new char[]{'@'},new char[]{':',' '});
//                textHighlight2(wh.wbtext,"http://"," ");
//                
//                if(wb.getHaveImage()){
//                    wh.wbimage.setImageResource(R.drawable.images);
//                }
//                Drawable cachedImage = asyncImageLoader.loadDrawable(wb.getUserIcon(),wh.wbicon, new ImageCallback(){
//
//                    @Override
//                    public void imageLoaded(Drawable imageDrawable,ImageView imageView, String imageUrl) {
//                        imageView.setImageDrawable(imageDrawable);
//                    }
//                    
//                });
//                 if (cachedImage == null) {
//                     wh.wbicon.setImageResource(R.drawable.usericon);
//                    }else{
//                        wh.wbicon.setImageDrawable(cachedImage);
//                    }
//            }
//            
//            return convertView;
//        }
//	
//}

