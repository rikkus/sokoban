/* vim: set ts=2 sw=2 noet: 
 * Sokoban for J2ME on SE t610
 *
 * Copyright (C) 2003 Rik Hemsley <rik@rikkus.info>
 */
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;

public final class Sokoban extends MIDlet implements CommandListener
{
	private Display				display;
	private SokobanCanvas	canvas;
	private StringBuffer	levelData;
	private int						height;
	private int						width;
	private int						manX;
	private int						manY;
	private int						openWater;
	private Stack					undoData;
	private Alert					solvedAlert;
	private int						level;
	private RecordStore		recordStore;
	private int						levelID;
	private int						HighestLevelAttainedKey = 0;

	private char					Wall					= '#';
	private char					Air						= ' ';
	private char					Water					= '.';
	private char					Block					= '$';
	private char					BlockOnWater	= '*';
	private char					Man						= '@';
	private char					ManOnWater		= '+';

	private Command				undoCommand;
	private Command				okCommand;
	private Command				exitCommand;
	private Command 			changeLevelCommand;
	private Command 			restartLevelCommand;

	private TextBox				levelScreen;

	public Sokoban()
	{
		display = Display.getDisplay(this);
	}

	public int width()
	{
		return this.width;
	}

	public int height()
	{
		return this.height;
	}

	public boolean solved()
	{
		return 0 == openWater;
	}

	public char data(int row, int col)
	{
		return levelData.charAt(row * height + col);
	}

	private void set(int row, int col, char c)
	{
		levelData.setCharAt(row * height + col, c);
	}

	public void restartLevel()
	{
		setLevel(level);
	}

	private void setLevel(int i)
	{
		if (i > Data.length - 1)
			return;

		level = i;

		undoData.removeAllElements();

		openWater	= 0;
		width			= 8;
		height		= 8;
		levelData	= new StringBuffer(Data[level]);

		manX = -1;
		manY = -1;

		for (i = 0; i < levelData.length(); ++i)
		{
			char c = levelData.charAt(i);

			if (c == Man || c == ManOnWater)
			{
				manX = i % width;
				manY = i / width;
			}

			if (c == Water || c == ManOnWater)
				openWater++;
		}

		//System.out.println("Open water: " + Integer.toString(openWater));

		canvas.reset(width, height);
	}

	private boolean outOfBounds(int y, int x)
	{
		return (x < 0) || (y < 0) || (x >= width) || (y >= height);
	}

	boolean hasWater(int y, int x)
	{
		return hasWater(data(y, x));
	}

	boolean hasWater(char c)
	{
		return c == Water || c == ManOnWater || c == BlockOnWater;
	}

	boolean hasBlock(char c)
	{
		return c == Block || c == BlockOnWater;
	}

	private boolean move(int dy, int dx)
	{
		boolean push = false;

		int newX = manX + dx;
		int newY = manY + dy;

		// Gone out of bounds?
		if (outOfBounds(newY, newX))
			return false;

		// Hit a wall?
		if (data(manY + dy, manX + dx) == Wall)
			return false;

		// Pushing a block?
		if (
				data(manY + dy, manX + dx) == Block
				||
				data(manY + dy, manX + dx) == BlockOnWater
		   )
		{
			int blockX = newX + dx;
			int blockY = newY + dy;

			// Block goes out of bounds?
			if (outOfBounds(blockY, blockX))
				return false;

			// Block hits wall?
			if (data(blockY, blockX) == Wall)
				return false;

			// Block hits other block?
			if (hasBlock(data(blockY, blockX)))
				return false;

			// Set the data for the cell we are pushing into.
			// Fallthrough will handle the rest.
			if (hasWater(blockY, blockX))
			{
				set(blockY, blockX, BlockOnWater);
				openWater--;

				if (hasWater(newY, newX))
				{
					openWater++;
				}
//				System.out.println("Open water: " + Integer.toString(openWater));
			}
			else
			{
				set(blockY, blockX, Block);

				if (hasWater(newY, newX))
				{
					openWater++;
				}

//				System.out.println("Open water: " + Integer.toString(openWater));
			}

			repaint(blockY, blockX);

			push = true;
		}

		// Walking bit.
		if (hasWater(newY, newX))
			set(newY, newX, ManOnWater);
		else
			set(newY, newX, Man);

		if (hasWater(manY, manX))
			set(manY, manX, Water);
		else
			set(manY, manX, Air);

		repaint(manY, manX);

		manY += dy;
		manX += dx;

		repaint(manY, manX);

		undoData.push(new Character(moveCharacter(dy, dx, push)));

		if (solved())
		{
			solvedAlert.setTitle("Level " + Integer.toString(level) + " solved");
			display.setCurrent(solvedAlert);
			setLevel(++level);
			savePosition();
		}

		return true;
	}

