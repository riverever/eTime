package lightsns.com;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.weibo.sdk.android.api.CommentsAPI;
import com.weibo.sdk.android.api.StatusesAPI;
import com.weibo.sdk.android.api.WeiboAPI;

import lightsns.com.R;
import lightsns.com.AsyncImageLoader;
import lightsns.com.HomeActivity;
import lightsns.com.AsyncImageLoader.ImageCallback;
import lightsns.db.AccessTokenKeeper;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class ViewActivity extends Activity{
	private LinearLayout loadingdingLayout;
	private Button fwdWeiBoBn;
	private Button commentBn;
	private Button listCommentsBn;
	private String key = "";
	private JSONObject weiboJson;
	private JSONObject countJson;
	PopupWindow _PopupWindow;
	StatusesAPI api;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewactivity);
		api = new StatusesAPI(AccessTokenKeeper.readAccessToken(ViewActivity.this));
		
		LinearLayout view = (LinearLayout) findViewById(R.id.viewlayout);
		loadingdingLayout = (LinearLayout) findViewById(R.id.loadingdingLayout);

		fwdWeiBoBn = (Button) findViewById(R.id.btn_forward);

		commentBn = (Button) findViewById(R.id.btn_comment);
		
		listCommentsBn = (Button) findViewById(R.id.btn_showcom);

		fwdWeiBoBn.setOnClickListener(new FwdWeibo());
		
		commentBn.setOnClickListener(new Comment());
		
		listCommentsBn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				listComments();
			}	
		});
		
		OpenThread();
	}

	protected void listComments() {
		// TODO Auto-generated method stub
		Intent intent = new Intent(ViewActivity.this,
				CommentsActivity.class);
		Bundle b = new Bundle();
		b.putString("key", key);
		intent.putExtras(b);
		startActivity(intent);
	}


	private void OpenThread() {
		// TODO Auto-generated method stub
		loadingdingLayout.setVisibility(View.VISIBLE);
		new Thread() {
			@Override
			public void run() {

				// 获取上一个页面传递过来的key，key为某一条微博的id
				Intent i = ViewActivity.this.getIntent();
				if (!i.equals(null)) {
					Bundle b = i.getExtras();
					if (b != null) {
						if (b.containsKey("key")) {
							key = b.getString("key");
							view(key);
						}
					}

				}
				Message message = handler.obtainMessage(0);
				handler.sendMessage(message);
			}
		}.start();
	}
	
	Handler handler = new Handler(){
		public void handleMessage(Message message) {
			try {
				if (weiboJson != null) {
					JSONObject u = weiboJson.getJSONObject("user");
					String userName = u.getString("screen_name");
					String userIcon = u.getString("profile_image_url");
					Log.e("userIcon", userIcon);
					String time = weiboJson.getString("created_at");
					String text = weiboJson.getString("text");

					TextView utv = (TextView) findViewById(R.id.user_name);
					utv.setText(userName);
					TextView ttv = (TextView) findViewById(R.id.text);
					ttv.setText(text);

					ImageView iv = (ImageView) findViewById(R.id.user_icon);
					AsyncImageLoader asyncImageLoader = new AsyncImageLoader();
					Drawable cachedImage = asyncImageLoader.loadDrawable(
							userIcon, iv, new ImageCallback() {
								@Override
								public void imageLoaded(Drawable imageDrawable,
										ImageView imageView, String imageUrl) {

									imageView.setImageDrawable(imageDrawable);
								}
							});
					if (cachedImage == null) {
						iv.setImageResource(R.drawable.icon);
					} else {
						iv.setImageDrawable(cachedImage);
					}
					if (weiboJson.has("bmiddle_pic")) {
						String picurl = weiboJson.getString("bmiddle_pic");
						String picurl2 = weiboJson.getString("original_pic");

						ImageView pic = (ImageView) findViewById(R.id.pic);
						pic.setTag(picurl2);
						pic.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Object obj = v.getTag();
								Intent intent = new Intent(ViewActivity.this,
										HomeActivity.class);
								Bundle b = new Bundle();
								b.putString("url", obj.toString());
								intent.putExtras(b);
								startActivity(intent);
							}
						});
						Drawable cachedImage2 = asyncImageLoader.loadDrawable(
								picurl, pic, new ImageCallback() {
									@Override
									public void imageLoaded(
											Drawable imageDrawable,
											ImageView imageView, String imageUrl) {
										showImg(imageView, imageDrawable);
									}
								});
						if (cachedImage2 == null) {
							// pic.setImageResource(R.drawable.usericon);
						} else {
							showImg(pic, cachedImage2);
						}
					}
				}

				if (countJson != null) {
				
					String comments = countJson.getString("comments");
					String rt = countJson.getString("reposts");
					Button btn_gz = (Button) findViewById(R.id.btn_forward);
					btn_gz.setText("转发(" + rt + ")");
					Button btn_pl = (Button) findViewById(R.id.btn_comment);
					btn_pl.setText("评论(" + comments + ")");
					
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		
			fwdWeiBoBn.setEnabled(true);
			listCommentsBn.setEnabled(true);
			commentBn.setEnabled(true);
			loadingdingLayout.setVisibility(View.GONE);
			
		}
	};
	
	protected void view(String key) {
		// TODO Auto-generated method stub
		WeiBoRequestListener weiboRequestListener = new WeiBoRequestListener();
		long weiboId = Long.parseLong(key);
		api.show(weiboId, weiboRequestListener);
		String response = weiboRequestListener.getResponse();
		try {
			weiboJson = new JSONObject(response);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String []ids = {key};
		api.count(ids, weiboRequestListener);
		response = weiboRequestListener.getResponse();
		Log.e("count", response);
		try {
			JSONArray countArray = new JSONArray(response);
			countJson = countArray.getJSONObject(0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	protected void showImg(ImageView view, Drawable img) {
		// TODO Auto-generated method stub
		int w = img.getIntrinsicWidth();
		int h = img.getIntrinsicHeight();
		Log.e("w", w + "/" + h);
		if (w > 300) {
			int hh = 300 * h / w;
			Log.e("hh", hh + "");
			LayoutParams para = view.getLayoutParams();
			para.width = 300;
			para.height = hh;
			view.setLayoutParams(para);
		}
		view.setImageDrawable(img);
	}
	
	private EditText et;

	
	class FwdWeibo implements OnClickListener{

		@Override
		public void onClick(View v) {
			LayoutInflater _LayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			ViewGroup _ViewGroup = (ViewGroup) _LayoutInflater.inflate(
					R.layout.comment_view, null, true);

			Button btnclosePop = (Button) _ViewGroup.findViewById(R.id.sendBn);
			btnclosePop.setText("转发");
			Button btnclose = (Button) _ViewGroup.findViewById(R.id.closeBn);
			et = (EditText) _ViewGroup.findViewById(R.id.editText);
			
			et.setHint("转发微博");
			btnclose.setOnClickListener(new closePopup());
			btnclosePop.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String Status = "";
					if (!et.getText().toString().trim().equals("")) {
						Status = et.getText().toString();		
					}
					WeiBoRequestListener weiboRequestListener = new WeiBoRequestListener();
					api.repost(Long.parseLong(key), Status, WeiboAPI.COMMENTS_TYPE.NONE, weiboRequestListener);
					
					if (_PopupWindow != null && _PopupWindow.isShowing()) {
						_PopupWindow.dismiss();
					}
					tocast("转发成功");
				}
			});

			_PopupWindow = new PopupWindow(_ViewGroup,
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
			_PopupWindow.setBackgroundDrawable(new BitmapDrawable());
			_PopupWindow.setAnimationStyle(R.style.PopupAnimation);
			_PopupWindow.showAtLocation(findViewById(R.id.viewlayout),
					Gravity.CENTER | Gravity.CENTER, 0, 0);
			_PopupWindow.update();
		}
		
	}
	class Comment implements OnClickListener {

		@Override
		public void onClick(View v) {
			LayoutInflater _LayoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			ViewGroup _ViewGroup = (ViewGroup) _LayoutInflater.inflate(
					R.layout.comment_view, null, true);

			Button btnclosePop = (Button) _ViewGroup.findViewById(R.id.sendBn);
			btnclosePop.setText("评论");
			Button btnclose = (Button) _ViewGroup.findViewById(R.id.closeBn);
			et = (EditText) _ViewGroup.findViewById(R.id.editText);
			
			et.setHint("请输入评语~~~~");
			btnclose.setOnClickListener(new closePopup());
			btnclosePop.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					if (!et.getText().toString().trim().equals("")) {
						RequestP("评论");
						if (_PopupWindow != null && _PopupWindow.isShowing()) {
							_PopupWindow.dismiss();
						}
					} else {
						tocast("评论内容不能为空~~！！！");
					}

				}
			});

			_PopupWindow = new PopupWindow(_ViewGroup,
					LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, true);
			_PopupWindow.setBackgroundDrawable(new BitmapDrawable());
			_PopupWindow.setAnimationStyle(R.style.PopupAnimation);
			_PopupWindow.showAtLocation(findViewById(R.id.viewlayout),
					Gravity.CENTER | Gravity.CENTER, 0, 0);
			_PopupWindow.update();
		}

		public void RequestP(String msg) {
			// TODO Auto-generated method stub
			CommentsAPI commentsApi = new CommentsAPI(AccessTokenKeeper.readAccessToken(ViewActivity.this));
			WeiBoRequestListener weiboRequestListener = new WeiBoRequestListener();
			commentsApi.create(et.getText().toString(), Long.parseLong(key), false, weiboRequestListener);
			if(!weiboRequestListener.getResponse().equals("1")){
				tocast(msg+"成功");
			}
			else{
				tocast(msg+"成功");
			}
		}
	}
	
	class closePopup implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (_PopupWindow != null && _PopupWindow.isShowing()) {
				_PopupWindow.dismiss();
			}
		}
	}
	
	public void tocast(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
}
