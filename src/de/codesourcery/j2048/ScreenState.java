package de.codesourcery.j2048;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class ScreenState
{
	public static final int TILE_WIDTH = 75;
	public static final int TILE_HEIGHT = 75;

	private final TickListenerContainer container;
	private final List<Tile> tiles = new ArrayList<Tile>();

	public final class Tile implements ITickListener
	{
			public int tileX;
			public int tileY;
			public int value;

			public int x;
			public int y;

			private ITickListener delegate = null;
			public boolean discarded = false;

			protected Tile(int tileX, int tileY, int value)
			{
				this.tileX = tileX;
				this.tileY = tileY;
				this.value = value;
				updateScreenLocation( tileX , tileY );
			}

			public void moveTo(int dstX,int dstY)
			{
				final ITickListener l = new TileMovingTickListener( this , dstX , dstY );
				if ( delegate == null ) {
					delegate = l;
				} else {
					delegate = new ChainingTickListener( this.delegate , l );
				}
				this.tileX = dstX;
				this.tileY = dstY;
			}

			public void updateScreenLocation(int tileX,int tileY)
			{
				final int xBorderOffset = tileX*GameScreen.BORDER_THICKNESS;
				final int yBorderOffset = tileY*GameScreen.BORDER_THICKNESS;
				this.x = GameScreen.BORDER_THICKNESS + this.tileX * TILE_WIDTH  + xBorderOffset;
				this.y = GameScreen.BORDER_THICKNESS + this.tileY * TILE_HEIGHT + yBorderOffset;
			}

			public void discard()
			{
				container.removeTickListener( this );
				tiles.remove( this );
				this.discarded = true;
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
	}

	public void reset()
	{
		for ( Tile t : tiles ) {
			t.discard();
		}
	}

	public void discard(int tileX,int tileY)
	{
		Tile t = getTile(tileX,tileY,false);
		if ( t != null ) {
			t.discard();
		}
	}

	private Tile getTile(int x,int y)
	{
		return getTile(x,y,true);
	}

	private Tile getTile(int x,int y,boolean failOnMissing)
	{
		for (int i = 0; i < tiles.size(); i++) {
			final Tile t = tiles.get(i);
			if ( t.tileX == x && t.tileY == y )
			{
				return t;
			}
		}
		if ( failOnMissing ) {
			throw new IllegalStateException("No tile "+x+","+y);
		}
		return null;
	}

	public void setTileValue(int tileX,int tileY,int tileValue)
	{
		Tile t = getTile(tileX,tileY,false);
		if ( t == null ) {
			t = new Tile(tileX,tileY,tileValue);
			tiles.add( t );
			container.addTickListener( t );
		} else {
			t.value = tileValue;
		}
	}

	public void visitTiles(Consumer<Tile> visitor)
	{
		for (int i = 0; i < tiles.size(); i++) {
			visitor.accept( tiles.get(i) );
		}
	}

	public void moveTile(int srcX,int srcY,int dstX,int dstY)
	{
		getTile(srcX,srcY).moveTo( dstX , dstY );
	}
}