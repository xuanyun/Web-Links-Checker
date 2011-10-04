/**
 *  Main class for command line version of Link Checker
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import xuan.yun.core.Env;
import xuan.yun.core.FileInfo;
import xuan.yun.core.LinkCheckerCore;
import xuan.yun.core.intf.CoreStateListener;

public class LinkChecker {

	/**
	 * Launch the command line version of Link Checker
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			
			// set log level
			Env.getLogger().setLevel(Level.WARNING);
			
			// the list to store the broken links
			final List<String> brokenLinks = new ArrayList<String>();
			
			// create and start the core
			final LinkCheckerCore core = new LinkCheckerCore(args[0]);
			core.addCoreStateListener(new CoreStateListener() {
				private void showProgress() {
					System.out.print(String.format(Env.getResourceBundle().getString("command.line.progress.format"), 
							core.getWorkingLinksCount() + core.getBrokenLinksCount(), core.getTotalLinksCount()));
				}
				public void linkCheckFailed(FileInfo info) {
					brokenLinks.add(info.getUrl());
					showProgress();
				}
				public void linkCheckPassed(FileInfo info) {
					showProgress();
				}
				public void linkDownloading(FileInfo info) {
					// data is downloading, do nothing in command line mode
				}
				public void allLinksChecked() {
					if (brokenLinks.size() == 0) {
						System.out.println(Env.getResourceBundle().getString("command.line.result.no.broken.link"));
					} else {
						System.out.println(Env.getResourceBundle().getString("command.line.result.found"));
						for (String link : brokenLinks) {
							System.out.println(link);
						}
					}
				}
				public void inputURLBroken() {
					System.out.println(Env.getResourceBundle().getString("command.line.result.input.url.broken"));
				}
				public void noLinkFound() {
					System.out.println(Env.getResourceBundle().getString("command.line.result.no.link.found"));
				}
			});
			core.start();
		} else {
			System.out.println(Env.getResourceBundle().getString("command.line.prompt.usage"));
		}
	}
}
