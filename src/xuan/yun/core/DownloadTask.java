/**
 *  DownloadTask represent a task to download a block of a file
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core;

import xuan.yun.core.Env;


public class DownloadTask {

	// parameter
	private final String url;
	private final String tarPath;
	
	// data info
	private String type;
	private long fileSize;
	
	// block info (start, pos, end)
	private long start;
	private long current;
	private long end;		// -1 means unknown
	
	// counter for failure
	private int failureCount;
	
	public DownloadTask(String url, String targetFilePath) {
		this.url = url;
		this.tarPath = targetFilePath;
	}
	
	public String getUrl() {
		return url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long size) {
		this.fileSize = size;
	}

	public long getCurrent() {
		return current;
	}

	public void setCurrent(long current) {
		this.current = current;
	}

	public long getEnd() {
		return end;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public String getTarPath() {
		return tarPath;
	}
	
	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}

	/**
	 * Split the task into two tasks
	 * 
	 * @return
	 * 		the new task
	 */
	public DownloadTask splitTask() {
		final String oldTaskDesc = toString();
		final DownloadTask task = new DownloadTask(getUrl(), getTarPath());
		final long splitPos = (getCurrent() + getEnd()) / 2;
		task.setStart(splitPos + 1);
		task.setCurrent(splitPos + 1);
		task.setEnd(getEnd());
		setEnd(splitPos);
		Env.getLogger().info(oldTaskDesc + " is splited to " + toString() + " and " + task);
		return task;
	}

	public String toString() {
		return "<DownloadTask url='" + url + "' start='" + start + "' current='" + current + "' end='" + end 
			+ "' type='" + type + "' fileSize='" + fileSize + "' failureCount='" + failureCount + "'/>";
	}
}
