/**
 *  The main frame of Link Checker GUI
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import xuan.yun.core.Env;
import xuan.yun.core.LinkCheckerCore;
import xuan.yun.core.intf.LinkChecker;

public class LinkCheckerFrame extends JFrame implements LinkChecker {

	private static final long serialVersionUID = 1L;
	
	private JTextField urlEdit;
	
	private JSpinner maxThreadEdit;
	
	final JTabbedPane viewTabs;

	public LinkCheckerFrame() throws HeadlessException {
		super(Env.getResourceBundle().getString("gui.frame.title"));
		
		final JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
		setContentPane(mainPanel);
		
		// Input fields
		{
			final JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
			mainPanel.add(inputPanel, BorderLayout.NORTH);
			
			inputPanel.add(new JLabel(Env.getResourceBundle().getString("gui.input.url")));
			
			urlEdit = new JTextField(30);
			urlEdit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					checkLinks(urlEdit.getText().trim());
				}
			});
			inputPanel.add(urlEdit);
			
			inputPanel.add(new JLabel(Env.getResourceBundle().getString("gui.input.max.thread")));
			
			maxThreadEdit = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
			inputPanel.add(maxThreadEdit);
			
			final JButton checkButton = new JButton(Env.getResourceBundle().getString("gui.input.check.links"));
			checkButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					checkLinks(urlEdit.getText().trim());
				}
			});
			inputPanel.add(checkButton);
		}
		
		// View tabs
		{
			viewTabs = new JTabbedPane() {
				private static final long serialVersionUID = 1L;
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);
					final Color bkColor = g.getColor();
					g.setColor(Color.DARK_GRAY);
					final String tips = Env.getResourceBundle().getString("gui.view.empty.tips");
					final int textWidth = g.getFontMetrics().stringWidth(tips);
					g.drawString(tips, (getWidth() - textWidth) / 2, getHeight() / 2);
					g.setColor(bkColor);
				}
			};
			viewTabs.setPreferredSize(new Dimension(900, 600));
			mainPanel.add(viewTabs, BorderLayout.CENTER);
		}
		
		pack();
	}

	///////////////////////////////////////////////////////////////////////////
	// LinkChecker
	///////////////////////////////////////////////////////////////////////////
	public void checkLinks(String url) {
		if (url != null && url.length() > 0) {
			final LinkCheckerCore core = new LinkCheckerCore(url, (Integer)maxThreadEdit.getValue());
			final LinkCheckerView view = new LinkCheckerView(core, this);
			final int newTabIndex = viewTabs.getTabCount();
			viewTabs.addTab(url, view);
			viewTabs.setSelectedIndex(newTabIndex);
			core.start();
		}
	}
	///////////////////////////////////////////////////////////////////////////
	// End of LinkChecker
	///////////////////////////////////////////////////////////////////////////
}
