/*
 * 02/17/2009
 *
 * IconRowHeader.java - Renders icons in the gutter.
 * Copyright (C) 2009 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 */
package org.fife.ui.rtextarea;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.ayound.ext.BookMarkListenerManager;
import org.ayound.ext.IBookMarkListener;


/**
 * Renderes icons in the {@link Gutter}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class IconRowHeader extends AbstractGutterComponent implements MouseListener {

	/**
	 * The icons to render.
	 */
	private List trackingIcons;

	/**
	 * The width of this component.
	 */
	private int width;

	/**
	 * Whether this component listens for mouse clicks and toggles "bookmark"
	 * icons on them.
	 */
	private boolean bookmarkingEnabled;

	/**
	 * The icon to use for bookmarks.
	 */
	private Icon bookmarkIcon;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	private Rectangle visibleRect;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	private Insets textAreaInsets;


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public IconRowHeader(RTextArea textArea) {
		super(textArea);
		visibleRect = new Rectangle();
		width = 16;
		addMouseListener(this);
	}


	/**
	 * Adds an icon that tracks an offset in the document, and is displayed
	 * adjacent to the line numbers.  This is useful for marking things such
	 * as source code errors.
	 *
	 * @param offs The offset to track.
	 * @param icon The icon to display.  This should be small (say 16x16).
	 * @return A tag for this icon.
	 * @throws BadLocationException If <code>offs</code> is an invalid offset
	 *         into the text area.
	 * @see #removeTrackingIcon(Object)
	 */
	public GutterIconInfo addOffsetTrackingIcon(int offs, Icon icon)
												throws BadLocationException {
		Position pos = textArea.getDocument().createPosition(offs);
		GutterIconImpl ti = new GutterIconImpl(icon, pos);
		if (trackingIcons==null) {
			trackingIcons = new ArrayList(1); // Usually small
		}
		int index = Collections.binarySearch(trackingIcons, ti);
		if (index<0) {
			index = -(index+1);
		}
		trackingIcons.add(index, ti);
		repaint();
		return ti;
	}


	/**
	 * Returns the icon to use for bookmarks.
	 *
	 * @return The icon to use for bookmarks.  If this is <code>null</code>,
	 *         bookmarking is effectively disabled.
	 * @see #setBookmarkIcon(Icon)
	 * @see #isBookmarkingEnabled()
	 */
	public Icon getBookmarkIcon() {
		return bookmarkIcon;
	}


	/**
	 * Returns the bookmarks known to this gutter.
	 *
	 * @return The bookmarks.  If there are no bookmarks, an empty array is
	 *         returned.
	 */
	public GutterIconInfo[] getBookmarks() {

		List retVal = new ArrayList(1);

		if (trackingIcons!=null) {
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl ti = getTrackingIcon(i);
				if (ti.getIcon()==bookmarkIcon) {
					retVal.add(ti);
				}
			}
		}

		GutterIconInfo[] array = new GutterIconInfo[retVal.size()];
		return (GutterIconInfo[])retVal.toArray(array);

	}


	/**
	 * {@inheritDoc}
	 */
	void handleDocumentEvent(DocumentEvent e) {
		int newLineCount = textArea.getLineCount();
		if (newLineCount!=currentLineCount) {
			currentLineCount = newLineCount;
			repaint();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Dimension getPreferredSize() {
		int h = textArea!=null ? textArea.getHeight() : 100; // Arbitrary
		return new Dimension(width, h);
	}


	private GutterIconImpl getTrackingIcon(int index) {
		return (GutterIconImpl)trackingIcons.get(index);
	}


	/**
	 * Returns the tracking icons at the specified line.
	 *
	 * @param line The line.
	 * @return The tracking icons at that line.  If there are no tracking
	 *         icons there, this will be an empty array.
	 * @throws BadLocationException If <code>line</code> is invalid.
	 */
	public GutterIconImpl[] getTrackingIcons(int line)
								throws BadLocationException {

		List retVal = new ArrayList(1);

		if (trackingIcons!=null) {
			int start = textArea.getLineStartOffset(line);
			int end = textArea.getLineEndOffset(line);
			if (line==textArea.getLineCount()-1) {
				end++; // Hack
			}
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=start && offs<end) {
					retVal.add(ti);
				}
				else if (offs>=end) {
					break; // Quit early
				}
			}
		}

		GutterIconImpl[] array = new GutterIconImpl[retVal.size()];
		return (GutterIconImpl[])retVal.toArray(array);

	}


	/**
	 * Returns whether bookmarking is enabled.
	 *
	 * @return Whether bookmarking is enabled.
	 * @see #setBookmarkingEnabled(boolean)
	 */
	public boolean isBookmarkingEnabled() {
		return bookmarkingEnabled;
	}


	/**
	 * {@inheritDoc}
	 */
	void lineHeightsChanged() {
		repaint();
	}


	public void mouseClicked(MouseEvent e) {
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
		if (bookmarkingEnabled && bookmarkIcon!=null) {
			try {
				int offs = textArea.viewToModel(e.getPoint());
				if (offs>-1) {
					int line = textArea.getLineOfOffset(offs);				
					toggleBookmark(line);
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}
	}


	public void mouseReleased(MouseEvent e) {
	}


	/**
	 * {@inheritDoc}
	 */
	protected void paintComponent(Graphics g) {

		if (textArea==null) {
			return;
		}

		visibleRect = g.getClipBounds(visibleRect);
		if (visibleRect==null) { // ???
			visibleRect = getVisibleRect();
		}
		//System.out.println("IconRowHeader repainting: " + visibleRect);
		if (visibleRect==null) {
			return;
		}

		// Fill in the background the same color as the text component.
		g.setColor(getBackground());
		g.fillRect(0,visibleRect.y, width,visibleRect.height);

		if (textArea.getLineWrap()) {
			paintComponentWrapped(g);
			return;
		}

		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();

		// Get the first and last lines to paint.
		int cellHeight = textArea.getLineHeight();
		int topLine = visibleRect.y/cellHeight;
		int bottomLine = Math.min(topLine+visibleRect.height/cellHeight,
							root.getElementCount());

		// Get where to start painting (top of the row).
		// We need to be "scrolled up" up just enough for the missing part of
		// the first line.
		int y = topLine*cellHeight;
		textAreaInsets = textArea.getInsets(textAreaInsets);
		if (textAreaInsets!=null) {
			y += textAreaInsets.top;
		}

		if (trackingIcons!=null) {
			int lastLine = bottomLine;
			for (int i=trackingIcons.size()-1; i>=0; i--) { // Last to first
				GutterIconImpl ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line<=lastLine && line>topLine-1) {
						Icon icon = ti.getIcon();
						if (icon!=null) {
							int y2 = y + (line-topLine)*cellHeight;
							y2 += (cellHeight-icon.getIconHeight())/2;
							ti.getIcon().paintIcon(this, g, 0, y2);
							lastLine = line-1; // Paint only 1 icon per line
						}
					}
					else if (line<=topLine-1) {
						break;
					}
				}
			}
		}

	}


	/**
	 * Paints icons when line wrapping is enabled.
	 *
	 * @param g The graphics context.
	 */
	private void paintComponentWrapped(Graphics g) {

		// The variables we use are as follows:
		// - visibleRect is the "visible" area of the text area; e.g.
		// [0,100, 300,100+(lineCount*cellHeight)-1].
		// actualTop.y is the topmost-pixel in the first logical line we
		// paint.  Note that we may well not paint this part of the logical
		// line, as it may be broken into many physical lines, with the first
		// few physical lines scrolled past.  Note also that this is NOT the
		// visible rect of this line number list; this line number list has
		// visible rect == [0,0, insets.left-1,visibleRect.height-1].
		// - offset (<=0) is the y-coordinate at which we begin painting when
		// we begin painting with the first logical line.  This can be
		// negative, signifying that we've scrolled past the actual topmost
		// part of this line.

		// The algorithm is as follows:
		// - Get the starting y-coordinate at which to paint.  This may be
		//   above the first visible y-coordinate as we're in line-wrapping
		//   mode, but we always paint entire logical lines.
		// - Paint that line's line number and highlight, if appropriate.
		//   Increment y to be just below the are we just painted (i.e., the
		//   beginning of the next logical line's view area).
		// - Get the ending visual position for that line.  We can now loop
		//   back, paint this line, and continue until our y-coordinate is
		//   past the last visible y-value.

		// We avoid using modelToView/viewToModel where possible, as these
		// methods trigger a parsing of the line into syntax tokens, which is
		// costly.  It's cheaper to just grab the child views' bounds.

		RTextAreaUI ui = (RTextAreaUI)textArea.getUI();
		View v = ui.getRootView(textArea).getView(0);
//		boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();
		int lineCount = root.getElementCount();
		int topPosition = textArea.viewToModel(
								new Point(visibleRect.x,visibleRect.y));
		int topLine = root.getElementIndex(topPosition);

		// Compute the y at which to begin painting text, taking into account
		// that 1 logical line => at least 1 physical line, so it may be that
		// y<0.  The computed y-value is the y-value of the top of the first
		// (possibly) partially-visible view.
		Rectangle visibleEditorRect = ui.getVisibleEditorRect();
		Rectangle r = IconRowHeader.getChildViewBounds(v, topLine,
												visibleEditorRect);
		int y = r.y;

		int visibleBottom = visibleRect.y + visibleRect.height;

		// Get the first possibly visible icon index.
		int currentIcon = -1;
		if (trackingIcons!=null) {
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl icon = getTrackingIcon(i);
				int offs = icon.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line>=topLine) {
						currentIcon = i;
						break;
					}
				}
			}
		}

		// Keep painting lines until our y-coordinate is past the visible
		// end of the text area.
		g.setColor(getForeground());
		int cellHeight = textArea.getLineHeight();
		while (y < visibleBottom) {

			r = LineNumberList.getChildViewBounds(v, topLine, visibleEditorRect);
//			int lineEndY = r.y+r.height;

			/*
			// Highlight the current line's line number, if desired.
			if (currentLineHighlighted && topLine==currentLine) {
				g.setColor(textArea.getCurrentLineHighlightColor());
				g.fillRect(0,y, width,lineEndY-y);
				g.setColor(getForeground());
			}
			*/

			// Possibly paint an icon.
			if (currentIcon>-1) {
				// We want to paint the last icon added for this line.
				GutterIconImpl toPaint = null;
				while (currentIcon<trackingIcons.size()) {
					GutterIconImpl ti = getTrackingIcon(currentIcon);
					int offs = ti.getMarkedOffset();
					if (offs>=0 && offs<=doc.getLength()) {
						int line = root.getElementIndex(offs);
						if (line==topLine) {
							toPaint = ti;
						}
						else if (line>topLine) {
							break;
						}
					}
					currentIcon++;
				}
				if (toPaint!=null) {
					Icon icon = toPaint.getIcon();
					if (icon!=null) {
						int y2 = y + (cellHeight-icon.getIconHeight())/2;
						icon.paintIcon(this, g, 0, y2);
					}
				}
			}

			// The next possible y-coordinate is just after the last line
			// painted.
			y += r.height;

			// Update topLine (we're actually using it for our "current line"
			// variable now).
			topLine++;
			if (topLine>=lineCount)
				break;

		}

	}


	/**
	 * Removes the specified tracking icon.
	 *
	 * @param tag A tag for a tracking icon.
	 * @see #removeAllTrackingIcons()
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeTrackingIcon(Object tag) {
		if (trackingIcons!=null && trackingIcons.remove(tag)) {
			repaint();
		}
	}


	/**
	 * Removes all tracking icons.
	 *
	 * @see #removeTrackingIcon(Object)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeAllTrackingIcons() {
		if (trackingIcons!=null && trackingIcons.size()>0) {
			trackingIcons.clear();
			repaint();
		}
	}


	/**
	 * Removes all bookmark tracking icons.
	 */
	private void removeBookmarkTrackingIcons() {
		if (trackingIcons!=null) {
			for (Iterator i=trackingIcons.iterator(); i.hasNext(); ) {
				GutterIconImpl ti = (GutterIconImpl)i.next();
				if (ti.getIcon()==bookmarkIcon) {
					i.remove();
				}
			}
		}
	}


	/**
	 * Sets the icon to use for bookmarks.  Any previous bookmark icons
	 * are removed.
	 *
	 * @param icon The new bookmark icon.  If this is <code>null</code>,
	 *        bookmarking is effectively disabled.
	 * @see #getBookmarkIcon()
	 * @see #isBookmarkingEnabled()
	 */
	public void setBookmarkIcon(Icon icon) {
		removeBookmarkTrackingIcons();
		bookmarkIcon = icon;
		repaint();
	}


	/**
	 * Sets whether bookmarking is enabled.  Note that a bookmarking icon
	 * must be set via {@link #setBookmarkIcon(Icon)} before bookmarks are
	 * truly enabled.
	 *
	 * @param enabled Whether bookmarking is enabled.  If this is
	 *        <code>false</code>, any bookmark icons are removed.
	 * @see #isBookmarkingEnabled()
	 * @see #setBookmarkIcon(Icon)
	 */
	public void setBookmarkingEnabled(boolean enabled) {
		if (enabled!=bookmarkingEnabled) {
			bookmarkingEnabled = enabled;
			if (!enabled) {
				removeBookmarkTrackingIcons();
			}
			repaint();
		}
	}


	/**
	 * Sets the text area being displayed.  This will clear any tracking
	 * icons currently displayed.
	 *
	 * @param textArea The text area.
	 */
	public void setTextArea(RTextArea textArea) {
		removeAllTrackingIcons();
		super.setTextArea(textArea);
	}


	/**
	 * Programatically toggles whether there is a bookmark for the specified
	 * line.  If bookmarking is not enabled, this method does nothing.
	 *
	 * @param line The line.
	 * @return Whether a bookmark is now at the specified line.
	 * @throws BadLocationException If <code>line</code> is an invalid line
	 *         number in the text area.
	 */
	public boolean toggleBookmark(int line) throws BadLocationException {
		IBookMarkListener listener = BookMarkListenerManager.getListener();
		
		if (!isBookmarkingEnabled() || getBookmarkIcon()==null) {
			return false;
		}

		GutterIconImpl[] icons = getTrackingIcons(line);
		if (icons.length==0) {
			int offs = textArea.getLineStartOffset(line);
			if(listener!=null && listener.beforeAddBookmark(line)){										
				addOffsetTrackingIcon(offs, bookmarkIcon);
			}
			return true;
		}

		boolean found = false;
		for (int i=0; i<icons.length; i++) {
			if (icons[i].getIcon()==bookmarkIcon) {
				if(listener!=null && listener.beforeRemoveBookmark(line)){											
					removeTrackingIcon(icons[i]);
				}
				found = true;
				// Don't quit, in case they manipulate the document so > 1
				// bookmark is on a single line (kind of flaky, but it
				// works...).  If they delete all chars in the document,
				// AbstractDocument gets a little flaky with the returned line
				// number for viewToModel(), so this is just us trying to save
				// face a little.
			}
		}
		if (!found) {
			int offs = textArea.getLineStartOffset(line);
			if(listener!=null && listener.beforeAddBookmark(line)){										
				addOffsetTrackingIcon(offs, bookmarkIcon);
			}
		}

		return !found;

	}


	/**
	 * Implementation of the icons rendered.
	 *
	 * @author Robert Futrell
	 * @version 1.0
	 */
	private static class GutterIconImpl implements GutterIconInfo, Comparable {

		private Icon icon;
		private Position pos;

		public GutterIconImpl(Icon icon, Position pos) {
			this.icon = icon;
			this.pos = pos;
		}

		public int compareTo(Object o) {
			if (o instanceof GutterIconImpl) {
				return pos.getOffset() - ((GutterIconImpl)o).getMarkedOffset();
			}
			return -1;
		}

		public boolean equals(Object o) {
			return o==this;
		}

		public Icon getIcon() {
			return icon;
		}

		public int getMarkedOffset() {
			return pos.getOffset();
		}

		public int hashCode() {
			return icon.hashCode(); // FindBugs
		}

	}


}