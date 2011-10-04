/**
 *  The state listener for LinkCheckerCore
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core.intf;

import xuan.yun.core.FileInfo;

public interface CoreStateListener {
	
	/**
	 * Will be called when the input URL is not accessable
	 */
	public void inputURLBroken();
	
	/**
	 * Will be called when there is no link found in given URL
	 */
	public void noLinkFound();

	/**
	 * Will be called when the link is downloading data
	 * 
	 * @param info
	 */
	public void linkDownloading(FileInfo info);
	
	/**
	 * Will be called when the link is checked OK
	 * 
	 * @param info
	 */
	public void linkCheckPassed(FileInfo info);
	
	/**
	 * Will be called when a broken link is found
	 * 
	 * @param info
	 */
	public void linkCheckFailed(FileInfo info);
	
	/**
	 * Will be called when all links are checked
	 */
	public void allLinksChecked();
}