/**
 *  A utility class to offer useful static fields and methods globally
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

public class Env {

	public static final String CONNECTION_TIMEOUT = "connection.timeout";
	public static final String READ_TIMEOUT = "read.timeout";
	public static final String DOWNLOAD_MAX_THREAD_COUNT = "download.max.thread.count";
	public static final String DOWNLOAD_MIN_BLOCK_SIZE = "download.min.block.size";
	public static final String DOWNLOAD_REMAIN_SPLIT_THRESHOLD = "download.remain.split.threshold";
	public static final String FILE_INFO_QUERY_MAX_RETRIES = "file.info.query.max.retries";
	public static final String MAX_FAILURES_COUNT = "max.failures.count";

	private static final Logger LOGGER = Logger.getLogger(Env.class.getName());
	
	private static ResourceBundle RESOURCE_BUNDLE = null;
	static {
		try {
			final String language = System.getProperty("user.language");
			final String country = System.getProperty("user.country");
			RESOURCE_BUNDLE = ResourceBundle.getBundle("xuan.yun.res.resource", new Locale(language, country));
		} catch (Exception e) {
			LOGGER.severe("Can not find resource bundle.");
		} 
	}
	
	private static Properties settings = new Properties(getDefaultSettings());
	static {
		try {
			File settingsFile = new File(getCurrentDirectory(), "config.xml");
			if (!settingsFile.exists()) {
				settingsFile = new File(getCurrentDirectory(), "config.xml");
			}
			final InputStream fis = new FileInputStream(settingsFile);
			settings.loadFromXML(fis);
		} catch (Exception e) {
			// ignore if the file does not existed, default settings will be used
		}
	}
	
	private static final Map<String, ImageIcon> IMAGE_MAP = new HashMap<String, ImageIcon>();
	
	/**
	 * Get image from resource
	 * 
	 * @param fileName
	 * @return
	 */
	public static ImageIcon getImageIcon(String fileName) {
		synchronized (IMAGE_MAP) {
			ImageIcon img = IMAGE_MAP.get(fileName);
			if (img == null) {
				final StringBuilder imgPathBuf = new StringBuilder();
				imgPathBuf.append("/xuan/yun/res/img/");
				imgPathBuf.append(fileName);
				img = new ImageIcon(Env.class.getResource(imgPathBuf.toString()));
				IMAGE_MAP.put(fileName, img);
			}
			return img;
		}
	}
	
	/**
	 * Return the resource bundle
	 * 
	 * @return
	 */
	public static ResourceBundle getResourceBundle() {
		return RESOURCE_BUNDLE;
	}
	
	/**
	 * Get the current directory
	 */
	public static File getCurrentDirectory() {
		try {
			final String myJarPath = Env.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			return new File(myJarPath).getParentFile();
		} catch (URISyntaxException e) {
			LOGGER.severe(e.getMessage());
		}
		return new File("");
	}
	
	private static Properties getDefaultSettings() {
		final Properties defaults = new Properties();
		defaults.put(CONNECTION_TIMEOUT, "10000");
		defaults.put(READ_TIMEOUT, "10000");
		defaults.put(DOWNLOAD_MAX_THREAD_COUNT, "5");
		defaults.put(DOWNLOAD_MIN_BLOCK_SIZE, "65535");
		defaults.put(DOWNLOAD_REMAIN_SPLIT_THRESHOLD, "65535");
		defaults.put(FILE_INFO_QUERY_MAX_RETRIES, "3");
		defaults.put(MAX_FAILURES_COUNT, "5");
		return defaults;
	}
	
	public static int getIntegerSetting(String key) {
		try {
			return Integer.parseInt(settings.getProperty(key));
		} catch (NumberFormatException e) {
			LOGGER.severe("Get parameter '" + key + "' failed: " + e.getMessage());
			return -1;
		}
	}
	
	public static void putSetting(String key, Object value) {
		settings.put(key, value.toString());
	}
	
	public static Logger getLogger() {
		return LOGGER;
	}
	
	/**
	 * Try to open url in web browser
	 * 
	 * @param url
	 * @throws Exception
	 */
	public static void openURL(String url) throws Exception {
		final String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("mac os")) {
			final Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
			final Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
			openURL.invoke(null, new Object[] { url });
		} else if (osName.startsWith("windows"))
			Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
		else {
			final String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
			String browser = null;
			for (int count = 0; count < browsers.length && browser == null; count++) {
				if (Runtime.getRuntime().exec(new String[] {"which", browsers[count]}).waitFor() == 0) {
					browser = browsers[count];
				}
			}
			if (browser == null) {
				throw new Exception("Could not find web browser");
			} else {
				Runtime.getRuntime().exec(new String[] { browser, url });
			}
		}
	}
}
