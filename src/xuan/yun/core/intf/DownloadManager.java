/**
 *  The DownloadManager interface is implemented by LinkCheckerCore
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core.intf;

import xuan.yun.core.DownloadTask;

public interface DownloadManager {

	/**
	 * Check if the manager is still running
	 * 
	 * @return
	 */
	public boolean isRunning();

	/**
	 * Check if there is idle download thread
	 * 
	 * @return
	 */
	public boolean hasIdleDownloadThread();
	
	/**
	 * Request download task from manager
	 * 
	 * @return null if not accepted
	 */
	public DownloadTask requestForDownloadTask();
	
	/**
	 * Add a download task to the manager
	 */
	public void addDownloadTask(DownloadTask task);
	
	/**
	 * Will be called when dataLength bytes data are downloaded 
	 * 
	 * @param dataLength
	 */
	public void onDataDownloaded(DownloadTask task, int dataLength);
	
	/**
	 * Will be called when a download task is finished
	 */
	public void onDownloadTaskFinished(DownloadTask task);
	
	/**
	 * Will be called when download is terminated
	 */
	public void onDownloadTerminated(DownloadTask task);
}
