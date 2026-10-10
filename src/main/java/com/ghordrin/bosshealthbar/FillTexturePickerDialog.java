package com.ghordrin.bosshealthbar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import net.runelite.api.Client;
import net.runelite.api.Texture;
import net.runelite.api.TextureProvider;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class FillTexturePickerDialog extends JDialog
{
	private static final int THUMB_SIZE = 48;
	private static final int COLUMNS = 6;
	private static final int TEXTURES_PER_FRAME = 50;
	private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2);
	private static final Border HOVER_BORDER = BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 2);
	private static final Border IDLE_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final JPanel resultsPanel = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
	private final JLabel statusLabel = new JLabel();

	private JButton selectedButton;
	private volatile int loadGeneration;

	FillTexturePickerDialog(Client client, ClientThread clientThread, ConfigManager configManager, BossHealthBarConfig config)
	{
		super(SwingUtilities.getWindowAncestor(client.getCanvas()), "Choose fill texture");
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		final JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(content);

		final JButton none = new JButton("None");
		none.setToolTipText("Go back to the plain fill");
		none.setFocusable(false);
		none.addActionListener(e -> selectNone());

		final JButton refresh = new JButton("Refresh");
		refresh.setToolTipText("Read the game's textures again");
		refresh.setFocusable(false);
		refresh.addActionListener(e -> loadTextures());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttons.setOpaque(false);
		buttons.add(none);
		buttons.add(refresh);

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());

		final JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setOpaque(false);
		header.add(buttons, BorderLayout.NORTH);
		header.add(statusLabel, BorderLayout.SOUTH);

		// Keeps the grid at its own size in the top left, instead of stretching it over the whole view.
		final JPanel resultsHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		resultsHolder.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		resultsPanel.setOpaque(false);
		resultsHolder.add(resultsPanel);
		final JScrollPane scrollPane = new JScrollPane(resultsHolder);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		content.add(header, BorderLayout.NORTH);
		content.add(scrollPane, BorderLayout.CENTER);

		setSize(400, 460);
		setResizable(false);
		setLocationRelativeTo(getOwner());

		loadTextures();
	}

	private void loadTextures()
	{
		statusLabel.setText("Loading...");
		// Decoded in batches over several frames so opening the window doesn't stall the game.
		// A newer load (from Refresh) makes an older one stop.
		final int generation = ++loadGeneration;
		final List<Integer> ids = new ArrayList<>();
		final List<BufferedImage> images = new ArrayList<>();
		final int[] nextId = {0};

		clientThread.invoke(() ->
		{
			if (generation != loadGeneration)
			{
				return true;
			}

			final TextureProvider provider = client.getTextureProvider();
			if (provider != null)
			{
				final Texture[] textures = provider.getTextures();
				final int end = Math.min(textures.length, nextId[0] + TEXTURES_PER_FRAME);
				for (int id = nextId[0]; id < end; id++)
				{
					if (textures[id] == null)
					{
						continue;
					}
					final BufferedImage image = toThumbnail(provider, id);
					if (image != null)
					{
						ids.add(id);
						images.add(image);
					}
				}
				nextId[0] = end;
				if (end < textures.length)
				{
					return false;
				}
			}

			SwingUtilities.invokeLater(() ->
			{
				if (generation == loadGeneration)
				{
					populate(ids, images);
				}
			});
			return true;
		});
	}

	private static BufferedImage toThumbnail(TextureProvider provider, int id)
	{
		final BufferedImage image = BarPainter.decodeTexture(provider, id);
		if (image == null)
		{
			return null;
		}

		final BufferedImage thumb = new BufferedImage(THUMB_SIZE, THUMB_SIZE, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = thumb.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, THUMB_SIZE, THUMB_SIZE, null);
		g.dispose();
		return thumb;
	}

	private void populate(List<Integer> ids, List<BufferedImage> images)
	{
		resultsPanel.removeAll();
		selectedButton = null;
		final int currentId = config.fillTextureId();

		for (int i = 0; i < ids.size(); i++)
		{
			final int id = ids.get(i);
			final JButton button = buildButton(id, images.get(i));
			if (id == currentId)
			{
				selectedButton = button;
				button.setBorder(SELECTED_BORDER);
			}
			resultsPanel.add(button);
		}

		statusLabel.setText(ids.isEmpty() ? "No textures loaded yet, try Refresh" : ids.size() + " textures");
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	private JButton buildButton(int textureId, BufferedImage image)
	{
		final JButton button = new JButton(new ImageIcon(image));
		button.setToolTipText("Texture " + textureId);
		button.setPreferredSize(new Dimension(THUMB_SIZE + 4, THUMB_SIZE + 4));
		button.setBorder(IDLE_BORDER);
		button.setContentAreaFilled(false);
		button.setFocusable(false);
		button.addActionListener(e -> select(button, textureId));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (button != selectedButton)
				{
					button.setBorder(HOVER_BORDER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				if (button != selectedButton)
				{
					button.setBorder(IDLE_BORDER);
				}
			}
		});
		return button;
	}

	private void select(JButton button, int textureId)
	{
		if (selectedButton != null)
		{
			selectedButton.setBorder(IDLE_BORDER);
		}
		selectedButton = button;
		button.setBorder(SELECTED_BORDER);
		configManager.setConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FILL_TEXTURE_ID_KEY, textureId);
	}

	private void selectNone()
	{
		if (selectedButton != null)
		{
			selectedButton.setBorder(IDLE_BORDER);
			selectedButton = null;
		}
		configManager.unsetConfiguration(BossHealthBarConfig.GROUP, BossHealthBarConfig.FILL_TEXTURE_ID_KEY);
	}
}
