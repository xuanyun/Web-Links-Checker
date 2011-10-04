/**
 *  Hyperlink button
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;

public class HyperLink extends JButton {

	private static final long serialVersionUID = 1L;
	
	private static final Color TEXT_COLOR = Color.BLUE;

	private Font textFont;
	
	private Font underlineFont;
	
	public HyperLink() {
		super();
		initStyle();
	}

	public HyperLink(Action a) {
		super(a);
		initStyle();
	}

	public HyperLink(Icon icon) {
		super(icon);
		initStyle();
	}

	public HyperLink(String text, Icon icon) {
		super(text, icon);
		initStyle();
	}

	public HyperLink(String text) {
		super(text);
		initStyle();
	}
	
	private void initStyle() {
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		setBorderPainted(false);
		setContentAreaFilled(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setForeground(TEXT_COLOR);
		
		textFont = getFont();
		Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
		map.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_LIGHT);
		underlineFont = textFont.deriveFont(map);
		setFont(underlineFont);
		
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				textFont = getFont();
				Map<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
				map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				underlineFont = textFont.deriveFont(map);
				setFont(underlineFont);
			}
			public void mouseExited(MouseEvent e) {
				setFont(textFont);
			}
		});
	}
}
