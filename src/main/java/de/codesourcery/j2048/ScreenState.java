/**
 * Copyright 2015 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.j2048;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.codesourcery.j2048.TickListenerContainer.ITickListener;

/**
 * Represents the current screen state.
 * 
 * <p>This classes main responsibility is keeping track of the various
 * tile positions on the screen and their animations.</p>
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class ScreenState implements ITickListener
{
	public static final int TILE_WIDTH = 75;
	public static final int TILE_HEIGHT = 75;

	private final List<Tile> tilesToRemove = new ArrayList<Tile>();	
	private final List<Tile> tiles = new ArrayList<Tile>();
	
	public final List<List<Runnable>> batches = new ArrayList<>();
	
	private Batch currentBatch = null;
	
	public final class Batch implements AutoCloseable 
	{
		private final List<Runnable> members = new ArrayList<>();
		
		private final boolean autoCommit;
		
		public Batch(boolean autoCommit) {
			this.autoCommit = autoCommit;
		}
		
		public void add(Runnable r) {
			members.add( r );
			if ( autoCommit ) {
				syncPoint();
			}
		}
		
		public void syncPoint() 
		{
			if ( ! members.isEmpty() ) 
			{
				batches.add( new ArrayList<>(members ) );
				members.clear();
			}
		}
		
		public void close() {
			syncPoint();
			currentBatch = null;
		}
	}
	
	public Batch startBatch() 
	{
		if ( currentBatch != null ) {
			currentBatch.close();
		}
		currentBatch = new Batch(false);
		return currentBatch;
	}
	
	private Batch currentBatch() 
	{
		if ( currentBatch == null ) {
			currentBatch = new Batch(true);
		}
		return currentBatch;
	}
	
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
			
			protected boolean hasPendingChanges() {
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

			private void updateScreenLocation(int tileX,int tileY)
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
			public boolean tick(float deltaSeconds)
			{
				final ITickListener tmp = delegates.isEmpty() ? null : delegates.get( 0 );
				if ( tmp!= null )
				{
					if ( ! tmp.tick( deltaSeconds ) )
					{
						delegates.remove(tmp);
					}
					return true;
				}
				return false;
			}

			public void setValue(int tileValue) {
				queue( ctx -> 
				{
					value = tileValue;
					return false;
				});
			}
	}
	
	public static void getTileLocation(int tileX,int tileY,Point point)
	{
		final int xBorderOffset = tileX*GameScreen.BORDER_THICKNESS;
		final int yBorderOffset = tileY*GameScreen.BORDER_THICKNESS;
		point.x = GameScreen.BORDER_THICKNESS + tileX * TILE_WIDTH  + xBorderOffset;
		point.y = GameScreen.BORDER_THICKNESS + tileY * TILE_HEIGHT + yBorderOffset;
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
		currentBatch().add( () -> getTile(tileX,tileY,true).destroy() );
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
		currentBatch().add( () -> 
		{
			Tile t = getTile(tileX,tileY,false);
			if ( t == null ) 
			{
				tiles.add( new Tile(tileX,tileY,tileValue) );
			} else {
				t.setValue( tileValue );
			}
		});
	}

	public void visitOccupiedTiles(Consumer<Tile> visitor)
	{
		for (int i = 0; i < tiles.size(); i++) 
		{
			visitor.accept( tiles.get(i)  );
		}
	}

	public void moveTile(int srcX,int srcY,int dstX,int dstY)
	{
		currentBatch().add( () -> getTile(srcX,srcY).moveTo( dstX , dstY ) );
	}

	@Override
	public boolean tick(float deltaSeconds) 
	{
		boolean tilesBusy = false;
		for ( Tile t : tiles ) {
			tilesBusy |= t.tick( deltaSeconds );
		}
		if ( ! tilesToRemove.isEmpty() ) {
			tiles.removeAll( tilesToRemove );
			tilesToRemove.clear();
		}
		if ( ! tilesBusy && ! batches.isEmpty() ) 
		{
			batches.remove(0).forEach( Runnable::run );
		}
		return true;
	}
	
	public boolean isInSyncWithBoardState() 
	{
		if ( ! batches.isEmpty() ) {
			return false;
		}
		for ( Tile t : tiles ) {
			if ( t.hasPendingChanges() ) {
				return false;
			}
		}
		return true;
	}
}