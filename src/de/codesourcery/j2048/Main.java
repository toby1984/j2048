package de.codesourcery.j2048;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main
{
	private final Random rnd = new Random(System.currentTimeMillis());
	private final GameState state = new GameState();

	// grid size
	protected static final int GRID_COLS = 4;
	protected static final int GRID_ROWS = 4;

	protected static final int BORDER_TILE = 0xffffff;
	protected static final int EMPTY_TILE =  0x000000;

	protected static enum Direction
	{
		DOWN(0,-1),
		UP(0,1),
		LEFT(-1,0),
		RIGHT(1,0);

		public final int xInc;
		public final int yInc;

		private Direction(int xInc, int yInc) {
			this.xInc = xInc;
			this.yInc = yInc;
		}
	}

	@FunctionalInterface
	protected interface ITileVisitor
	{
		public void visit(int x,int y,int tileValue);
	}

	protected interface ITileVisitorWithResult<T>
	{
		public boolean visit(int x,int y,int tileValue);

		public T getResult();
	}

	protected static final class GameState
	{
		public final int[] grid;
		public int score;
		public boolean gameOver;

		public GameState()
		{
			this.grid = new int[ GRID_COLS * GRID_ROWS ];
			reset();
		}

		public void reset()
		{
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
			final ITileVisitorWithResult<Boolean> r = new ITileVisitorWithResult<Boolean>() {

				private boolean result = true;

				@Override
				public boolean visit(int x, int y, int tileValue) {
					if ( tileValue == EMPTY_TILE ) {
						result = false;
						return false;
					}
					return true;
				}

				@Override
				public Boolean getResult() { return result; }
			};
			return visitVisibleTiles( r );
		}

		public void setTile(int x,int y,int value)
		{
			final int ptr = x+y*GRID_COLS;
			grid[ptr] = value;
		}

		public void clearTile(int x,int y)
		{
			final int ptr = x+y*GRID_COLS;
			grid[ptr] = EMPTY_TILE;
		}

		public boolean isOccupied(int x,int y) {
			final int ptr = x+y*GRID_COLS;
			return grid[ptr] != EMPTY_TILE;
		}

		public <T> T visitVisibleTiles(ITileVisitorWithResult<T> v)
		{
outer:
			for ( int y = 0 ; y < GRID_ROWS ; y++ )
			{
				for ( int x = 0 ; x < GRID_COLS ; x++ )
				{
					final int ptr = y*GRID_COLS + x;
					if ( ! v.visit( x,y, grid[ ptr ] ) ) {
						break outer;
					}
				}
			}
			return v.getResult();
		}

		public void visitVisibleNonEmptyTiles(ITileVisitor v)
		{
			for ( int y = 0 ; y < GRID_ROWS ; y++ )
			{
				for ( int x = 0 ; x < GRID_COLS ; x++ )
				{
					final int ptr = x+y*GRID_COLS;
					int value = grid[ptr];
					if ( value != EMPTY_TILE )
					{
						v.visit( x,y, value );
					}
				}
			}
		}
	}

	protected static final class MyPanel extends JPanel
	{
		private final GameState state;
		private Font numberFont;
		private Font textFont;
		private Font gameOverFont;

		public MyPanel(GameState state) {
			this.state = state;
			setBackground(Color.WHITE);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D gfx = (Graphics2D) g;
			if ( numberFont == null ) {
				numberFont = getFont().deriveFont( Font.BOLD , 24  );
				textFont = getFont().deriveFont( Font.BOLD , 32  );
				gameOverFont = getFont().deriveFont( Font.BOLD , 64  );				
			}
			super.paintComponent(gfx);

			g.setFont( numberFont );

			// draw grid lines
			final int yOffset = 40;			
			final int width = getWidth()-5;
			final int height = getHeight() - yOffset-1;

			final int widthPerCell  = width / GRID_COLS;
			final int heightPerCell = height / GRID_ROWS;

			gfx.setColor(Color.BLACK);
			for ( int y = 0 ; y <= GRID_ROWS ; y++ )
			{
				gfx.drawLine( 0, yOffset+y*heightPerCell , width , yOffset+y*heightPerCell );
			}
			for ( int x = 0 ; x <= GRID_COLS ; x++ )
			{
				gfx.drawLine( x*widthPerCell , yOffset , x*widthPerCell , yOffset+height );
			}			

			state.visitVisibleNonEmptyTiles( (x,y,tileValue) ->
			{
				final int value = 1 << tileValue;
				final Rectangle r = new Rectangle( x*widthPerCell, yOffset + y*heightPerCell, widthPerCell , heightPerCell );
				renderCenteredText( Integer.toString( value ) , r , gfx );
			});

			g.setColor(Color.BLACK);
			g.setFont( textFont );
			g.drawString( "Score: "+state.score,5,25);
			
			if ( state.gameOver ) {
				g.setColor(Color.RED);
				g.setFont( gameOverFont );
				renderCenteredText( "GAME OVER !!!", new Rectangle(0,0,getWidth(),getHeight() ), gfx );
			}
		}
	}

	protected static void renderCenteredText(String text, Rectangle area, Graphics2D gfx)
	{
		final LineMetrics lineMetrics = gfx.getFontMetrics().getLineMetrics( text ,  gfx );
		final Rectangle2D bounds = gfx.getFontMetrics().getStringBounds( text , gfx );
		double cx = area.getX() + area.getWidth()/2f - bounds.getWidth()/2f;

		double ascent = lineMetrics.getAscent();
		double descent = lineMetrics.getDescent();
		final double top = area.getY();
		final double bottom = area.getY()+area.getHeight();
		final double cy=(top+((bottom+1-top)/2) - ((ascent + descent)/2)) + ascent;
		gfx.drawString( text , (int) cx , (int) cy );
	}

	public static void main(String[] args) {

		new Main().run();
	}

	public void run()
	{
		restartGame();

		final MyPanel panel = new MyPanel(state);

		final KeyListener l = new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				boolean generateTile = false;
				switch( e.getKeyCode() )
				{
					case KeyEvent.VK_ENTER:
						restartGame();
						break;                	
					case KeyEvent.VK_A:
					case KeyEvent.VK_LEFT:
						if ( ! state.gameOver ) {
							generateTile = tilt(Direction.LEFT);
						}
						break;
					case KeyEvent.VK_D:
					case KeyEvent.VK_RIGHT:
						if ( ! state.gameOver ) {
							generateTile = tilt(Direction.RIGHT);
						}
						break;
					case KeyEvent.VK_W:
					case KeyEvent.VK_UP:
						if ( ! state.gameOver ) {
							generateTile = tilt(Direction.DOWN);
						}
						break;
					case KeyEvent.VK_S:
					case KeyEvent.VK_DOWN:
						if ( ! state.gameOver ) {
							generateTile = tilt(Direction.UP);
						}
						break;
					default:
				}
				if ( generateTile ) 
				{
					if ( state.isBoardFull() ) 
					{
						state.gameOver = true;
					} else {
						setRandomTile();
					}
				} else if ( ! state.gameOver && state.isBoardFull() ) {
					state.gameOver = true;
				}
				panel.repaint();                
			}
		};

		final JFrame frame = new JFrame("test");
		frame.addKeyListener( l );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel.setFocusable(true);
		panel.setRequestFocusEnabled( true );
		panel.addKeyListener( l );
		panel.setPreferredSize( new Dimension(640,480));
		panel.setMinimumSize( new Dimension(200,200));
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setVisible( true );
		panel.requestFocus();
	}

	@FunctionalInterface
	protected interface IMoveFunction {
		public void moveTile(int x,int y);
	}

	private boolean tilt(Direction direction)
	{
		switch( direction )
		{
			case UP:
				return tiltUp();
			case DOWN:
				return tiltDown();
			case LEFT:
				return tiltLeft();
			case RIGHT:
				return tiltRight();
			default:
				throw new RuntimeException();
		}
	}

	private boolean tiltLeft() {

		final boolean[] moved = {false};
		final Runnable run = () -> 
		{
			for ( int y = 0 ; y < GRID_ROWS ; y++ ) 		
			{
				for ( int x = 0 ; x < GRID_COLS ; x++ )			
				{
					if ( state.getTile(x,y) != EMPTY_TILE ) 
					{
						moved[0] |= moveTileLeft(x, y);
					}
				}
			}
		};

		run.run();
		// merge left
		boolean merged = false;
		for ( int y = 0 ; y < GRID_ROWS ; y++ ) 		
		{
			for ( int x = 1 ; x < GRID_COLS ; x++ ) 
			{
				final int tile = state.getTile(x,y);
				if ( tile != EMPTY_TILE ) 
				{
					final int neightbourTile = state.getTile( x-1 , y );
					if ( neightbourTile == tile )
					{
						state.score += 1<<(tile+1);
						state.setTile( x-1 , y , tile+1);
						state.clearTile( x , y );
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

	private boolean tiltRight() 
	{
		final boolean[] moved = { false} ;

		final Runnable run = () -> {for ( int y = 0 ; y < GRID_ROWS ; y++ ) 		
		{
			for ( int x = GRID_COLS -2 ; x >= 0 ; x-- )			
			{
				if ( state.getTile(x,y) != EMPTY_TILE ) 
				{
					moved[0] |=moveTileRight(x, y);
				}
			}
		}};
		run.run();

		// merge right
		boolean merged = false;
		for ( int y = 0 ; y < GRID_ROWS ; y++ ) 		
		{
			for ( int x = GRID_COLS -2 ; x >= 0 ; x-- ) 
			{
				final int tile = state.getTile(x,y);
				if ( tile != EMPTY_TILE ) {
					final int neightbourTile = state.getTile( x+1 , y );
					if ( neightbourTile == tile )
					{
						state.score += 1<<(tile+1);
						state.setTile( x+1 , y , tile+1 );
						state.clearTile( x , y );
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

	private boolean tiltDown() 
	{
		final boolean[] moved={false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < GRID_COLS ; x++ )
			{
				for ( int y = 1 ; y < GRID_ROWS ; y++ ) 
				{
					if ( state.getTile(x,y) != EMPTY_TILE ) 
					{
						moved[0] |= moveTileDown(x, y);
					}
				}
			}			
		};
		run.run();
		// merge downwards
		boolean merged = false;
		for ( int x = 0 ; x < GRID_COLS ; x++ ) 
		{
			for ( int y = 1 ; y < GRID_ROWS ; y++ ) 
			{
				final int tile = state.getTile(x,y);
				if ( tile != EMPTY_TILE ) {
					final int neightbourTile = state.getTile( x , y - 1 );
					if ( neightbourTile == tile )
					{
						state.score += 1<<(tile+1);
						state.setTile( x, y - 1  , tile+1 );
						state.clearTile( x , y );
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

	private boolean tiltUp() 
	{
		// move up
		final boolean[] moved = {false};
		final Runnable run = () -> {
			for ( int x = 0 ; x < GRID_COLS ; x++ )
			{
				for ( int y = GRID_ROWS-2 ; y >= 0 ; y-- ) 
				{
					if ( state.getTile(x,y) != EMPTY_TILE ) 
					{
						moved[0] |= moveTileUp(x, y);
					}
				}
			}			
		};
		run.run();

		boolean merged = false;
		for ( int x = 0 ; x < GRID_COLS ; x++ ) 
		{
			for ( int y = GRID_ROWS-2 ; y >= 0  ; y-- ) 
			{
				final int tile = state.getTile(x,y);
				if ( tile != EMPTY_TILE ) {
					final int neightbourTile = state.getTile( x , y + 1 );
					if ( neightbourTile == tile )
					{
						state.score += 1<<(tile+1);
						state.setTile( x, y +1 , tile+1 );
						state.clearTile( x , y );
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
		while ( y > 0 && state.getTile(x, y-1 ) == EMPTY_TILE) 
		{
			state.setTile( x,y-1, state.getTile(x, y) );
			state.clearTile(x,y);
			y--;
			moved=true;
		}
		return moved;
	}

	private boolean moveTileUp(int x,int y) 
	{
		boolean moved = false;
		while ( y < GRID_ROWS-1 && state.getTile(x, y+1 ) == EMPTY_TILE) 
		{
			moved = true;
			state.setTile( x,y+1, state.getTile(x, y) );
			state.clearTile(x,y);
			y++;
			moved=true;
		}
		return moved;
	}	

	private boolean moveTileLeft(int x,int y) 
	{
		boolean moved = false;
		while ( x > 0 && state.getTile(x-1, y ) == EMPTY_TILE) 
		{
			state.setTile( x-1,y, state.getTile(x, y) );
			state.clearTile(x,y);
			x--;
			moved=true;
		}
		return moved;
	}	

	private boolean moveTileRight(int x,int y) 
	{
		boolean moved = false;
		while ( x < GRID_COLS-1 && state.getTile(x+1, y ) == EMPTY_TILE) 
		{
			state.setTile( x+1,y, state.getTile(x, y) );
			state.clearTile(x,y);
			x++;
			moved=true;
		}
		return moved;
	}	

	private void restartGame() 
	{
		state.reset();

		//		state.setTile( 2 , 1 ,1 );		
		//		state.setTile( 2 , 2 ,1 );
		//		state.setTile( 2 , 3 ,1 );
		setRandomTile();
	}

	private void setRandomTile() {

		int value;
		if ( rnd.nextFloat() > 0.9 ) {
			value = 2; // 2^2 = 4
		} else {
			value = 1; // 2^1 = 2
		}

		if ( state.isBoardFull() ) {
			throw new IllegalStateException("Board is full?");
		}
		int x,y;
		do {
			x = rnd.nextInt( GRID_COLS );
			y = rnd.nextInt( GRID_ROWS );
		} while ( state.isOccupied(x,y) );
		state.setTile(x,y,value);
	}
}