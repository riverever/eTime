package lightsns.com;
import java.io.IOException;

import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.net.RequestListener;
public class WeiBoRequestListener implements RequestListener{
	
	private String response = "1";
	
	@Override
	public void onComplete(String response) {
		// TODO Auto-generated method stub
		this.response = response; 
	}
	
	public String getResponse(){
		return response;
	}

	@Override
	public void onError(WeiboException e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onIOException(IOException e) {
		// TODO Auto-generated method stub
		
	}

}
