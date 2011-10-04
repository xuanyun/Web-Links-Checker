/**
 *  The FileInfo object store all kinds of informations of the remote file
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core;

/**
 * The information for a remote file
 */
public class FileInfo {

	// current state of the file
	public static enum State {
		INDETERMINATED,	// download in progress
		DOWNLOADED,		// download finished
		FAILED			// download failed
	};
	
	private final String url;
	private String type;
	private long size;
	private int statusCode;
	
	private long downloadedSize;
	private long startTime;
	private long usedTime;
	
	private State state = State.INDETERMINATED;
	
	public FileInfo(String url) {
		super();
		this.url = url;
		setSize(-1);
	}

	public long getSize() {
		return size;
	}

	public void setSize(long fileSize) {
		this.size = fileSize;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUrl() {
		return url;
	}

	public long getDownloadedSize() {
		return downloadedSize;
	}

	public void setDownloadedSize(long size) {
		this.downloadedSize = size;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getUsedTime() {
		return usedTime;
	}

	public void setUsedTime(long usedTime) {
		this.usedTime = usedTime;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String toString() {
		return "<FileInfo url='" + url + "' type='" + type + "' size='" + size + "' status='" + statusCode 
			+ "' downloadedSize='" + downloadedSize + "' usedTime='" + usedTime + "'/>";
	}
}
