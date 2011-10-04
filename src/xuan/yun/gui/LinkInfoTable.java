/**
 *  Table to display the status of links
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import xuan.yun.core.Env;
import xuan.yun.core.FileInfo;
import xuan.yun.core.LinkCheckerCore;
import xuan.yun.core.FileInfo.State;
import xuan.yun.core.intf.LinkChecker;

public class LinkInfoTable extends JTable {

	private static final long serialVersionUID = 1L;
	
	private Map<String, JComponent> actionPanesMap;
	
	private LinkChecker checker;

	public LinkInfoTable(LinkCheckerCore core, LinkChecker chk) {
		super(new LinkInfoTableModel(core));

		checker = chk;
		
		actionPanesMap = new HashMap<String, JComponent>();
		
		setRowHeight(30);
		
		// auto edit action cell (show action links)
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				final int hoverRow = rowAtPoint(e.getPoint());
				if (getEditingRow() != hoverRow) {
					final TableCellEditor editor = getCellEditor();
					if (editor != null) {
						editor.stopCellEditing();
					}
					editCellAt(hoverRow, 7);
				}
			}
		});
		
		final JTableHeader header = getTableHeader();
		header.setPreferredSize(new Dimension(30, 30));
		
		// reorder columns allowed
		header.setReorderingAllowed(true);
		
		// sort rows supported (for JRE6+)
		try {
			final Class<?> sorterCls = Class.forName("javax.swing.table.TableRowSorter");
			final Constructor constructor = sorterCls.getConstructor(new Class[]{TableModel.class});
			final Object sorter = constructor.newInstance(new Object[]{getModel()});
			final Method setter = getClass().getMethod("setRowSorter", new Class[]{Class.forName("javax.swing.RowSorter")});
			setter.invoke(this, new Object[]{sorter});
		} catch (Exception e) {
			Env.getLogger().warning("JRE version is lower than 6, table sorting is disabled.");
		}
		
		// custom columns
		final TableColumnModel cm = getColumnModel();
		cm.getColumn(0).setCellRenderer(new StateRenderer());
		cm.getColumn(0).setMaxWidth(80);
		cm.getColumn(1).setMinWidth(200);
		cm.getColumn(2).setCellRenderer(new CenteredRenderer());
		cm.getColumn(2).setMaxWidth(80);
		cm.getColumn(4).setCellRenderer(new SizeRenderer());
		cm.getColumn(5).setCellRenderer(new SizeRenderer());
		cm.getColumn(6).setCellRenderer(new TimeRenderer());
		cm.getColumn(6).setMaxWidth(100);
		cm.getColumn(7).setCellRenderer(new ActionRenderer());
		cm.getColumn(7).setCellEditor(new ActionEditor());
		cm.getColumn(7).setMinWidth(180);
	}
	
	/**
	 * Renderer for fields that need to be centered
	 */
	private static class CenteredRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setHorizontalAlignment(SwingConstants.CENTER);
			return this;
		}
	}
	
	/**
	 * Renderer for state column
	 */
	private static class StateRenderer extends CenteredRenderer {
		private static final long serialVersionUID = 1L;
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof State) {
				setText("");
				final State state = (State)value;
				if (state == State.FAILED) {
					setIcon(Env.getImageIcon("failed.png"));
				} else if (state == State.DOWNLOADED) {
					setIcon(Env.getImageIcon("downloaded.png"));
				} else {
					setIcon(Env.getImageIcon("indeterminated.png"));
				}
			}
			return this;
		}
	}
	
	/**
	 * Renderer for size column
	 */
	private static class SizeRenderer extends CenteredRenderer {
		private static final long serialVersionUID = 1L;
		private static final NumberFormat SIZE_FORMAT = new DecimalFormat("#,###,###,###,###");
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Long) {
				final long size = (Long)value;
				if (size == -1) {
					setText(Env.getResourceBundle().getString("gui.view.table.format.size.unknown"));
				} else {
					setText(Env.getResourceBundle().getString("gui.view.table.format.size.bytes")
							.replaceFirst("__size__", SIZE_FORMAT.format(size)));
				}
			}
			return this;
		}
	}
	
	/**
	 * Renderer for used time column
	 */
	private static class TimeRenderer extends CenteredRenderer {
		private static final long serialVersionUID = 1L;
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Long) {
				final long usedTime = (Long)value;
				final long hour = usedTime / 3600000;
				final long min = (usedTime % 3600000) / 60000;
				final long sec = (usedTime % 60000) / 1000;
				setText(String.format("%02d:%02d:%02d", hour, min, sec));
			}
			return this;
		}
	}
	
	/**
	 * Renderer for action column
	 */
	private class ActionRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			return getActionPaneForRow(row);
		}
	}
	
	/**
	 * Editor for action column (show action links)
	 */
	private class ActionEditor implements TableCellEditor  {
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			return getActionPaneForRow(row);
		}

		public void addCellEditorListener(CellEditorListener l) {

		}
		
		public void removeCellEditorListener(CellEditorListener l) {
			
		}

		public void cancelCellEditing() {

		}

		public Object getCellEditorValue() {
			return null;
		}

		public boolean isCellEditable(EventObject anEvent) {
			return true;
		}

		public boolean shouldSelectCell(EventObject anEvent) {
			return true;
		}

		public boolean stopCellEditing() {
			return true;
		}
	}
	
	private JComponent getActionPaneForRow(int row) {
		int actualRow = row;
		try {
			final Method converter = getClass().getMethod("convertRowIndexToModel", new Class[]{int.class});
			actualRow = (Integer)converter.invoke(this, new Object[]{row});
		} catch (Exception e) {
			// JRE version is lower than 6, ignore
		}
		final FileInfo info = (FileInfo)getModel().getValueAt(actualRow, -1);
		
		JComponent pane = actionPanesMap.get(info.getUrl());
		if (pane == null) {
			pane = new JPanel(new FlowLayout(FlowLayout.CENTER));
			pane.setOpaque(false);
			actionPanesMap.put(info.getUrl(), pane);
		}
		final Object status = pane.getClientProperty("status");
		if (status != info.getState()) {
			pane.removeAll();
			final HyperLink copyLink = new HyperLink(Env.getResourceBundle().getString("gui.view.action.copy"));
			copyLink.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(new StringSelection(info.getUrl()), null);
				}
			});
			pane.add(copyLink);
			if (info.getState() == State.DOWNLOADED) {
				final HyperLink openLink = new HyperLink(Env.getResourceBundle().getString("gui.view.action.open"));
				openLink.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							Env.openURL(info.getUrl());
						} catch (Exception ex) {
							Env.getLogger().warning("Could not open URL in browser. " + ex.getMessage());
						}
					}
				});
				pane.add(openLink);
				if (info.getType() != null && info.getType().toLowerCase().contains("text/html")) {
					final HyperLink checkLink = new HyperLink(Env.getResourceBundle().getString("gui.view.action.check"));
					checkLink.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (checker != null) {
								checker.checkLinks(info.getUrl());
							}
						}
					});
					pane.add(checkLink);
				}
			}
			pane.putClientProperty("status", info.getState());
		}
		return pane;
	}
}