	void repaint()
	{
		for (int y = 0; y < height; ++y)
		{
			for (int x = 0; x < width; ++x)
			{
				repaint(y, x);
			}
		}
	}

	void repaint(int y, int x)
	{
		canvas.repaint(y, x);
	}

	public boolean undo()
	{
		if (undoData.empty())
			return false;

		char lastMove = ((Character)undoData.pop()).charValue();

		int dx = xDelta(lastMove);
		int dy = yDelta(lastMove);

		int newX = manX - dx;
		int newY = manY - dy;

		if (hasWater(newY, newX))
		{
			set(newY, newX, ManOnWater);
		}
		else
		{
			set(newY, newX, Man);
		}

		repaint(newY, newX);

		if (push(lastMove))
		{
			int oldBlockX = manX + dx;
			int oldBlockY = manY + dy;

			if (hasWater(oldBlockY, oldBlockX))
			{
				set(oldBlockY, oldBlockX, Water);
				openWater++;
			}
			else
			{
				set(oldBlockY, oldBlockX, Air);
			}

			if (hasWater(manY, manX))
			{
				set(manY, manX, BlockOnWater);
				openWater--;
			}
			else
			{
				set(manY, manX, Block);
			}

			repaint(oldBlockY, oldBlockX);
		}
		else
		{
			if (hasWater(manY, manX))
				set(manY, manX, Water);
			else
				set(manY, manX, Air);
		}

//		System.out.println("Open water: " + Integer.toString(openWater));

		repaint(manY, manX);

		manX = newX;
		manY = newY;

		if (solved())
		{
			solvedAlert.setTitle("Level " + Integer.toString(level) + " solved");
			display.setCurrent(solvedAlert);
			setLevel(++level);
			byte[] x = new byte[1];
			x[0] = (new Integer(level)).byteValue();
			savePosition();
		}

		return true;
	}

	boolean push(char c)
	{
		return 'U' == c || 'D' == c || 'L' == c || 'R' == c;
	}

	int xDelta(char c)
	{
		if ('r' == c || 'R' == c)
			return 1;
		if ('l' == c || 'L' == c)
			return -1;

		return 0;
	}

	int yDelta(char c)
	{
		if ('d' == c || 'D' == c)
			return 1;
		if ('u' == c || 'U' == c)
			return -1;

		return 0;
	}

	char moveCharacter(int dy, int dx, boolean push)
	{
		if (push)
		{
			if (-1 == dy)
				return 'U';
			else if (1 == dy)
				return 'D';
			else if (-1 == dx)
				return 'L';
			else if (1 == dx)
				return 'R';
		}
		else
		{
			if (-1 == dy)
				return 'u';
			else if (1 == dy)
				return 'd';
			else if (-1 == dx)
				return 'l';
			else if (1 == dx)
				return 'r';
		}

		return 'E';
	}

	public void up()
	{
		move(-1, 0);
	}

	public void down()
	{
		move(1, 0);
	}

