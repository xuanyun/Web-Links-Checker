/**
 *  Pie progress indicator
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

public class ProgressPie extends JComponent {

	private static final long serialVersionUID = 1L;

	private static final Color COLOR_WORKING_LINK = new Color(128, 255, 128);
	
	private static final Color COLOR_BROKEN_LINK = new Color(255, 128, 128);
	
	private static final Color COLOR_CHECKING_LINK = new Color(255, 255, 128);
	
	private int totalLinks;
	
	private int workingLinks;
	
	private int brokenLinks;
	
	private int checkingLinks;

	public ProgressPie() {
		super();
	}

	public int getBrokenLinks() {
		return brokenLinks;
	}

	public void setBrokenLinks(int brokenLinks) {
		this.brokenLinks = brokenLinks;
	}

	public int getCheckingLinks() {
		return checkingLinks;
	}

	public void setCheckingLinks(int checkingLinks) {
		this.checkingLinks = checkingLinks;
	}

	public int getTotalLinks() {
		return totalLinks;
	}

	public void setTotalLinks(int totalLinks) {
		this.totalLinks = totalLinks;
	}

	public int getWorkingLinks() {
		return workingLinks;
	}

	public void setWorkingLinks(int workingLinks) {
		this.workingLinks = workingLinks;
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		final Graphics2D g2d = (Graphics2D)g;
		final RenderingHints bkHints = g2d.getRenderingHints();
		final RenderingHints renderHints = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);   
		g2d.setRenderingHints(renderHints);
		
		final Color bkColor = g.getColor();
		final int w = getWidth() - 8;
		final int h = getHeight() - 12;
		final int x = (getWidth() - w) / 2;
		final int y = (getHeight() - h) / 2 - 2;

		g.setColor(Color.GRAY);
		g.fillOval(x, y + 4, w, h);
		
		g.setColor(Color.WHITE);
		g.fillOval(x, y, w, h);
		
		if (totalLinks > 0) {
			g.setColor(COLOR_WORKING_LINK);
			final int workingLinkAngle = 360 * workingLinks / totalLinks;
			g.fillArc(x, y, w, h, 0, workingLinkAngle);
			
			g.setColor(COLOR_BROKEN_LINK);
			final int brokenLinkAngle = 360 * brokenLinks / totalLinks;
			g.fillArc(x, y, w, h, workingLinkAngle, brokenLinkAngle);
			
			g.setColor(COLOR_CHECKING_LINK);
			final int checkingLinkAngle = 360 * checkingLinks / totalLinks;
			g.fillArc(x, y, w, h, workingLinkAngle + brokenLinkAngle, checkingLinkAngle);
		}
		
		g.setColor(Color.LIGHT_GRAY);
		g.drawOval(x, y, w, h);
		
		g.setColor(bkColor);
		g2d.setRenderingHints(bkHints);
	}
}
