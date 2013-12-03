/*
 * Created on 2013/11/28
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.ohoooo.BZBinSearch;

/**
 * @author Suguru Oho
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public interface BinComparator {
	public static final int BEFORE = -1;
	public static final int IN = 0;
	public static final int AFTER = 1;
	public int compare(long value);
}