	public void left()
	{
		move(0, -1);
	}

	public void right()
	{
		move(0, 1);
	}

	public void init() throws MIDletStateChangeException
	{
	}

	public void startApp()
	{
		undoCommand					= new Command("Undo", Command.SCREEN, 1);
		changeLevelCommand	= new Command("Change level", Command.SCREEN, 3);
		restartLevelCommand	= new Command("Restart level", Command.SCREEN, 4);
		okCommand						= new Command("Ok", Command.OK, 20);
		exitCommand					= new Command("Exit", Command.EXIT, 50);

		level				= 0;

		canvas			= new SokobanCanvas(this);

		undoData		= new Stack();
		solvedAlert	= new Alert("");

		try
		{
			solvedAlert.setImage(Image.createImage("/sokoban.png"));
		}
		catch (java.io.IOException e)
		{
			System.out.println("Can't load images");
		}

		canvas.addCommand(undoCommand);
		canvas.addCommand(restartLevelCommand);
		canvas.addCommand(changeLevelCommand);
		canvas.addCommand(exitCommand);
		canvas.setCommandListener(this);

		try
		{
			recordStore =
				RecordStore.openRecordStore
				("Sokoban", true);
		}
		catch (RecordStoreException e)
		{
			e.printStackTrace();
		}

		loadPosition();
		setLevel(level);

		display.setCurrent(canvas);
	}

	public void pauseApp()
	{
//		System.out.println("pauseApp()");
	}

	public void destroyApp(boolean cond)
	{
		// Empty.
	}

	public void commandAction(Command c, Displayable d)
	{
		System.out.println("commandAction");

		if (c == undoCommand)
		{
			undo();
		}
		else if (c == exitCommand)
		{
			destroyApp(false);
			notifyDestroyed();
		}
		else if (c == restartLevelCommand)
		{
			restartLevel();
		}
		else if (c == changeLevelCommand)
		{
			System.out.println("Change level...");

			if (levelScreen == null)
			{
				levelScreen =
					new TextBox
					("Enter level", Integer.toString(level), 4, TextField.NUMERIC);

				levelScreen.addCommand(okCommand);
				levelScreen.setCommandListener(this);
			}
			else
			{
				levelScreen.setString(Integer.toString(level));
			}

			display.setCurrent(levelScreen);
		}
		else if (c == okCommand && d == levelScreen)
		{
			setLevel(Integer.parseInt(((TextBox)levelScreen).getString()));
			display.setCurrent(canvas);
		}
	}

	private void loadPosition()
	{
		try
		{
			levelID = 0;

			RecordEnumeration en = recordStore.enumerateRecords(null, null, false);

			byte[] data = new byte[8];

			while (en.hasNextElement())
			{
				int index = en.nextRecordId();

				if (8 == recordStore.getRecordSize(index))
				{
					recordStore.getRecord(index, data, 0);

					int id = getInt(data, 0);

					if (HighestLevelAttainedKey == id)
					{
						level = getInt(data, 4);
						levelID = index;

						break;
					}
				}
			}
		}
		catch (RecordStoreException e)
		{
			e.printStackTrace();
		}
	}
	
	private void savePosition()
	{
		byte[] data = new byte[8];
		putInt(data, 0, HighestLevelAttainedKey);
		putInt(data, 4, level);

		try
		{
			if (0 == levelID)
				levelID = recordStore.addRecord(data, 0, data.length);
			else
				recordStore.setRecord(levelID, data, 0, data.length);
		}
		catch (RecordStoreException e)
		{
			e.printStackTrace();
		}
	}

	private void putInt(byte[] buf, int offset, int value)
	{
		buf[offset + 0] = (byte)((value >> 24) & 0xff);
		buf[offset + 1] = (byte)((value >> 16) & 0xff);
		buf[offset + 2] = (byte)((value >>  8) & 0xff);
		buf[offset + 3] = (byte)((value >>  0) & 0xff);
	}

