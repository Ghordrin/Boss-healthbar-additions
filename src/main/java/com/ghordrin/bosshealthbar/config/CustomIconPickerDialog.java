package com.ghordrin.bosshealthbar.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.item.ItemPrice;

// The window behind "Choose custom icon". Items are searched by name, sprites are browsed, and whichever is
// picked is the one the Custom theme shows.
class CustomIconPickerDialog extends JDialog
{
	private static final int THUMB_SIZE = 48;
	private static final int COLUMNS = 6;
	// A pass runs once per game cycle, so the whole range costs its size divided by this many cycles. Fifty
	// at a time left the window loading for over a minute; this reads it in a few.
	private static final int SPRITES_PER_PASS = 500;
	// Sprite ids run well past the icons; this covers the ones the game has without probing forever.
	private static final int MAX_SPRITE_ID = 6000;
	// An icon is small. Anything larger is interface furniture, which would swamp the grid and the memory
	// it takes to hold it, and would be drawn far too small on a bar anyway.
	private static final int MAX_SPRITE_SIZE = 64;
	private static final int MIN_SPRITE_SIZE = 6;
	private static final int MAX_ITEM_RESULTS = 120;
	private static final Border SELECTED_BORDER = BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2);
	private static final Border HOVER_BORDER = BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 2);
	private static final Border IDLE_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final BossHealthBarConfig config;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;

	private final JPanel resultsPanel = new JPanel(new GridLayout(0, COLUMNS, 4, 4));
	private final JLabel statusLabel = new JLabel();
	private final JTextField searchField = new JTextField();
	private final JToggleButton itemsButton = new JToggleButton("Items", true);
	private final JToggleButton spritesButton = new JToggleButton("Sprites");

	// The sprites found by the last browse, kept so typing filters them without probing the game again.
	private final List<Integer> spriteIds = new ArrayList<>();
	private final List<BufferedImage> spriteImages = new ArrayList<>();

	private JButton selectedButton;
	private volatile int loadGeneration;

	CustomIconPickerDialog(Client client, ClientThread clientThread, ConfigManager configManager,
		BossHealthBarConfig config, ItemManager itemManager, SpriteManager spriteManager)
	{
		super(SwingUtilities.getWindowAncestor(client.getCanvas()), "Choose custom icon");
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.config = config;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		final JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setContentPane(content);

		final ButtonGroup sources = new ButtonGroup();
		sources.add(itemsButton);
		sources.add(spritesButton);
		itemsButton.setFocusable(false);
		spritesButton.setFocusable(false);
		itemsButton.setToolTipText("Search the game's items by name");
		spritesButton.setToolTipText("Browse the game's sprites, such as clan ranks and skill icons");
		itemsButton.addActionListener(e -> sourceChanged());
		spritesButton.addActionListener(e -> sourceChanged());

		final JButton none = new JButton("None");
		none.setToolTipText("Go back to no custom icon");
		none.setFocusable(false);
		none.addActionListener(e -> selectNone());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		buttons.setOpaque(false);
		buttons.add(itemsButton);
		buttons.add(spritesButton);
		buttons.add(none);

		searchField.setToolTipText("Part of an item's name, or part of a sprite's number");
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				searchChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				searchChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				searchChanged();
			}
		});

		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());

		final JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setOpaque(false);
		header.add(buttons, BorderLayout.NORTH);
		header.add(searchField, BorderLayout.CENTER);
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

		showItems();
	}

	private boolean showingItems()
	{
		return itemsButton.isSelected();
	}

	private void sourceChanged()
	{
		searchField.setText("");
		if (showingItems())
		{
			showItems();
		}
		else if (spriteIds.isEmpty())
		{
			loadSprites();
		}
		else
		{
			showSprites();
		}
	}

	private void searchChanged()
	{
		if (showingItems())
		{
			showItems();
		}
		else
		{
			showSprites();
		}
	}


	private void showItems()
	{
		final String query = searchField.getText().trim();
		if (query.isEmpty())
		{
			clear();
			statusLabel.setText("Type part of an item's name");
			return;
		}

		final List<ItemPrice> found = itemManager.search(query);
		clear();
		if (found.isEmpty())
		{
			statusLabel.setText("No item matches \"" + query + "\"");
			return;
		}

		final int shown = Math.min(found.size(), MAX_ITEM_RESULTS);
		final int currentId = config.customIconSpriteId() < 0 ? config.customIconItemId() : -1;
		for (int i = 0; i < shown; i++)
		{
			final ItemPrice item = found.get(i);
			final JButton button = buildButton(item.getName(), () -> selectItem(item.getId()));
			itemManager.getImage(item.getId()).addTo(button);
			if (item.getId() == currentId)
			{
				markSelected(button);
			}
			resultsPanel.add(button);
		}

		statusLabel.setText(shown < found.size() ? "First " + shown + " of " + found.size() + " items"
			: shown + (shown == 1 ? " item" : " items"));
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	// Sprite ids aren't listed anywhere, so they are read one by one. Decoded in batches over several frames
	// so opening the window doesn't stall the game, and a newer load makes an older one stop.
	private void loadSprites()
	{
		clear();
		statusLabel.setText("Loading sprites...");
		spriteIds.clear();
		spriteImages.clear();
		final int generation = ++loadGeneration;
		final int[] nextId = {0};

		clientThread.invoke(() ->
		{
			if (generation != loadGeneration)
			{
				return true;
			}

			final List<Integer> ids = new ArrayList<>();
			final List<BufferedImage> images = new ArrayList<>();
			final int end = Math.min(MAX_SPRITE_ID, nextId[0] + SPRITES_PER_PASS);
			for (int id = nextId[0]; id < end; id++)
			{
				final BufferedImage sprite = sprite(id);
				if (sprite == null || sprite.getWidth() > MAX_SPRITE_SIZE || sprite.getHeight() > MAX_SPRITE_SIZE
					|| sprite.getWidth() < MIN_SPRITE_SIZE || sprite.getHeight() < MIN_SPRITE_SIZE)
				{
					continue;
				}
				ids.add(id);
				images.add(sprite);
			}
			nextId[0] = end;
			final boolean done = end >= MAX_SPRITE_ID;

			// Each pass is shown as it lands, so the grid fills in rather than the window sitting on
			// "Loading sprites..." until the last id has been read.
			SwingUtilities.invokeLater(() ->
			{
				if (generation != loadGeneration)
				{
					return;
				}
				spriteIds.addAll(ids);
				spriteImages.addAll(images);
				showSprites();
				if (!done)
				{
					statusLabel.setText("Loading sprites... " + spriteIds.size() + " so far");
				}
			});
			return done;
		});
	}

	// Not every id is a sprite the game can hand over: some archives hold nothing, and reading one throws
	// rather than answering with nothing. Left to itself that ends the whole sweep, since the client drops a
	// task that threw, and the window would sit on "Loading sprites..." for good.
	private BufferedImage sprite(int id)
	{
		try
		{
			return spriteManager.getSprite(id, 0);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private void showSprites()
	{
		final String query = searchField.getText().trim();
		clear();
		if (spriteIds.isEmpty())
		{
			statusLabel.setText("No sprites loaded, log in and reopen this window");
			return;
		}

		final int currentId = config.customIconSpriteId();
		int shown = 0;
		for (int i = 0; i < spriteIds.size(); i++)
		{
			final int id = spriteIds.get(i);
			if (!query.isEmpty() && !String.valueOf(id).contains(query))
			{
				continue;
			}
			final JButton button = buildButton("Sprite " + id, () -> selectSprite(id));
			button.setIcon(new ImageIcon(spriteImages.get(i)));
			if (id == currentId)
			{
				markSelected(button);
			}
			resultsPanel.add(button);
			shown++;
		}

		statusLabel.setText(shown == 0 ? "No sprite number has \"" + query + "\" in it"
			: shown + (shown == 1 ? " sprite" : " sprites"));
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	private void clear()
	{
		resultsPanel.removeAll();
		selectedButton = null;
		resultsPanel.revalidate();
		resultsPanel.repaint();
	}

	private JButton buildButton(String tooltip, Runnable onPick)
	{
		final JButton button = new JButton();
		button.setToolTipText(tooltip);
		button.setPreferredSize(new Dimension(THUMB_SIZE + 4, THUMB_SIZE + 4));
		button.setBorder(IDLE_BORDER);
		button.setContentAreaFilled(false);
		button.setFocusable(false);
		button.addActionListener(e ->
		{
			markSelected(button);
			onPick.run();
		});
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

	private void markSelected(JButton button)
	{
		if (selectedButton != null)
		{
			selectedButton.setBorder(IDLE_BORDER);
		}
		selectedButton = button;
		button.setBorder(SELECTED_BORDER);
	}

	private void selectItem(int itemId)
	{
		write(CustomIcon.ITEM, itemId);
	}

	private void selectSprite(int spriteId)
	{
		write(CustomIcon.SPRITE, spriteId);
	}

	private void selectNone()
	{
		write(CustomIcon.NONE, -1);
		clear();
		if (showingItems())
		{
			statusLabel.setText("Type part of an item's name");
		}
		else
		{
			showSprites();
		}
	}

	// Only one of the two is ever set, so what the bar shows is what was just picked.
	private void write(CustomIcon kind, int id)
	{
		ConfigWrites.write(configManager, BossHealthBarConfig.CUSTOM_ICON_ITEM_ID_KEY,
			kind == CustomIcon.ITEM ? id : -1);
		ConfigWrites.write(configManager, BossHealthBarConfig.CUSTOM_ICON_SPRITE_ID_KEY,
			kind == CustomIcon.SPRITE ? id : -1);
	}

	private enum CustomIcon
	{
		NONE,
		ITEM,
		SPRITE
	}
}
