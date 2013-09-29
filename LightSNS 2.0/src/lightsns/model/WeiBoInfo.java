package lightsns.model;

public class WeiBoInfo {
	// ����id
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	// ������id
	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	// ����������
	private String userName;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	// ������ͷ��
	private String userIcon;

	public String getUserIcon() {
		return userIcon;
	}

	public void setUserIcon(String userIcon) {
		this.userIcon = userIcon;
	}

	// ����ʱ��
	private String time;

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	// �Ƿ���ͼƬ
	private Boolean haveImage = false;

	public Boolean getHaveImage() {
		return haveImage;
	}

	public void setHaveImage(Boolean haveImage) {
		this.haveImage = haveImage;
	}

	// ��������
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	private String type;	
	public String gettype(){
		return type;
	}
	public void setType(String type){
		this.type = type;
	}
	
	private String image;
	public String getImage(){
		return image;
	}
	public void setImage(String image){
		this.image = image;
	}
}