	private int getInt(byte[] buf, int offset)
	{
		return
			(
			 (buf[offset + 0] & 0xff) << 24 |
			 (buf[offset + 1] & 0xff) << 16 |
			 (buf[offset + 2] & 0xff) <<  8 |
			 (buf[offset + 3] & 0xff)
			);
	}

	/*
	 * # -> Wall
	 *   -> Air
	 * . -> Water
	 *
	 * $ -> Block
	 * * -> Block on water
	 *
	 * @ -> Man
	 */
	private static final String Data[] =
	{
		  "########"
		+ "## .####"
		+ "##  ####"
		+ "##*@  ##"
		+ "##  $ ##"
		+ "##  ####"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##    ##"
		+ "## #@ ##"
		+ "## $* ##"
		+ "## .* ##"
		+ "##    ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#      #"
		+ "# .**$@#"
		+ "#      #"
		+ "#####  #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##     #"
		+ "## .$. #"
		+ "## $@$ #"
		+ "#  .$. #"
		+ "#      #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#     ##"
		+ "# .$. ##"
		+ "# $.$ ##"
		+ "# .$. ##"
		+ "# $.$ ##"
		+ "#  @  ##"
		+ "########"
		,
		  "########"
		+ "##.  ###"
		+ "##@$$ ##"
		+ "###   ##"
		+ "####  ##"
		+ "#####.##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##     #"
		+ "## # # #"
		+ "##. $*@#"
		+ "##   ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "## @ ###"
		+ "##...###"
		+ "##$$$###"
		+ "##    ##"
		+ "##    ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#   .. #"
		+ "#  @$$ #"
		+ "##### ##"
		+ "####  ##"
		+ "####  ##"
		+ "####  ##"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##  ####"
		+ "## . . #"
		+ "## $$#@#"
		+ "###    #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##  *  #"
		+ "##     #"
		+ "### # ##"
		+ "###$@.##"
		+ "###   ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "####   #"
		+ "####$$@#"
		+ "##   ###"
		+ "##     #"
		+ "## . . #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "###  ###"
		+ "### $$ #"
		+ "###... #"
		+ "##  @$ #"
		+ "##   ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "### @ ##"
		+ "###   ##"
		+ "####$ ##"
		+ "## ...##"
		+ "## $$ ##"
		+ "####  ##"
		+ "########"
		,
		  "########"
		+ "#   .###"
		+ "# ## ###"
		+ "#  $$@##"
		+ "# #   ##"
		+ "#.  ####"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#   ####"
		+ "# @ ####"
		+ "# $$####"
		+ "##. . ##"
		+ "##    ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##  ####"
		+ "## $$ ##"
		+ "##... ##"
		+ "## @$ ##"
		+ "##   ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "###  ###"
		+ "##@$.###"
		+ "# $$  ##"
		+ "# . . ##"
		+ "###   ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "###  ###"
		+ "##     #"
		+ "##.**$@#"
		+ "##   ###"
		+ "###  ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##. #  #"
		+ "##  $  #"
		+ "##. $#@#"
		+ "##  $  #"
		+ "##. #  #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "###   ##"
		+ "###   ##"
		+ "## $$$ #"
		+ "## .+. #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#     ##"
		+ "#@$$$ ##"
		+ "#  #...#"
		+ "##    ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##... ##"
		+ "##  $ ##"
		+ "## #$###"
		+ "##  $ ##"
		+ "##  @ ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "###    #"
		+ "##  ## #"
		+ "## # $ #"
		+ "##  * .#"
		+ "### #@##"
		+ "###   ##"
		+ "########"
		,
		  "########"
		+ "#  @ ###"
		+ "#  # ###"
		+ "# .#  ##"
		+ "# .$$$ #"
		+ "# .#   #"
		+ "####   #"
		+ "########"
		,
		  "########"
		+ "#  #####"
		+ "#    ###"
		+ "#  $*@ #"
		+ "### .# #"
		+ "###    #"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#### @##"
		+ "##  $ ##"
		+ "##  *.##"
		+ "##  *.##"
		+ "##  $ ##"
		+ "####  ##"
		+ "########"
		,
		  "########"
		+ "###. .##"
		+ "## * * #"
		+ "##  #  #"
		+ "## $ $ #"
		+ "### @ ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##   ###"
		+ "##  $  #"
		+ "###* . #"
		+ "###   @#"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##  ####"
		+ "##.*$  #"
		+ "## .$# #"
		+ "### @  #"
		+ "###   ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#   ####"
		+ "# #  ###"
		+ "#@$*.###"
		+ "##  . ##"
		+ "## $# ##"
		+ "###   ##"
		+ "########"
		,
		  "########"
		+ "#@     #"
		+ "# .$$. #"
		+ "# $..$ #"
		+ "# $..$ #"
		+ "# .$$. #"
		+ "#      #"
		+ "########"
		,
		  "########"
		+ "# @#  ##"
		+ "#.$   ##"
		+ "#. # $##"
		+ "#.$#   #"
		+ "#. # $ #"
		+ "#  #   #"
		+ "########"
		,
		  "########"
		+ "### . ##"
		+ "### $  #"
		+ "# . $#@#"
		+ "# #$ . #"
		+ "#  $ ###"
		+ "## . ###"
		+ "########"
		,
		  "########"
		+ "#      #"
		+ "# $*** #"
		+ "# *  * #"
		+ "# *  * #"
		+ "# ***. #"
		+ "#     @#"
		+ "########"
		,
		  "########"
		+ "#@$.   #"
		+ "#      #"
		+ "#      #"
		+ "#      #"
		+ "#      #"
		+ "#      #"
		+ "########"
		,
		  "########"
		+ "###  . #"
		+ "## * # #"
		+ "## .$  #"
		+ "##  #$##"
		+ "### @ ##"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##  .@ #"
		+ "## #.# #"
		+ "##   $ #"
		+ "##.$$ ##"
		+ "##  ####"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#### @##"
		+ "#  *$ ##"
		+ "#     ##"
		+ "## .####"
		+ "##$ ####"
		+ "## .####"
		+ "########"
		,
		  "########"
		+ "##.###.#"
		+ "## #  .#"
		+ "## $$ @#"
		+ "##  $  #"
		+ "##  #  #"
		+ "##  ####"
		+ "########"
		,
		  "########"
		+ "#### @##"
		+ "####   #"
		+ "#. #$$ #"
		+ "#     ##"
		+ "#.  $###"
		+ "##.  ###"
		+ "########"
		,
		  "########"
		+ "# ..####"
		+ "# $    #"
		+ "#  #$# #"
		+ "# @ .$ #"
		+ "########"
		+ "########"
		+ "########"
		,
		  "########"
		+ "###  .##"
		+ "# $ # ##"
		+ "# *$  ##"
		+ "# .#@ ##"
		+ "#    ###"
		+ "#   ####"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#.  @.##"
		+ "#  $# ##"
		+ "# # $. #"
		+ "#   $# #"
		+ "####   #"
		+ "########"
		,
		  "########"
		+ "#. .####"
		+ "#.#$$ ##"
		+ "#   @ ##"
		+ "# $#  ##"
		+ "##   ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#.  ####"
		+ "# #   ##"
		+ "# . # ##"
		+ "# $*$ ##"
		+ "##@ ####"
		+ "##  ####"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#.   . #"
		+ "# # #  #"
		+ "#@$  $.#"
		+ "##### $#"
		+ "#####  #"
		+ "########"
		,
		  "########"
		+ "#  #####"
		+ "#  #####"
		+ "# .*   #"
		+ "##$    #"
		+ "## #$###"
		+ "##. @###"
		+ "########"
		,
		  "########"
		+ "## @ ###"
		+ "## .   #"
		+ "#. $.$ #"
		+ "##$# ###"
		+ "##   ###"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##   ###"
		+ "# $# ###"
		+ "# . @###"
		+ "# *   ##"
		+ "## #$ ##"
		+ "##.  ###"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##  ####"
		+ "#..$  .#"
		+ "# #$ $ #"
		+ "#@  #  #"
		+ "#####  #"
		+ "########"
		,
		  "########"
		+ "##  .@##"
		+ "##   $.#"
		+ "####*# #"
		+ "##     #"
		+ "#  $  ##"
		+ "#   ####"
		+ "########"
		,
		  "########"
		+ "##@ ####"
		+ "##  ####"
		+ "##. ####"
		+ "# $$. .#"
		+ "#  $ ###"
		+ "###  ###"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##.  ###"
		+ "## # ###"
		+ "## *$  #"
		+ "##  $. #"
		+ "##  @###"
		+ "########"
		,
		  "########"
		+ "########"
		+ "###   ##"
		+ "### #.##"
		+ "###  .##"
		+ "#@ $$ ##"
		+ "#  .$ ##"
		+ "########"
		,
		  "########"
		+ "#   @###"
		+ "# $# ###"
		+ "# * $  #"
		+ "#   ## #"
		+ "##.  . #"
		+ "###   ##"
		+ "########"
		,
		  "########"
		+ "##   @##"
		+ "##  #  #"
		+ "##.  $ #"
		+ "## $$#.#"
		+ "####  .#"
		+ "########"
		+ "########"
		,
		  "########"
		+ "########"
		+ "###. ###"
		+ "# .  ###"
		+ "#   $$ #"
		+ "## . $@#"
		+ "########"
		+ "########"
		,
		  "########"
		+ "##@.  ##"
		+ "# $$* ##"
		+ "#  #  ##"
		+ "#  #  .#"
		+ "#### # #"
		+ "####   #"
		+ "########"
		,
		  "########"
		+ "#####  #"
		+ "#####$.#"
		+ "###  . #"
		+ "###  #.#"
		+ "# $  $ #"
		+ "#   #@ #"
		+ "########"
		,
		  "########"
		+ "#  .####"
		+ "# $.. ##"
		+ "#  ##$##"
		+ "##  #  #"
		+ "##$   @#"
		+ "##  ####"
		+ "########"
		,
		  "########"
		+ "###  ###"
		+ "###  ###"
		+ "### .. #"
		+ "#  $#  #"
		+ "#  .$$ #"
		+ "#### @ #"
		+ "########"
		,
		  "########"
		+ "#   ####"
		+ "# # *@##"
		+ "#  *   #"
		+ "###$   #"
		+ "###   .#"
		+ "########"
		+ "########"
		,
		  "########"
		+ "### .  #"
		+ "# $@#. #"
		+ "#  $# ##"
		+ "#  *  ##"
		+ "##  # ##"
		+ "###   ##"
		+ "########"
		,
		  "########"
		+ "########"
		+ "########"
		+ "##  ####"
		+ "#     ##"
		+ "#  #$$@#"
		+ "#  . *.#"
		+ "########"
		,
		  "########"
		+ "##@    #"
		+ "#. #   #"
		+ "# $$$.##"
		+ "# .#  ##"
		+ "#  #####"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#      #"
		+ "# # ##*#"
		+ "# #@ $ #"
		+ "#.$ .  #"
		+ "#####  #"
		+ "#####  #"
		+ "########"
		,
		  "########"
		+ "##@   ##"
		+ "###$   #"
		+ "### .  #"
		+ "# $ #$##"
		+ "# .  .##"
		+ "####  ##"
		+ "########"
		,
		  "########"
		+ "#   ####"
		+ "#  $  ##"
		+ "##$$ .##"
		+ "##@ . ##"
		+ "### # ##"
		+ "###  .##"
		+ "########"
		,
		  "########"
		+ "#   ####"
		+ "# $$   #"
		+ "# .#.  #"
		+ "#  ## ##"
		+ "#  ##$##"
		+ "# @  .##"
		+ "########"
		,
		  "########"
		+ "########"
		+ "########"
		+ "# .  ###"
		+ "# .# ###"
		+ "# @$$  #"
		+ "# $.   #"
		+ "########"
		,
		  "########"
		+ "# @.#  #"
		+ "# .$ . #"
		+ "#  #$  #"
		+ "#  $  ##"
		+ "###  ###"
		+ "###  ###"
		+ "########"
		,
		  "########"
		+ "#    . #"
		+ "# $  $@#"
		+ "#.$.####"
		+ "#  #####"
		+ "#  #####"
		+ "#  #####"
		+ "########"
		,
		  "########"
		+ "# .  ###"
		+ "#  #@###"
		+ "#  $ ###"
		+ "##$#  ##"
		+ "#   # ##"
		+ "#. *  ##"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#### . #"
		+ "# *@ . #"
		+ "# $ #  #"
		+ "# #  $ #"
		+ "#   ####"
		+ "########"
		,
		  "########"
		+ "########"
		+ "########"
		+ "###  ###"
		+ "# .. $.#"
		+ "#  $$ @#"
		+ "####   #"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#####@ #"
		+ "##### .#"
		+ "# $ $ $#"
		+ "#   .  #"
		+ "### .  #"
		+ "########"
		,
		  "########"
		+ "#   #  #"
		+ "# #.$ $#"
		+ "#   $  #"
		+ "#####. #"
		+ "###   @#"
		+ "###   .#"
		+ "########"
		,
		  "########"
		+ "####@ ##"
		+ "###  ..#"
		+ "## $#$##"
		+ "#   $. #"
		+ "#  #   #"
		+ "#    ###"
		+ "########"
		,
		  "########"
		+ "#   @###"
		+ "# $$####"
		+ "# $ .  #"
		+ "## #.# #"
		+ "#.   # #"
		+ "#      #"
		+ "########"
		,
		  "########"
		+ "####  ##"
		+ "#### $##"
		+ "# @$.  #"
		+ "# ##   #"
		+ "#   ## #"
		+ "#   * .#"
		+ "########"
		,
		  "########"
		+ "#### @ #"
		+ "####   #"
		+ "## $ $##"
		+ "## $  ##"
		+ "#.  # ##"
		+ "#..   ##"
		+ "########"
		,
		  "########"
		+ "########"
		+ "####. @#"
		+ "#  .$  #"
		+ "# #  ###"
		+ "# $ $ .#"
		+ "####   #"
		+ "########"
		,
		  "########"
		+ "########"
		+ "#  .# @#"
		+ "# # $  #"
		+ "# $.#$ #"
		+ "## .   #"
		+ "##  ####"
		+ "########"
		,
		  "########"
		+ "########"
		+ "##     #"
		+ "##.## .#"
		+ "##*  $@#"
		+ "##  #$ #"
		+ "##  #  #"
		+ "########"
		,
		  "########"
		+ "#. #####"
		+ "# $#####"
		+ "#  #####"
		+ "# .$ @ #"
		+ "# .$ # #"
		+ "###    #"
		+ "########"
		,
		  "########"
		+ "#      #"
		+ "# #$   #"
		+ "# $ @#.#"
		+ "##$#.  #"
		+ "##    .#"
		+ "########"
		+ "########"
		,
		  "########"
		+ "#  . ###"
		+ "#    ###"
		+ "# #$$. #"
		+ "#.  ## #"
		+ "#@$ ## #"
		+ "###    #"
		+ "########"
		,
		  "  .   . "
		+ " ..  .. "
		+ "        "
		+ "      . "
		+ "     .@ "
		+ " .      "
		+ "  ..    "
		+ "    ... "
	};
}

