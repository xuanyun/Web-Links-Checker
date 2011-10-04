/**
 *  The view to display the report of links checking
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import xuan.yun.core.Env;
import xuan.yun.core.FileInfo;
import xuan.yun.core.LinkCheckerCore;
import xuan.yun.core.intf.CoreStateListener;
import xuan.yun.core.intf.LinkChecker;

public class LinkCheckerView extends JComponent {

	private static final long serialVersionUID = 1L;
	
	private final LinkCheckerCore core;
	
	private ProgressPie pie;
	
	private JLabel progressLabel;
	private JLabel totalLinksLabel;
	private JLabel brokenLinksLabel;
	private JLabel workingLinksLabel;
	private JLabel downloadSizeLabel;
	private JLabel downloadTimeLabel;
	
	private LinkInfoTable infoTable;

	public LinkCheckerView(final LinkCheckerCore core, final LinkChecker checker) {
		super();
		this.core = core;
		core.addCoreStateListener(new CoreStateListener() {
			public void inputURLBroken() {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(LinkCheckerView.this), 
						Env.getResourceBundle().getString("gui.view.prompt.input.url.broken").replaceFirst("__url__", core.getUrl()), 
						Env.getResourceBundle().getString("gui.view.prompt.title.error"), 
						JOptionPane.ERROR_MESSAGE);
			}
			public void noLinkFound() {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(LinkCheckerView.this), 
						Env.getResourceBundle().getString("gui.view.prompt.no.link.found").replaceFirst("__url__", core.getUrl()), 
						Env.getResourceBundle().getString("gui.view.prompt.title.error"), 
						JOptionPane.ERROR_MESSAGE);
			}
			public void linkDownloading(FileInfo info) {
				updateView();
			}
			public void linkCheckFailed(FileInfo info) {
				updateView();
			}
			public void linkCheckPassed(FileInfo info) {
				updateView();
			}
			public void allLinksChecked() {
				updateView();
			}
		});
		
		setLayout(new BorderLayout(5, 5));
		{
			// Overview
			final JPanel overviewPanel = new JPanel(new BorderLayout(50, 5));
			overviewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(overviewPanel, BorderLayout.NORTH);
			{
				final JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
				urlPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
				overviewPanel.add(urlPanel, BorderLayout.NORTH);

				final JLabel urlLabel = new JLabel(core.getUrl());
				urlPanel.add(urlLabel);
				
				final HyperLink copyLink = new HyperLink(Env.getResourceBundle().getString("gui.view.action.copy"));
				copyLink.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(new StringSelection(core.getUrl()), null);
					}
				});
				urlPanel.add(copyLink);
				
				final HyperLink openLink = new HyperLink(Env.getResourceBundle().getString("gui.view.action.open"));
				openLink.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							Env.openURL(core.getUrl());
						} catch (Exception ex) {
							Env.getLogger().warning("Could not open URL in browser. " + ex.getMessage());
						}
					}
				});
				urlPanel.add(openLink);
			}
			{
				pie = new ProgressPie();
				pie.setPreferredSize(new Dimension(150, 100));
				overviewPanel.add(pie, BorderLayout.WEST);
			}
			{
				final JPanel itemsPanel = new JPanel(new GridLayout(3, 2));
				overviewPanel.add(itemsPanel, BorderLayout.CENTER);
				
				progressLabel = new JLabel();
				itemsPanel.add(progressLabel);
				
				totalLinksLabel = new JLabel();
				itemsPanel.add(totalLinksLabel);

				downloadSizeLabel = new JLabel();
				itemsPanel.add(downloadSizeLabel);
				
				workingLinksLabel = new JLabel();
				itemsPanel.add(workingLinksLabel);

				downloadTimeLabel = new JLabel();
				itemsPanel.add(downloadTimeLabel);
				
				brokenLinksLabel = new JLabel();
				itemsPanel.add(brokenLinksLabel);
			}
			
			// Link Info Table
			infoTable = new LinkInfoTable(core, checker);
			final JScrollPane scroller = new JScrollPane(infoTable);
			add(scroller, BorderLayout.CENTER);
		}
		updateView();
	}
	
	/**
	 * Call this method to update the view display
	 */
	private void updateView() {
		final int brokenLinksCount = core.getBrokenLinksCount();
		final int workingLinksCount = core.getWorkingLinksCount();
		final int checkingLinksCount = core.getCheckingLinksCount();
		final int totalLinksCount = core.getTotalLinksCount();
		final long downloadSize = core.getDownloadSize();
		final long downloadTime = core.getElapsedTime();
		final long hour = downloadTime / 3600000;
		final long min = (downloadTime % 3600000) / 60000;
		final long sec = (downloadTime % 60000) / 1000;

		pie.setTotalLinks(totalLinksCount);
		pie.setWorkingLinks(workingLinksCount);
		pie.setBrokenLinks(brokenLinksCount);
		pie.setCheckingLinks(checkingLinksCount);
		pie.repaint();
		
		progressLabel.setText(Env.getResourceBundle().getString("gui.view.info.progress")
					.replaceFirst("__percent__", totalLinksCount == 0 ? "0" : String.valueOf((brokenLinksCount + workingLinksCount) * 100 / totalLinksCount)));
		totalLinksLabel.setText(Env.getResourceBundle().getString("gui.view.info.total")
				.replaceFirst("__total__", String.valueOf(totalLinksCount)));
		workingLinksLabel.setText(Env.getResourceBundle().getString("gui.view.info.working")
				.replaceFirst("__working__", String.valueOf(workingLinksCount)));
		brokenLinksLabel.setText(Env.getResourceBundle().getString("gui.view.info.broken")
				.replaceFirst("__broken__", String.valueOf(brokenLinksCount)));
		downloadSizeLabel.setText(Env.getResourceBundle().getString("gui.view.info.download.size")
				.replaceFirst("__size__", String.valueOf(downloadSize)));
		downloadTimeLabel.setText(Env.getResourceBundle().getString("gui.view.info.download.time")
				.replaceFirst("__time__", String.format("%02d:%02d:%02d", hour, min, sec)));
		
		final LinkInfoTableModel model = (LinkInfoTableModel)infoTable.getModel();
		model.fireTableDataChanged();
	}
}
