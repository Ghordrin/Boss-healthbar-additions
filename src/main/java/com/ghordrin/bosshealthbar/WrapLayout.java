package com.ghordrin.bosshealthbar;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

// FlowLayout's preferred size never wraps, so a scroll pane around it never grows past one row.
// This measures rows against the scroll pane's viewport width instead.
final class WrapLayout extends FlowLayout
{
	WrapLayout(int align, int hgap, int vgap)
	{
		super(align, hgap, vgap);
	}

	@Override
	public Dimension preferredLayoutSize(Container target)
	{
		synchronized (target.getTreeLock())
		{
			final int maxWidth = wrapWidth(target);
			final int hgap = getHgap();
			final int vgap = getVgap();
			final Insets insets = target.getInsets();

			int rowWidth = 0;
			int rowHeight = 0;
			int totalWidth = 0;
			int totalHeight = 0;

			final int members = target.getComponentCount();
			for (int i = 0; i < members; i++)
			{
				final Component m = target.getComponent(i);
				if (!m.isVisible())
				{
					continue;
				}
				final Dimension d = m.getPreferredSize();
				if (rowWidth > 0 && rowWidth + hgap + d.width > maxWidth)
				{
					totalWidth = Math.max(totalWidth, rowWidth);
					totalHeight += rowHeight + vgap;
					rowWidth = 0;
					rowHeight = 0;
				}
				rowWidth += (rowWidth > 0 ? hgap : 0) + d.width;
				rowHeight = Math.max(rowHeight, d.height);
			}
			totalWidth = Math.max(totalWidth, rowWidth);
			totalHeight += rowHeight;

			return new Dimension(
				totalWidth + insets.left + insets.right,
				totalHeight + insets.top + insets.bottom + vgap * 2);
		}
	}

	private static int wrapWidth(Container target)
	{
		final JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
		if (scrollPane != null)
		{
			final JViewport viewport = scrollPane.getViewport();
			if (viewport.getWidth() > 0)
			{
				return viewport.getWidth();
			}
		}
		return target.getWidth() > 0 ? target.getWidth() : Integer.MAX_VALUE;
	}
}
