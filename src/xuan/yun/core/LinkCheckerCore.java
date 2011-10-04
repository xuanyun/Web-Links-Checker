/**
 *  LinkCheckerCore will do the link checkings simultaneously
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xuan.yun.core.FileInfo.State;
import xuan.yun.core.intf.CoreStateListener;
import xuan.yun.core.intf.DownloadManager;
import xuan.yun.core.intf.FileInfoCallback;

public class LinkCheckerCore implements DownloadManager {
	
	private static final Pattern LINK_REG = Pattern.compile("<a\\s[^>]*href\\s*=\\s*[\"\']?([^\"\' ]*)[\"\']?[^>]*>(.*?(?<!</a>))</a>", 
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final Pattern PROTOCOL_REG = Pattern.compile("^[^:]+://", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern FILE_REG = Pattern.compile("^.*\\.\\w{2,4}$", Pattern.CASE_INSENSITIVE);
	
	private int maxThreads;		// maximum count of threads for downloading
	
	private final String url;		// input URL (main URL)
	private final String baseUrl;	// the base URL (for relative URL)
	
	private long startTime = 0;		// the time that start the core
	private long elapsedTime = 0;	// time used for downloading
	private long downloadSize = 0;	// downloaded data size
	
	private boolean running = false;	// if the core is running

	private List<DownloadTask> downloadTasks;	// task pool
	
	private List<DownloadThread> downloadThreads;	// thread pool
	
	private Map<String, List<DownloadTask>> fileTasksMap; // the tasks for certain URL
	
	private Map<String, FileInfo> fileInfoMap;	// the FileInfo objects for certain URL

	private List<CoreStateListener> listeners;	// listeners that observe the link state
	
	/**
	 * Create instance for given URL, with maxThreads = DOWNLOAD_MAX_THREAD_COUNT
	 * 
	 * @param url
	 */
	public LinkCheckerCore(String url) {
		this(url, Env.getIntegerSetting(Env.DOWNLOAD_MAX_THREAD_COUNT));
	}
	
	/**
	 * Create instance for given URL and max thread number
	 * 
	 * @param url
	 * @param maxThreads
	 */
	public LinkCheckerCore(String urlStr, int maxThreads) {
		super();
		url = normalizeURL(urlStr);
		baseUrl = getBaseURL(url);
    	
		setMaxThreads(maxThreads);
		
		// listeners
		listeners = new ArrayList<CoreStateListener>();
		
		// initialize the file info map
		fileInfoMap = new LinkedHashMap<String, FileInfo>();
		
		// init the tasks map
		fileTasksMap = new HashMap<String, List<DownloadTask>>();
		
		// initialize the thread pool
		downloadThreads = new ArrayList<DownloadThread>(maxThreads);
		for (int i = 0; i < maxThreads; i ++) {
			downloadThreads.add(new DownloadThread(this));
		}
		
		// initialize the task pool
		downloadTasks = new ArrayList<DownloadTask>();
	}
	
	/**
	 * Create a temporary file
	 * 
	 * @return
	 */
	private File createTemporaryFile() {
		try {
			final File tmpFile = File.createTempFile("LinkChk", ".tmp");
			tmpFile.deleteOnExit();
			return tmpFile;
		} catch (IOException e) {
			Env.getLogger().severe("Can not create temporary file.");
		}
		return null;
	}
	
	/**
	 * Return the URL to be downloaded and checked
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Return a well formatted URL string
	 * 
	 * @param urlStr
	 * @return
	 */
	private String normalizeURL(String urlStr) {
		final Matcher m = PROTOCOL_REG.matcher(urlStr);
		if (!m.find()) {
			return "http://" + urlStr;
		}
		return urlStr;
	}
	
	/**
	 * Return the base URL for given URL, must end with /
	 * 
	 * @param urlStr
	 * @return
	 */
	private String getBaseURL(String url) {
		if (FILE_REG.matcher(url).matches()) {
			final int lastSlashPos = url.lastIndexOf('/');
			if (lastSlashPos == url.indexOf("://") + 2) {
				return url + "/";
			} else {
				return url.substring(0, lastSlashPos) + "/";
			}
		} else {
			return url.endsWith("/") ? url : url + "/";
		}
	}
	
	/**
	 * Download the given URL, separate the file to certain blocks if needed
	 * 
	 * @param info
	 * @param tarPath
	 */
	private void downloadFile(FileInfo info, String tarPath) {
		if (info.getSize() == -1) {
			// can not split, add single download task
			final DownloadTask task = new DownloadTask(info.getUrl(), tarPath);
			task.setStart(0);
			task.setCurrent(0);
			task.setEnd(-1);
			task.setType(info.getType());
			task.setFileSize(info.getSize());
			addDownloadTask(task);
		} else {
			// calculate the block sizes
			final int minBlockSize = Env.getIntegerSetting(Env.DOWNLOAD_MIN_BLOCK_SIZE);
			long blockSize = (long)Math.ceil((double)info.getSize() / (double)maxThreads);
			if (blockSize < minBlockSize) {
				blockSize = minBlockSize;
			}
			long start = 0;
			long end = 0;
			while (start <= info.getSize()) {
				final DownloadTask task = new DownloadTask(info.getUrl(), tarPath);
				task.setStart(start);
				task.setCurrent(start);
				end = start + blockSize;
				if (end > info.getSize()) {
					end = info.getSize();
				}
				task.setEnd(end);
				task.setType(info.getType());
				task.setFileSize(info.getSize());
				addDownloadTask(task);
				start = end + 1;
			}
		}
	}
	
	/**
	 * Try to get the basic information of remote file
	 * Will retry FILE_INFO_QUERY_MAX_RETRIES times at most
	 * 
	 * @param urlStr
	 * 		the URL of the remote file
	 * @param callback
	 * 		if callback is null, the query process will block the current thread.
	 * 		otherwise a new thread will be created to perform the query, and the callback will be invoked when query is finished.
	 * @return
	 */
	private FileInfo getRemoteFileInfo(final String urlStr, final FileInfoCallback callback) {
		final FileInfo info = fileInfoMap.containsKey(urlStr) ? fileInfoMap.get(urlStr) : new FileInfo(urlStr);
		final Runnable proc = new Runnable() {
			public void run() {
				final int maxRetries = Env.getIntegerSetting(Env.FILE_INFO_QUERY_MAX_RETRIES);
				for (int retryCount = 0; retryCount < maxRetries; retryCount ++) {
					try {
						final URLConnection conn = new URL(urlStr).openConnection();
						conn.setConnectTimeout(Env.getIntegerSetting(Env.CONNECTION_TIMEOUT));
						conn.setReadTimeout(Env.getIntegerSetting(Env.READ_TIMEOUT));
						if (conn instanceof HttpURLConnection) {
							final HttpURLConnection httpConn = (HttpURLConnection)conn;
							try {
								info.setStatusCode(httpConn.getResponseCode());
							} catch (RuntimeException e) {
								info.setStatusCode(-1);
							}
						}
						info.setType(conn.getContentType());
						info.setSize(conn.getContentLength());
						break;
					} catch (IOException ioe) {
						if (retryCount == maxRetries - 1) {
							Env.getLogger().severe("Can not get the size of remote file: " + urlStr);
						} else {
							Env.getLogger().severe("Get remote file size failed, retrying...");
						}
					}
				}
				if (callback != null) {
					callback.fileInfoReceived(info);
				}
			}
		};
		if (callback == null) {
			proc.run();
		} else {
			new Thread(proc).start();
		}
        return info;
	}
	
	/**
	 * Check the links in the page that stored in given file path
	 * 
	 * @param filePath
	 */
	private void checkLinks(String filePath) {
		try {
			// find out all links
			final byte[] buffer = new byte[(int) new File(filePath).length()];
			final BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
			f.read(buffer);
			final String htmlContent = new String(buffer);
			final Matcher m = LINK_REG.matcher(htmlContent);
			final Set<String> linkSet = new HashSet<String>();
	        while (m.find()) {
	        	final String link = m.group(1);
	        	try {
					final URI uri = new URI(link);
					String absLink = link;
					if (!uri.isAbsolute()) {
						if (link.startsWith("/")) {
							absLink = baseUrl + link.substring(1);
						} else {
							absLink = baseUrl + link;
						}
					}
					if (absLink.toLowerCase().startsWith("http")) {
						linkSet.add(absLink);
						fileInfoMap.put(absLink, new FileInfo(absLink));
					} else {
						// ignore non HTTP URL
						Env.getLogger().warning("Ignore non-http URL: " + absLink);
					}
				} catch (Exception e) {
					// ignore malformed URL
					Env.getLogger().warning("Ignore malformed URL: " + link);
				}
	        }
	        // try to download the links in page
	        for (String link : linkSet) {
	        	final File tmpFile = createTemporaryFile();
	    		if (tmpFile != null) {
	    			getRemoteFileInfo(link, new FileInfoCallback() {
						public void fileInfoReceived(FileInfo info) {
							if (info.getStatusCode() >= 200 && info.getStatusCode() < 300) {
								// download the file for 2xx status code
								downloadFile(info, tmpFile.getAbsolutePath());
								Env.getLogger().info("Check link: " + info.getUrl());
							} else {
								// the link is broken
								info.setState(State.FAILED);
								for (CoreStateListener l : listeners) {
									l.linkCheckFailed(info);
								}
							}
						}
	    			});
	    		}
	        }
	        // if no link found
	        if (linkSet.size() == 0) {
	        	for (CoreStateListener l : listeners) {
					l.noLinkFound();
				}
	        }
		} catch (FileNotFoundException e) {
			Env.getLogger().severe("Can not load file: " + filePath);
		} catch (Exception e) {
			Env.getLogger().severe(e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Return the max thread number
	 * 
	 * @return
	 */
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * Set max thread number
	 * 
	 * @param maxThreads
	 * 		must be 1~20
	 */
	public void setMaxThreads(int maxThreads) {
		if (maxThreads >= 1 && maxThreads <= 20) {
			this.maxThreads = maxThreads;
			Env.putSetting(Env.DOWNLOAD_MAX_THREAD_COUNT, maxThreads);
		} else {
			Env.getLogger().warning("Max threads number should between 1 to 20.  The new value " + maxThreads + " is ignored.");
		}
	}
	
	/**
	 * Add LinkStateListener
	 * 
	 * @param l
	 */
	public void addCoreStateListener(CoreStateListener l) {
		listeners.add(l);
	}
	
	/**
	 * Remove LinkStateListener
	 * 
	 * @param l
	 */
	public void removeLinkStateListener(CoreStateListener l) {
		listeners.remove(l);
	}
	
	/**
	 * remove all LinkStateListener instances
	 */
	public void removeAllLinkStateListeners() {
		listeners.clear();
	}

	/**
	 * Start to down the URL and check links
	 */
	public synchronized void start() {
		elapsedTime = 0;
		startTime = System.currentTimeMillis();
		running = true;
		// try to download the main URL
		final File tmpFile = createTemporaryFile();
		if (tmpFile != null) {
			final FileInfo info = getRemoteFileInfo(url, new FileInfoCallback() {
				public void fileInfoReceived(FileInfo info) {
					// the input URL is broken, stop working
					if (info.getStatusCode() < 200 || info.getStatusCode() >= 300) {
						for (CoreStateListener l : listeners) {
							l.inputURLBroken();
						}
						stop();
					}
				}
			});
			// for main URL, download it directly
			info.setType("text/html");
			downloadFile(info, tmpFile.getAbsolutePath());
		}
		// start the download threads
		for (DownloadThread thread : downloadThreads) {
			thread.start();
		}
	}
	
	/**
	 * Stop links checking
	 */
	public synchronized void stop() {
		elapsedTime = System.currentTimeMillis() - startTime;
		running = false;
		for (DownloadThread thread : downloadThreads) {
			thread.interrupt();
		}
	}
	
	/**
	 * Return the total number of links to be checked
	 * 
	 * @return
	 */
	public int getTotalLinksCount() {
		return fileInfoMap.size();
	}
	
	/**
	 * Return the number of broken links
	 * 
	 * @return
	 */
	public int getBrokenLinksCount() {
		int count = 0;
		for (FileInfo info : fileInfoMap.values()) {
			if (info.getState() == State.FAILED) {
				count ++;
			}
		}
		return count;
	}
	
	/**
	 * Return the number of broken links
	 * 
	 * @return
	 */
	public int getWorkingLinksCount() {
		int count = 0;
		for (FileInfo info : fileInfoMap.values()) {
			if (info.getState() == State.DOWNLOADED) {
				count ++;
			}
		}
		return count;
	}
	
	/**
	 * Return the number of links that are currently checking
	 * 
	 * @return
	 */
	public int getCheckingLinksCount() {
		final Set<String> links = new HashSet<String>();
		for (DownloadThread thread : downloadThreads) {
			final DownloadTask task = thread.getTask();
			if (task != null) {
				links.add(task.getUrl());
			}
		}
		return links.size();
	}
	
	/**
	 * Get the elapsed time for downloading/checking
	 * 
	 * @return
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Get the size of data that has been downloaded
	 * 
	 * @return
	 */
	public long getDownloadSize() {
		return downloadSize;
	}

	/**
	 * Get the map that store all files' info
	 * 
	 * @return
	 */
	public Map<String, FileInfo> getFileInfoMap() {
		return fileInfoMap;
	}

	///////////////////////////////////////////////////////////////////////////
	// DownloadManager
	///////////////////////////////////////////////////////////////////////////
	public void addDownloadTask(DownloadTask task) {
		synchronized (downloadTasks) {
			downloadTasks.add(task);
			List<DownloadTask> tasks = fileTasksMap.get(task.getUrl());
			if (tasks == null) {
				tasks = new ArrayList<DownloadTask>();
				fileTasksMap.put(task.getUrl(), tasks);
			}
			if (!tasks.contains(task)) {
				tasks.add(task);
			}
			downloadTasks.notifyAll();
		}
	}

	public boolean hasIdleDownloadThread() {
		for (DownloadThread thread : downloadThreads) {
			if (!thread.isRunningTask()) {
				return true;
			}
		}
		return false;
	}

	public boolean isRunning() {
		return running;
	}

	public void onDataDownloaded(DownloadTask task, int dataLength) {
		elapsedTime = System.currentTimeMillis() - startTime;
		downloadSize += dataLength;
		final FileInfo info = fileInfoMap.get(task.getUrl());
		if (info != null) {
			info.setDownloadedSize(info.getDownloadedSize() + dataLength);
			info.setUsedTime(System.currentTimeMillis() - info.getStartTime());
			for (CoreStateListener l : listeners) {
				l.linkDownloading(info);
			}
		}
	}

	public void onDownloadTaskFinished(DownloadTask task) {
		elapsedTime = System.currentTimeMillis() - startTime;
		List<DownloadTask> tasks = fileTasksMap.get(task.getUrl());
		if (tasks == null) {
			tasks = new ArrayList<DownloadTask>();
			fileTasksMap.put(task.getUrl(), tasks);
		}
		tasks.remove(task);
		if (tasks.size() == 0) {
			// all tasks for given URL is finished, file is done
			if (task.getUrl().equals(url)) {
				// the main URL is downloaded, check links inside
				if (task.getType().toLowerCase().contains("text/html")) {
					Env.getLogger().info("URL is downloaded, checking the links inside...");
					checkLinks(task.getTarPath());
				}
			} else {
				// the link is downloaded, update its state
				final FileInfo info = fileInfoMap.get(task.getUrl());
				if (info != null) {
					// download finished
					info.setState(State.DOWNLOADED);
					if (info.getSize() != -1) {
						info.setDownloadedSize(info.getSize());
					}
					info.setUsedTime(System.currentTimeMillis() - info.getStartTime());
					Env.getLogger().info("URL \"" + info.getUrl() + "\" is checked OK.");
					for (CoreStateListener l : listeners) {
    					l.linkCheckPassed(info);
    				}
				}
				// stop if all links are checked
				boolean canStop = true;
				for (FileInfo fileInfo : fileInfoMap.values()) {
					if (fileInfo.getState() == State.INDETERMINATED) {
						canStop = false;
						break;
					}
				}
				if (canStop) {
					Env.getLogger().info("All links are checked.");
					for (CoreStateListener l : listeners) {
						l.allLinksChecked();
					}
					stop();
				}
			}
		}
	}

	public void onDownloadTerminated(DownloadTask task) {
		elapsedTime = System.currentTimeMillis() - startTime;
		final FileInfo info = fileInfoMap.get(task.getUrl());
		if (info != null) {
			// download failed
			info.setState(State.FAILED);
			info.setUsedTime(System.currentTimeMillis() - info.getStartTime());
			for (CoreStateListener l : listeners) {
				l.linkCheckFailed(info);
			}
		}
	}

	public DownloadTask requestForDownloadTask() {
		synchronized (downloadTasks) {
			while (running && downloadTasks.size() == 0) {
				try {
					downloadTasks.wait();
				} catch (InterruptedException e) {
					return null;
				}
			}
			final DownloadTask task = downloadTasks.remove(0);
			final FileInfo info = fileInfoMap.get(task.getUrl());
			if (info != null) {
				if (info.getStartTime() == 0) {
					info.setStartTime(System.currentTimeMillis());
				}
			}
			return task;
		}
	}
	///////////////////////////////////////////////////////////////////////////
	// End of DownloadManager
	///////////////////////////////////////////////////////////////////////////
}
