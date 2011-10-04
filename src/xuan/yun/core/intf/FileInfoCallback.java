/**
 *  The callback will be invoked when remote file info is received
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core.intf;

import xuan.yun.core.FileInfo;

public interface FileInfoCallback {

	/**
	 * Call this method when remote file info is arrived
	 * 
	 * @param info
	 */
	public void fileInfoReceived(FileInfo info);
	
}
