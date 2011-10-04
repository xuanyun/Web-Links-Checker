/**
 *  Table model for LinkInfoTable
 *  
 *  Author: Xuan Yun (gdxuanyun@yahoo.com)
 *  All Rights Reserved
 */
package xuan.yun.gui;

import java.util.LinkedHashMap;

import javax.swing.table.DefaultTableModel;

import xuan.yun.core.Env;
import xuan.yun.core.FileInfo;
import xuan.yun.core.LinkCheckerCore;
import xuan.yun.core.FileInfo.State;

public class LinkInfoTableModel extends DefaultTableModel {

	private static final long serialVersionUID = 1L;
	
	private final String[] COLUMN_NAMES = new String[]{
			Env.getResourceBundle().getString("gui.view.table.column.state"),
			Env.getResourceBundle().getString("gui.view.table.column.url"),
			Env.getResourceBundle().getString("gui.view.table.column.code"),
			Env.getResourceBundle().getString("gui.view.table.column.type"),
			Env.getResourceBundle().getString("gui.view.table.column.size"),
			Env.getResourceBundle().getString("gui.view.table.column.downloaded"),
			Env.getResourceBundle().getString("gui.view.table.column.time"),
			Env.getResourceBundle().getString("gui.view.table.column.action")
		};
		
	private LinkCheckerCore core;
	
	public LinkInfoTableModel(LinkCheckerCore core) {
		super();
		this.core = core;
	}

	public boolean isCellEditable(int row, int column) {
		return getValueAt(row, 0) != State.INDETERMINATED && column == 7;
	}
	
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	public int getRowCount() {
		if (core == null) {
			return 0;
		}
		return core.getTotalLinksCount();
	}

	public Object getValueAt(int row, int column) {
		if (row < 0 || row >= core.getTotalLinksCount()) {
			return null;
		}
		final FileInfo info = getFileInfoByRow(row);
		if (info != null) {
			switch (column) {
			case -1:
				return info;
			case 0:
				return info.getState();
			case 1:
				return info.getUrl();
			case 2:
				return info.getStatusCode();
			case 3:
				return info.getType();
			case 4:
				return info.getSize();
			case 5:
				return info.getDownloadedSize();
			case 6:
				return info.getUsedTime();
			case 7:
				return info.getState();
			}
		}
		return null;
	}
	
	/**
	 * Return the FileInfo object for given row index
	 * 
	 * @param row
	 * @return
	 */
	private FileInfo getFileInfoByRow(int row) {
		final LinkedHashMap<String, FileInfo> map = (LinkedHashMap<String, FileInfo>)core.getFileInfoMap();
		int i = 0;
		FileInfo info = null;
		for (FileInfo infoObj : map.values()) {
			if (i == row) {
				info = infoObj;
				break;
			}
			i ++;
		}
		return info;
	}
}
