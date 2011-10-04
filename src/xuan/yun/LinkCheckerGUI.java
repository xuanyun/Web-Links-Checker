/**
 *  Main class for GUI version of Link Checker
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun;

import java.util.logging.Level;

import javax.swing.UIManager;

import xuan.yun.core.Env;
import xuan.yun.gui.LinkCheckerFrame;

public class LinkCheckerGUI {

	/**
	 * Launch the GUI version of Link Checker
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// set log level
		Env.getLogger().setLevel(Level.WARNING);
		
		// set system look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			Env.getLogger().warning("Look and feel initialization failed. " + e.getMessage());
		}
		
		// create GUI
		final LinkCheckerFrame frame = new LinkCheckerFrame();
		frame.setDefaultCloseOperation(LinkCheckerFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
