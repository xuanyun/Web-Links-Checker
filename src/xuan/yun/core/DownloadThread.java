/**
 *  DownloadThread is a thread to take and execute DownloadTask 
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;

import xuan.yun.core.Env;
import xuan.yun.core.intf.DownloadManager;

public class DownloadThread extends Thread {

	private static int threadId = 0;
	
	private int remainSplitThreshold = Env.getIntegerSetting(Env.DOWNLOAD_REMAIN_SPLIT_THRESHOLD);
	
	private DownloadManager manager;
	
	private DownloadTask task;

	public DownloadThread(DownloadManager mgr) {
		super();
		manager = mgr;
		setName("Thread #" + threadId);
		threadId ++;
	}

	public void run() {
		// always try to get a task to run
		while (manager.isRunning()) {
			task = manager.requestForDownloadTask();
			if (task == null) {
				continue;
			}
			Env.getLogger().info(getName() + " took task " + task);
			
			// run the task
			boolean taskFinished = false;
			RandomAccessFile out = null;
			try {
				// remote source file
				final URL url = new URL(task.getUrl());
				final URLConnection conn = url.openConnection();
				conn.setConnectTimeout(Env.getIntegerSetting(Env.CONNECTION_TIMEOUT));
				conn.setReadTimeout(Env.getIntegerSetting(Env.READ_TIMEOUT));
				conn.setAllowUserInteraction(true);
				if (task.getEnd() != -1) {
					conn.setRequestProperty("Range", "bytes=" + task.getCurrent() + "-" + task.getEnd());
				}
				final BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
				
				// local target file
				out = new RandomAccessFile(new File(task.getTarPath()), "rw");
                out.seek(task.getCurrent());
                
                // task loop
				long len;
				byte[] buff = new byte[1024];
				while (task.getEnd() == -1 || task.getCurrent() < task.getEnd()) {
					// read data
					len = in.read(buff, 0, buff.length);
				    if (len == -1) {
				    	break;
				    }
				    
				    // calculate new position
				    long newPos = task.getCurrent() + len;
				    if (task.getEnd() >= 0 && newPos > task.getEnd()) {
				    	newPos = task.getEnd() + 1;
                    }
				    
				    // write data to file
				    final int dataLength = (int)(newPos - task.getCurrent());
				    out.write(buff, 0, dataLength);
				    
				    // move task current position
				    task.setCurrent(newPos);
				    
				    // report progress
				    manager.onDataDownloaded(task, dataLength);
				    
				    // split the task, if the task has a known end, not too small, and there is idle download thread
				    if (task.getEnd() > 0 && task.getEnd() - task.getCurrent() > remainSplitThreshold && manager.hasIdleDownloadThread()) {
				    	manager.addDownloadTask(task.splitTask());
                    }
				}
				out.close();
				in.close();
				taskFinished = true;
			} catch (FileNotFoundException fnfe) {
				Env.getLogger().severe(getName() + ": " + fnfe.getMessage());
				final DownloadTask latestTask = task;
				task = null;
				manager.onDownloadTerminated(latestTask);
			} catch (IOException ioe) {
				Env.getLogger().info(getName() + " met " + ioe.getClass().getSimpleName() + ": " + ioe.getMessage());
				// Return the task to manager if failure count < MAX_FAILURES_COUNT
				task.setFailureCount(task.getFailureCount() + 1);
				Env.getLogger().info(getName() + " gave up " + task);
				final DownloadTask latestTask = task;
				task = null;
				final int maxFailures = Env.getIntegerSetting(Env.MAX_FAILURES_COUNT);
				if (latestTask.getFailureCount() < maxFailures) {
					manager.addDownloadTask(latestTask);
				} else {
					Env.getLogger().info("Too many failures, terminate the download for " + latestTask.getUrl());
					manager.onDownloadTerminated(latestTask);
				}
			} catch (Exception e) {
				Env.getLogger().severe(e.getMessage());
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (IOException e) {}
				}
			}
			
			// task is finished
			if (taskFinished) {
				Env.getLogger().info(getName() + " finished task " + task);
				final DownloadTask latestTask = task;
				task = null;
				manager.onDownloadTaskFinished(latestTask);
			}
		}
	}
	
	public boolean isRunningTask() {
		return (task != null);
	}

	public DownloadTask getTask() {
		return task;
	}
}
