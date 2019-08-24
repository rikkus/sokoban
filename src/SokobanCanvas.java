/* vim: set ts=2 sw=2 noet: 
 * Sokoban for J2ME on SE t610
 *
 * Copyright (C) 2003 Rik Hemsley <rik@rikkus.info>
 */
import javax.microedition.lcdui.*;
import java.util.*;

public final class SokobanCanvas extends Canvas
{
	private Sokoban	sokoban;
	private int			cellSize;
	private Image		imageSokoban;
	private Image		imageWall;
	private Image		imageWater;
	private Image		imageAir;
	private Image		imageBlock;
	private int			xOffset = 0;
	private int			yOffset = 0;

	public SokobanCanvas(Sokoban sokoban) 
	{
		this.sokoban = sokoban;

		try
		{
			imageSokoban	= Image.createImage("/sokoban.png");
			imageWall			= Image.createImage("/wall.png");
			imageWater		= Image.createImage("/water.png");
			imageAir			= Image.createImage("/air.png");
			imageBlock		= Image.createImage("/block.png");
		}
		catch (java.io.IOException e)
		{
			System.out.println("Can't load images");
		}
	}

	public void reset(int width, int height)
	{
		cellSize = 15;

		xOffset = (getWidth()		- (width	* cellSize)) / 2;
		yOffset = (getHeight()	- (height	* cellSize)) / 2;

		repaint();
	}

	protected void keyRepeated(int keyCode)
	{
		keyPressed(keyCode);
	}

	protected void keyPressed(int keyCode)
	{
		//System.out.println("Key: " + Integer.toString(keyCode));

		switch (getGameAction(keyCode))
		{
			case Canvas.LEFT:
				sokoban.left();
				return;
			case Canvas.RIGHT:
				sokoban.right();
				return;
			case Canvas.UP:
				sokoban.up();
				return;
			case Canvas.DOWN:
				sokoban.down();
				return;
			default:
				break;
		}

		switch (keyCode)
		{
			case KEY_NUM2:
				sokoban.up();
				break;
			case KEY_NUM8:
			case KEY_NUM5:
				sokoban.down();
				break;
			case KEY_NUM4:
				sokoban.left();
				break;
			case KEY_NUM6:
				sokoban.right();
				break;
			case KEY_NUM1:
			case KEY_STAR:
				sokoban.undo();
				break;
			case KEY_NUM0:
				sokoban.restartLevel();
				break;
		}
	}

	public void repaint(int y, int x)
	{
		repaint
			(
			 x * cellSize	+ xOffset,
			 y * cellSize + yOffset,
			 cellSize,
			 cellSize
			);
	}

	protected void paint(Graphics g)
	{
		int clipX0	= (g.getClipX()				- xOffset)	/ cellSize;
		int clipY0	= (g.getClipY()				- yOffset)	/ cellSize;
		int clipX1	= (g.getClipWidth())							/ cellSize + clipX0;
		int clipY1	= (g.getClipHeight())							/ cellSize + clipY0;

		if (clipX0 < 0)
			clipX0 = 0;

		if (clipY0 < 0)
			clipY0 = 0;

		if (clipX1 > 8)
			clipX1 = 8;

		if (clipY1 > 8)
			clipY1 = 8;

		for (int y = clipY0; y < clipY1; ++y)
		{
			for (int x = clipX0; x < clipX1; ++x)
			{
				paintCell(g, x, y);
			}
		}
	}

	protected void paintCell(Graphics g, int x, int y)
	{
		int paintX = x * cellSize + xOffset;
		int paintY = y * cellSize + yOffset;

		switch (sokoban.data(y, x))
		{
			case ' ':
				g.drawImage(imageAir,			paintX, paintY, 0);
				break;
			case '#':
				g.drawImage(imageWall,		paintX, paintY, 0);
				break;
			case '.':
				g.drawImage(imageWater,		paintX, paintY, 0);
				break;
			case '$':
				g.drawImage(imageAir, 		paintX, paintY, 0);
				g.drawImage(imageBlock,		paintX, paintY, 0);
				break;
			case '*':
				g.drawImage(imageWater,		paintX, paintY, 0);
				g.drawImage(imageBlock,		paintX, paintY, 0);
				break;
			case '@':
				g.drawImage(imageAir,			paintX, paintY, 0);
				g.drawImage(imageSokoban,	paintX, paintY, 0);
				break;
			case '+':
				g.drawImage(imageWater,		paintX, paintY, 0);
				g.drawImage(imageSokoban,	paintX, paintY, 0);
				break;
		}
	}
}
