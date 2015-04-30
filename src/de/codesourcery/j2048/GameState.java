package de.codesourcery.j2048;

public final class GameState
{
	// grid size
	public static final int GRID_COLS = 4;
	public static final int GRID_ROWS = 4;

	public static final int EMPTY_TILE =  0x000000;

	public final ScreenState screenState;

	public final int[] grid=new int[ GRID_COLS * GRID_ROWS ];
	public int score;
	public boolean gameOver;

	public GameState(TickListenerContainer container)
	{
		screenState = new ScreenState(container);
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
		screenState.setTileValue( x, y , GameState.EMPTY_TILE );
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

		final boolean[] moved = {false};
		final Runnable run = () ->
		{
			for ( int y = 0 ; y < GameState.GRID_ROWS ; y++ )
			{
				for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileLeft(x, y);
					}
				}
			}
		};

		run.run();
		// merge left
		boolean merged = false;
		for ( int y = 0 ; y < GameState.GRID_ROWS ; y++ )
		{
			for ( int x = 1 ; x < GameState.GRID_COLS ; x++ )
			{
				final int tile = getTile(x,y);
				if ( tile != GameState.EMPTY_TILE )
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
		if ( merged ) {
			run.run();
		}
		return moved[0] | merged;
	}

	public boolean tiltRight()
	{
		final boolean[] moved = { false} ;

		final Runnable run = () ->
		{
			for ( int y = 0 ; y < GameState.GRID_ROWS ; y++ )
			{
				for ( int x = GameState.GRID_COLS -2 ; x >= 0 ; x-- )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileRight(x, y);
					}
				}
			}
		};
		run.run();

		// merge right
		boolean merged = false;
		for ( int y = 0 ; y < GameState.GRID_ROWS ; y++ )
		{
			for ( int x = GameState.GRID_COLS -2 ; x >= 0 ; x-- )
			{
				final int tile = getTile(x,y);
				if ( tile != GameState.EMPTY_TILE ) {
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
		if ( merged ) {
			run.run();
		}
		return moved[0] | merged;
	}

	public boolean tiltDown()
	{
		final boolean[] moved={false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
			{
				for ( int y = 1 ; y < GameState.GRID_ROWS ; y++ )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileDown(x, y);
					}
				}
			}
		};
		run.run();
		// merge downwards
		boolean merged = false;
		for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
		{
			for ( int y = 1 ; y < GameState.GRID_ROWS ; y++ )
			{
				final int tile = getTile(x,y);
				if ( tile != GameState.EMPTY_TILE ) {
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
		if ( merged ) {
			run.run();
		}
		return moved[0] | merged;
	}

	public boolean tiltUp()
	{
		// move up
		final boolean[] moved = {false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
			{
				for ( int y = GameState.GRID_ROWS-2 ; y >= 0 ; y-- )
				{
					if ( isOccupied(x,y) )
					{
						moved[0] |= moveTileUp(x, y);
					}
				}
			}
		};
		run.run();

		boolean merged = false;
		for ( int x = 0 ; x < GameState.GRID_COLS ; x++ )
		{
			for ( int y = GameState.GRID_ROWS-2 ; y >= 0  ; y-- )
			{
				final int tile = getTile(x,y);
				if ( tile != GameState.EMPTY_TILE ) {
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
		if ( merged ) {
			run.run();
		}
		return moved[0] | merged;
	}

	private boolean moveTileDown(int x,int y)
	{
		boolean moved = false;
		while ( y > 0 && isEmpty(x, y-1 ) )
		{
			setTileValue( x,y-1, getTile(x, y) );
			clearTile(x,y);
			y--;
			moved=true;
		}
		return moved;
	}

	private boolean moveTileUp(int x,int y)
	{
		boolean moved = false;
		final int initialX = x;
		final int initialY = y;
		while ( y < GameState.GRID_ROWS-1 && isEmpty(x, y+1 ) )
		{
			moved = true;
			internalSetTileValue( x,y+1, getTile(x, y) );
			internalClearTile(x,y);
			y++;
			moved=true;
		}
		if ( moved ) {
			screenState.moveTile(initialX,initialY,x,y-1);
		}
		return moved;
	}

	private boolean moveTileLeft(int x,int y)
	{
		boolean moved = false;
		while ( x > 0 && isEmpty(x-1, y ) )
		{
			setTileValue( x-1,y, getTile(x, y) );
			clearTile(x,y);
			x--;
			moved=true;
		}
		return moved;
	}

	private boolean moveTileRight(int x,int y)
	{
		boolean moved = false;
		while ( x < GameState.GRID_COLS-1 && isEmpty(x+1, y ) )
		{
			setTileValue( x+1,y, getTile(x, y) );
			clearTile(x,y);
			x++;
			moved=true;
		}
		return moved;
	}
}