package lightsns.com;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.CommentsAPI;
import com.weibo.sdk.android.api.WeiboAPI;
import com.weibo.sdk.android.api.WeiboAPI.AUTHOR_FILTER;

import lightsns.com.R;

import lightsns.model.Comments;
import lightsns.com.AsyncImageLoader;
import lightsns.com.AsyncImageLoader.ImageCallback;
import lightsns.db.AccessTokenKeeper;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class CommentsActivity extends Activity{

	
	private LinearLayout loadingdingLayout;
	private ImageButton refreshBtn;
	private List<Comments> cmList;
	protected String key;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.commentslist_view);
		
		LinearLayout view = (LinearLayout) findViewById(R.id.layout);
		loadingdingLayout = (LinearLayout) findViewById(R.id.loadingdingLayout);

		refreshBtn = (ImageButton) findViewById(R.id.refresh);
		refreshBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//cmList.clear();
				loadingdingLayout.setVisibility(View.VISIBLE);
				OpenThread();
			}
			
		});

		//tocast(ConfigHelper.nowUserr.getUserName());
		OpenThread();


	}

	
	public void OpenThread(){
		loadingdingLayout.setVisibility(View.VISIBLE);
		new Thread() {
			public void run(){
				// 获取上一个页面传递过来的key，key为某一条微博的id
				Intent i = CommentsActivity.this.getIntent();
				if (!i.equals(null)) {
					Bundle b = i.getExtras();
					if (b != null) {
						if (b.containsKey("key")) {
							key = b.getString("key");
							listComments(key);
						}
					}

				}
				Message message = handler.obtainMessage(0);
				handler.sendMessage(message);

			}
		}.start();
	}
	
	protected void listComments(String key) {
		// TODO Auto-generated method stub
		CommentsAPI commentsApi = new CommentsAPI(AccessTokenKeeper.readAccessToken(CommentsActivity.this));
		WeiBoRequestListener weiboRequestListener = new WeiBoRequestListener();
		commentsApi.show(Long.parseLong(key), 0, 0, 10, 1, WeiboAPI.AUTHOR_FILTER.ALL, weiboRequestListener);
		String cString = weiboRequestListener.getResponse();
		Log.e("COMMENTS", cString);
		try {
			JSONObject cJson = new JSONObject(cString);
			
			JSONArray data = cJson.getJSONArray("comments");
			for (int i = 0; i < data.length(); i++) {
				JSONObject d = data.getJSONObject(i);
				if (d != null) {
					JSONObject u = d.getJSONObject("user");
					if (d.has("retweeted_status")) {
						JSONObject r = d.getJSONObject("retweeted_status");
					}
					String id = d.getString("id");
					String userId = u.getString("id");
					String userName = u.getString("screen_name");
					String userIcon = u.getString("profile_image_url");
					String text = d.getString("text");
					Boolean haveImg = false;
					if (d.has("thumbnail_pic")) {
						haveImg = true;
					}
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
					if (cmList == null) {
						cmList = new ArrayList<Comments>();
					}
					Comments c = new Comments();
					c.setId(id);
					c.setUserId(userId);
					c.setUserName(userName);
					c.setTime(time);
					c.setText(text);
					c.setUserIcon(userIcon);
					cmList.add(c);
				}
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}



	Handler handler = new Handler() {
		public void handleMessage(Message message) {
			if (cmList != null) {
				CommentsAdapater adapater = new CommentsAdapater();
				ListView Msglist = (ListView) findViewById(R.id.Msglist);
				Msglist.setAdapter(adapater);
				loadingdingLayout.setVisibility(View.GONE);
			}
		}
	};
	
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
	
	public String ConvertTime(Date data) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月dd日HH时mm分");
		String time = df.format(data);
		return time;
	}
	
	
	
	public class CommentsAdapater extends BaseAdapter {

		private AsyncImageLoader asyncImageLoader;

		@Override
		public int getCount() {
			return cmList.size();
		}

		@Override
		public Object getItem(int position) {
			return cmList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			asyncImageLoader = new AsyncImageLoader();
			convertView = LayoutInflater.from(getApplicationContext()).inflate(
					R.layout.comments, null);
			CommentsHolder cms = new CommentsHolder();
			cms.cmicon = (ImageView) convertView.findViewById(R.id.cmicon);
			cms.cmtext = (TextView) convertView.findViewById(R.id.cmtext);
			cms.cmtime = (TextView) convertView.findViewById(R.id.cmtime);
			cms.cmuser = (TextView) convertView.findViewById(R.id.cmuser);
			cms.cmimage = (ImageView) convertView.findViewById(R.id.cmimage);
			Comments cm = cmList.get(position);
			if (cm != null) {
				convertView.setTag(cm.getId());
				
				cms.cmtime.setText(cm.getTime());
				cms.cmuser.setText(cm.getUserName());
				cms.cmtext.setText(cm.getText(), TextView.BufferType.SPANNABLE);
				Drawable cachedImage = asyncImageLoader.loadDrawable(
						cm.getUserIcon(), cms.cmicon, new ImageCallback() {

							@Override
							public void imageLoaded(Drawable imageDrawable,
									ImageView imageView, String imageUrl) {
								imageView.setImageDrawable(imageDrawable);
							}

						});
				if (cachedImage == null) {
					cms.cmicon.setImageResource(R.drawable.r2);
				} else {
					cms.cmicon.setImageDrawable(cachedImage);
				}
			}

			return convertView;
		}
	}
}
