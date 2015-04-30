package de.codesourcery.j2048;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class ScreenState implements ITickListener
{
	public static final int TILE_WIDTH = 75;
	public static final int TILE_HEIGHT = 75;

	private final List<Tile> tilesToRemove = new ArrayList<Tile>();	
	private final List<Tile> tiles = new ArrayList<Tile>();

	public final class Tile implements ITickListener
	{
			public int tileX;
			public int tileY;
			public int value;

			public int x;
			public int y;

			private final List<ITickListener> delegates=new ArrayList<>();

			protected Tile(int tileX, int tileY, int value)
			{
				this.tileX = tileX;
				this.tileY = tileY;
				this.value = value;
				updateScreenLocation( tileX , tileY );
			}
			
			public boolean hasPendingChanges() {
				return ! delegates.isEmpty();
			}

			public void moveTo(int dstX,int dstY)
			{
				queue( new TileMovingTickListener( this , dstX , dstY ) );
				queue( ctx -> 
				{
					this.tileX = dstX;
					this.tileY = dstY;					
					return false;
				} );
			}
			
			public boolean isOccupied() {
				return value != BoardState.EMPTY_TILE;
			}

			public void updateScreenLocation(int tileX,int tileY)
			{
				final int xBorderOffset = tileX*GameScreen.BORDER_THICKNESS;
				final int yBorderOffset = tileY*GameScreen.BORDER_THICKNESS;
				this.x = GameScreen.BORDER_THICKNESS + this.tileX * TILE_WIDTH  + xBorderOffset;
				this.y = GameScreen.BORDER_THICKNESS + this.tileY * TILE_HEIGHT + yBorderOffset;
			}
			
			public void destroy() 
			{
				queue( ctx -> 
				{
					tilesToRemove.add( this );				
					this.delegates.clear();					
					return false;
				});				
			}

			private void queue(ITickListener l) {
				this.delegates.add( l );
			}

			@Override
			public String toString() {
				return "Tile [tileX=" + tileX + ", tileY=" + tileY + ", value=" + value + ", x=" + x + ", y=" + y + "]";
			}

			@Override
			public boolean tick(ITickContext ctx)
			{
				final ITickListener tmp = delegates.isEmpty() ? null : delegates.get( 0 );
				if ( tmp!= null )
				{
					if ( ! tmp.tick( ctx ) )
					{
						delegates.remove(tmp);
					}
				}
				return true;
			}

			public void setValue(int tileValue) {
				queue( ctx -> 
				{
					value = tileValue;
					return false;
				});
			}
	}

	public ScreenState(TickListenerContainer container)
	{
		container.addTickListener(this);
	}

	public void reset()
	{
		tilesToRemove.clear();
		tiles.clear();
	}

	public void clear(int tileX,int tileY)
	{
		getTile(tileX,tileY,true).setValue( BoardState.EMPTY_TILE );
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
		System.out.println("setTileValue("+tileX+","+tileY+") = "+tileValue);
		Tile t = getTile(tileX,tileY,false);
		if ( t == null ) {
			tiles.add( new Tile(tileX,tileY,tileValue) );
		} else {
			t.setValue( tileValue );
		}
	}

	public void visitOccupiedTiles(Consumer<Tile> visitor)
	{
		for (int i = 0; i < tiles.size(); i++) 
		{
			final Tile t = tiles.get(i);
			if ( t.isOccupied() ) {
				visitor.accept( t  );
			}
		}
	}

	public void moveTile(int srcX,int srcY,int dstX,int dstY)
	{
		System.out.println("moveTile(): ("+srcX+","+srcY+") -> ("+dstX+","+dstY+")");
		getTile(srcX,srcY).moveTo( dstX , dstY );
	}

	@Override
	public boolean tick(ITickContext ctx) 
	{
		for ( Tile t : tiles ) {
			t.tick( ctx );
		}
		if ( ! tilesToRemove.isEmpty() ) {
			tiles.removeAll( tilesToRemove );
			tilesToRemove.clear();
		}
		return true;
	}
	
	public boolean isInSyncWithBoardState() 
	{
		for ( Tile t : tiles ) {
			if ( t.hasPendingChanges() ) {
				return false;
			}
		}
		return true;
	}
}