package de.codesourcery.j2048;

import java.util.function.Consumer;

import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class ScreenState
{
	public static final int TILE_WIDTH = 75;
	public static final int TILE_HEIGHT = 75;

	private final TickListenerContainer container;
	private final Tile[] tiles = new Tile[ GameState.GRID_COLS*GameState.GRID_ROWS ];

	public final class Tile implements ITickListener
	{
			public int tileX;
			public int tileY;
			public int value;

			public int x;
			public int y;

			private ITickListener delegate = null;

			protected Tile(int tileX, int tileY, int value)
			{
				this.tileX = tileX;
				this.tileY = tileY;
				this.value = value;
				updateScreenLocation( tileX , tileY );
			}

			public void updateScreenLocation(int tileX,int tileY)
			{
				final int xBorderOffset = tileX*GameScreen.BORDER_THICKNESS;
				final int yBorderOffset = tileY*GameScreen.BORDER_THICKNESS;
				this.x = GameScreen.BORDER_THICKNESS + this.tileX * TILE_WIDTH  + xBorderOffset;
				this.y = GameScreen.BORDER_THICKNESS + this.tileY * TILE_HEIGHT + yBorderOffset;
			}

			public boolean isOccupied() {
				return value != GameState.EMPTY_TILE;
			}

			public void reset()
			{
				this.delegate = null;
			}

			@Override
			public String toString() {
				return "Tile [tileX=" + tileX + ", tileY=" + tileY + ", value=" + value + ", x=" + x + ", y=" + y + "]";
			}

			@Override
			public boolean tick(ITickContext ctx)
			{
				ITickListener tmp = delegate;
				if ( tmp!= null )
				{
					if ( ! tmp.tick( ctx ) )
					{
						delegate = null;
					}
				}
				return true;
			}

	}

	public ScreenState(TickListenerContainer container)
	{
		this.container=container;
		for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
		{
			for ( int y = 0 ; y < GameState.GRID_ROWS ; y++ )
			{
				int ptr = x + y*GameState.GRID_COLS;
				tiles[ptr] = new Tile(x,y,GameState.EMPTY_TILE);
			}
		}
		for ( Tile t : tiles ) {
			container.addTickListener( t );
		}
	}

	public void reset()
	{
		for ( Tile t : tiles ) {
			t.reset();
		}
	}

	public void discard(int tileX,int tileY)
	{
		for ( Tile t : tiles )
		{
			if ( t.tileX == tileX && t.tileY == tileY )
			{
				t.value = GameState.EMPTY_TILE;
				return;
			}
		}
	}

	public void setTileValue(int tileX,int tileY,int tileValue)
	{
		for (int i = 0; i < tiles.length; i++)
		{
			final Tile t = tiles[i];
			if ( t.tileX == tileX && t.tileY == tileY )
			{
				System.out.println("set(): Updating tile value of "+t+" to "+tileValue);
				t.value = tileValue;
				return;
			}
		}
	}

	public void visitTiles(Consumer<Tile> visitor)
	{
		for (int i = 0; i < tiles.length; i++) {
			Tile t = tiles[i];
			if ( t.isOccupied() ) {
				visitor.accept(t);
			}
		}
	}
}