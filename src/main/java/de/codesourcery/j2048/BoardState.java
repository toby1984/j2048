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

import java.util.Random;

/**
 * The game/board state.
 * 
 * <p>Instead of storing the tile value directly, the logarithm is stored so
 * if tile == 3 this is actually 2^3.</p>
 * 
 * <p>This class comes with various subclassing hooks that are used by the 
 * {@link BoardWithScreenState} class to generate animations for the board's 
 * state transitions.</p>
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class BoardState
{
	// grid size
	public static final int GRID_COLS = 4;
	public static final int GRID_ROWS = 4;

	public static final int EMPTY_TILE =  0x000000;

	public final int[] board=new int[ GRID_COLS * GRID_ROWS ];
	private int score;
	private boolean gameOver;
	private int tileCount;

	public BoardState() {
	}

	public final BoardState createCopy() 
	{
		final BoardState copy = new BoardState();
		System.arraycopy( this.board , 0 , copy.board , 0 , GRID_COLS * GRID_ROWS );
		copy.score = this.score;
		copy.gameOver = this.gameOver;
		copy.tileCount = this.tileCount;
		return copy;
	}	
	
	public final int getTileCount() 
	{
		return tileCount;
	}
	
	public final int getHighestTileValue() 
	{
		int result = 0;
		for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
		{
			for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
			{
				final int tile = getTile(x, y);
				if ( tile != BoardState.EMPTY_TILE ) 
				{
					result = Math.max( result , 1<<tile );
				}
			}
		}
		return result;
	}	
	
	public final void placeRandomTile(Random rnd) 
	{
		if ( isBoardFull() ) {
			return;
		}
		
		final int value = rnd.nextFloat() > 0.9 ? 2 : 1;

		int x,y;
		do {
			x = rnd.nextInt( BoardState.GRID_COLS );
			y = rnd.nextInt( BoardState.GRID_ROWS );
		}
		while ( isOccupied(x,y) );
		setTileValue(x,y,value);
	}

	public final void reset()
	{
		resetScreenState();

		gameOver = false;
		score = 0;
		tileCount = 0;

		for ( int i = 0 ; i < GRID_COLS*GRID_ROWS ; i++ )
		{
			board[i] = EMPTY_TILE;
		}
	}

	public final int getTile(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		return board[ptr];
	}

	public final boolean isBoardFull()
	{
		return tileCount == GRID_COLS*GRID_ROWS;
	}

	public final void setTileValue(int x,int y,int value)
	{
		internalSetTileValue(x,y,value);
		setScreenState(x, y, value);
	}

	private void internalSetTileValue(int x,int y,int value)
	{
		final int ptr = x+y*GRID_COLS;
		if ( board[ptr] == EMPTY_TILE ) 
		{
			tileCount++;
		}
		board[ptr] = value;
	}

	private void clearTile(int x,int y)
	{
		internalClearTile(x,y);
		clearScreenState(x,y);
	}

	private void internalClearTile(int x,int y)
	{
		final int ptr = x+y*GRID_COLS;
		if ( board[ptr] != EMPTY_TILE ) {
			tileCount--;
		}
		board[ptr] = EMPTY_TILE;
	}

	public final boolean isOccupied(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		return board[ptr] != EMPTY_TILE;
	}

	public final boolean isEmpty(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		return board[ptr] == EMPTY_TILE;
	}

	public final boolean tiltLeft() {

		startBatch();

		final boolean[] moved = {false};
		final Runnable run = () ->
		{
			for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
			{
				for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileLeft(x, y);
					}
				}
			}
		};

		run.run();
		sync();

		// merge left
		boolean merged = false;
		for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
		{
			for ( int x = 1 ; x < BoardState.GRID_COLS ; x++ )
			{
				final int tile = getTile(x,y);
				if ( tile != BoardState.EMPTY_TILE )
				{
					final int neightbourTile = getTile( x-1 , y );
					if ( neightbourTile == tile )
					{
						score += 1<<(tile+1);
						setTileValue( x-1 , y , tile+1);
						clearTile( x , y );
						merged = true;
					}
				}
			}
		}
		sync();	

		if ( merged ) {
			run.run();
		}
		close();
		return moved[0] | merged;
	}

	public final boolean tiltRight()
	{
		startBatch();

		final boolean[] moved = { false} ;
		final Runnable run = () ->
		{
			for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
			{
				for ( int x = BoardState.GRID_COLS -2 ; x >= 0 ; x-- )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileRight(x, y);
					}
				}
			}
		};
		run.run();
		sync();

		// merge right
		boolean merged = false;
		for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
		{
			for ( int x = BoardState.GRID_COLS -2 ; x >= 0 ; x-- )
			{
				final int tile = getTile(x,y);
				if ( tile != BoardState.EMPTY_TILE ) {
					final int neightbourTile = getTile( x+1 , y );
					if ( neightbourTile == tile )
					{
						score += 1<<(tile+1);
						setTileValue( x+1 , y , tile+1 );
						clearTile( x , y );
						merged = true;
					}
				}
			}
		}
		sync();

		if ( merged ) {
			run.run();
		}
		close();		
		return moved[0] | merged;
	}

	public final boolean tiltDown()
	{
		startBatch();

		final boolean[] moved={false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
			{
				for ( int y = 1 ; y < BoardState.GRID_ROWS ; y++ )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileDown(x, y);
					}
				}
			}
		};
		run.run();
		sync();

		// merge downwards
		boolean merged = false;
		for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
		{
			for ( int y = 1 ; y < BoardState.GRID_ROWS ; y++ )
			{
				final int tile = getTile(x,y);
				if ( tile != BoardState.EMPTY_TILE ) {
					final int neightbourTile = getTile( x , y - 1 );
					if ( neightbourTile == tile )
					{
						score += 1<<(tile+1);
						setTileValue( x, y - 1  , tile+1 );
						clearTile( x , y );
						merged = true;
					}
				}
			}
		}
		sync();
		if ( merged ) {
			run.run();
		}
		close();
		return moved[0] | merged;
	}

	public final boolean tiltUp()
	{
		startBatch();

		// move tiles up
		final boolean[] moved = {false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
			{
				for ( int y = BoardState.GRID_ROWS-2 ; y >= 0 ; y-- )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileUp(x, y);
					}
				}
			}		
		};

		run.run();
		sync();			

		// merge adjacent tiles
		boolean merged = false;
		for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
		{
			for ( int y = BoardState.GRID_ROWS-2 ; y >= 0  ; y-- )
			{
				final int tile = getTile(x,y);
				if ( tile != BoardState.EMPTY_TILE ) {
					final int neightbourTile = getTile( x , y + 1 );
					if ( neightbourTile == tile )
					{
						score += 1<<(tile+1);
						setTileValue( x, y +1 , tile+1 );
						clearTile( x , y );
						merged = true;
					}
				}
			}
		}

		sync();
		// try to move remaining tiles to fill gaps
		if ( merged ) {
			run.run();
		}

		close();
		return moved[0] | merged;
	}

	private boolean moveTileDown(int x,int y)
	{
		boolean moved = false;
		final int initialX = x;
		final int initialY = y;
		while ( y > 0 && isEmpty(x, y-1 ) )
		{
			internalSetTileValue( x,y-1, getTile(x, y) );
			internalClearTile(x,y);
			y--;
			moved=true;
		}
		if ( moved ) {
			moveTile(initialX,initialY,x,y);
		}		
		return moved;
	}

	private boolean moveTileUp(int x,int y)
	{
		boolean moved = false;
		final int initialX = x;
		final int initialY = y;
		while ( y < BoardState.GRID_ROWS-1 && isEmpty(x, y+1 ) )
		{
			moved = true;
			internalSetTileValue( x,y+1, getTile(x, y) );
			internalClearTile(x,y);
			y++;
			moved=true;
		}
		if ( moved ) {
			moveTile(initialX,initialY,x,y);
		}
		return moved;
	}

	private boolean moveTileLeft(int x,int y)
	{
		boolean moved = false;
		final int initialX = x;
		final int initialY = y;
		while ( x > 0 && isEmpty(x-1, y ) )
		{
			internalSetTileValue( x-1,y, getTile(x, y) );
			internalClearTile(x,y);
			x--;
			moved=true;
		}
		if ( moved ) {
			moveTile(initialX,initialY,x,y);
		}		
		return moved;
	}

	private boolean moveTileRight(int x,int y)
	{
		boolean moved = false;
		final int initialX = x;
		final int initialY = y;
		while ( x < BoardState.GRID_COLS-1 && isEmpty(x+1, y ) )
		{
			internalSetTileValue( x+1,y, getTile(x, y) );
			internalClearTile(x,y);
			x++;
			moved=true;
		}
		if ( moved ) {
			moveTile(initialX,initialY,x,y);
		}
		return moved;
	}

	public final boolean isGameOver() 
	{
		if ( gameOver ) {
			return true;
		}
		if ( ! isBoardFull() ) {
			return false;
		}

		// board is full, check whether any two tiles can be merged
		for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ ) 
		{
			for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ ) 
			{
				final int tile = getTile(x, y);
				if ( x-1 >= 0 ) { // check left neighbor
					if ( getTile(x-1,y) == tile ) {
						return false;
					}
				}
				if ( x+1 < BoardState.GRID_COLS ) { // check right neighbor
					if ( getTile(x+1,y) == tile ) {
						return false;
					}
				}
				if ( y-1 >= 0 ) { // check top neighbor
					if ( getTile(x,y-1) == tile ) {
						return false;
					}
				}		
				if ( y+1 < BoardState.GRID_ROWS ) { // check bottom neighbor
					if ( getTile(x,y+1) == tile ) {
						return false;
					}
				}					
			}
		}
		gameOver = true;
		return true;
	}	


	public final int getScore() {
		return score;
	}

	// overridable stuff
	protected void resetScreenState() {
	}	

	protected void moveTile(int initialX,int initialY,int x,int y) {
	}

	protected void startBatch() {
	}

	protected void sync() {
	}	

	protected void close() {
	}

	protected void clearScreenState(int x,int y) {
	}

	protected void setScreenState(int x, int y, int value) {
	}	
}