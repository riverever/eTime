package lightsns.model;

public class Comments {
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	// 发布人id
	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	// 发布人名字
	private String userName;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	// 发布人头像
	private String userIcon;

	public String getUserIcon() {
		return userIcon;
	}

	public void setUserIcon(String userIcon) {
		this.userIcon = userIcon;
	}

	// 发布时间
	private String time;

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	// 评论内容
	private String Comment;

	public String getText() {
		return Comment;
	}

	public void setText(String Comment) {
		this.Comment = Comment;
	}
}

