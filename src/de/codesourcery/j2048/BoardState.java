package de.codesourcery.j2048;

import de.codesourcery.j2048.ScreenState.Batch;

public final class BoardState
{
	// grid size
	public static final int GRID_COLS = 4;
	public static final int GRID_ROWS = 4;

	public static final int EMPTY_TILE =  0x000000;

	public final ScreenState screenState;

	public final int[] grid=new int[ GRID_COLS * GRID_ROWS ];
	private int score;
	private boolean gameOver;

	public BoardState(ScreenState screenState)
	{
		this.screenState = screenState;
		reset();
	}

	public void reset()
	{
		screenState.reset();

		gameOver = false;
		score = 0;

		for ( int i = 0 ; i < GRID_COLS*GRID_ROWS ; i++ )
		{
			grid[ i ] = EMPTY_TILE;
		}
	}

	public int getTile(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		try {
			return grid[ptr];
		} catch(ArrayIndexOutOfBoundsException e) {
			throw new ArrayIndexOutOfBoundsException("AIOOBE @ "+ptr+" , x= "+x+", y="+y);
		}
	}

	public boolean isBoardFull()
	{
		for ( int i = 0 ; i < GRID_COLS*GRID_ROWS ; i++ ) {
			if ( grid[i] == EMPTY_TILE ) {
				return false;
			}
		}
		return true;
	}

	public void setTileValue(int x,int y,int value)
	{
		internalSetTileValue(x,y,value);
		screenState.setTileValue( x , y , value );
	}

	private void internalSetTileValue(int x,int y,int value)
	{
		final int ptr = x+y*GRID_COLS;
		grid[ptr] = value;
	}

	private void clearTile(int x,int y)
	{
		internalClearTile(x,y);
		screenState.clear( x, y );
	}

	private void internalClearTile(int x,int y)
	{
		final int ptr = x+y*GRID_COLS;
		grid[ptr] = EMPTY_TILE;
	}

	public boolean isOccupied(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		return grid[ptr] != EMPTY_TILE;
	}

	public boolean isEmpty(int x,int y) {
		final int ptr = x+y*GRID_COLS;
		return grid[ptr] == EMPTY_TILE;
	}

	public boolean tiltLeft() {

		final Batch batch = screenState.startBatch();
		
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
		batch.syncPoint();
		
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
		batch.syncPoint();	
		
		if ( merged ) {
			run.run();
		}
		batch.close();
		return moved[0] | merged;
	}

	public boolean tiltRight()
	{
		final Batch batch = screenState.startBatch();
		
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
		batch.syncPoint();

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
		batch.syncPoint();
		
		if ( merged ) {
			run.run();
		}
		batch.close();		
		return moved[0] | merged;
	}

	public boolean tiltDown()
	{
		final boolean[] moved={false};
		
		final Batch batch = screenState.startBatch();
		
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
		batch.syncPoint();
		
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
		batch.syncPoint();
		if ( merged ) {
			run.run();
		}
		batch.close();
		return moved[0] | merged;
	}

	public boolean tiltUp()
	{
		final Batch batch = screenState.startBatch();
		
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
		batch.syncPoint();			
		
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
		
		batch.syncPoint();
		// try to move remaining tiles to fill gaps
		if ( merged ) {
			run.run();
		}
		
		batch.close();
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
			screenState.moveTile(initialX,initialY,x,y);
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
			screenState.moveTile(initialX,initialY,x,y);
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
			screenState.moveTile(initialX,initialY,x,y);
		}		
		return moved;
	}
	
	public boolean isGameOver() 
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
			screenState.moveTile(initialX,initialY,x,y);
		}
		return moved;
	}
	
	public int getScore() {
		return score;
	}
}