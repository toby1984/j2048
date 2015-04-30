package de.codesourcery.j2048;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public final class GameScreen extends JPanel
{
	private Font numberFont;
	private Font textFont;
	private Font gameOverFont;

	public static final int BORDER_THICKNESS = 1;

	public static final int BOARD_Y_OFFSET = 40;

	public static final int WIDTH  = GameState.GRID_COLS * ScreenState.TILE_WIDTH  + (GameState.GRID_COLS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;
	public static final int HEIGHT = BOARD_Y_OFFSET + GameState.GRID_ROWS * ScreenState.TILE_HEIGHT + (GameState.GRID_ROWS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;

	private final Object BUFFER_LOCK = new Object();

	// @GuardedBy( BUFFER_LOCK )
	private final BufferedImage[] buffers = new BufferedImage[2];
	// @GuardedBy( BUFFER_LOCK )
	private final Graphics2D[] bufferGfxs = new Graphics2D[2];

	private int bufferIndex;

	public GameScreen() {
		setBackground(Color.WHITE);
	}

	private Graphics2D getBackBufferGfx()
	{
		synchronized(BUFFER_LOCK)
		{
			return getBufferGfx( (bufferIndex+1) % 2 );
		}
	}

	private BufferedImage getFrontBuffer()
	{
		synchronized(BUFFER_LOCK)
		{
			return getBuffer( bufferIndex );
		}
	}

	private Graphics2D getBufferGfx(int bufferIdx)
	{
		synchronized(BUFFER_LOCK)
		{
			Graphics2D result = bufferGfxs[bufferIndex];
			if ( result == null ) {
				init();
				result = bufferGfxs[bufferIndex];
			}
			return result;
		}
	}

	private BufferedImage getBuffer(int bufferIdx)
	{
		synchronized(BUFFER_LOCK)
		{
			BufferedImage result = buffers[bufferIndex];
			if ( result == null ) {
				init();
				result = buffers[bufferIndex];
			}
			return result;
		}
	}

	private void swapBuffers()
	{
		synchronized(BUFFER_LOCK)
		{
			bufferIndex = (bufferIndex+1) % 2;
		}
	}

	private void init()
	{
		for ( Graphics2D gfx : bufferGfxs ) {
			if ( gfx != null ) {
				gfx.dispose();
			}
		}
		buffers[0] = new BufferedImage( WIDTH,HEIGHT, BufferedImage.TYPE_INT_RGB);
		buffers[1] = new BufferedImage( WIDTH,HEIGHT, BufferedImage.TYPE_INT_RGB);
		bufferGfxs[0] = buffers[0].createGraphics();
		bufferGfxs[1] = buffers[1].createGraphics();

		bufferGfxs[0].setColor( getBackground() );
		bufferGfxs[0].fillRect(0 , 0 , WIDTH , HEIGHT );

		bufferGfxs[1].setColor( getBackground() );
		bufferGfxs[1].fillRect(0 , 0 , WIDTH , HEIGHT );
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		synchronized(BUFFER_LOCK)
		{
			g.drawImage( getFrontBuffer() , 0 , 0 , null );
			BUFFER_LOCK.notifyAll();
		}
	}

	public void render(GameState state)
	{
		synchronized(BUFFER_LOCK)
		{
			doRender(state);
			repaint();
			try {
				BUFFER_LOCK.wait();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void doRender(GameState state)
	{
		final Graphics2D gfx = getBackBufferGfx();
		if ( numberFont == null ) {
			numberFont = getFont().deriveFont( Font.BOLD , 24  );
			textFont = getFont().deriveFont( Font.BOLD , 32  );
			gameOverFont = getFont().deriveFont( Font.BOLD , 64  );
		}

		// clear screen
		gfx.setColor( Color.WHITE );
		gfx.fillRect( 0 , 0 , WIDTH , HEIGHT );

		// draw grid lines
		final int boardWidth = WIDTH-5;
		final int boardHeight = HEIGHT - BOARD_Y_OFFSET-1;

		gfx.setColor(Color.RED);
		for ( int y = BOARD_Y_OFFSET ,i = 0 ; i <= GameState.GRID_ROWS ; i++, y+= ScreenState.TILE_HEIGHT+1 )
		{
			gfx.drawLine( 0, y , boardWidth , y );
		}
		for ( int x=0,i = 0 ; i <= GameState.GRID_COLS ; i++, x += ScreenState.TILE_WIDTH+1 )
		{
			gfx.drawLine( x , BOARD_Y_OFFSET , x , BOARD_Y_OFFSET+boardHeight );
		}

		// draw tiles
		gfx.setFont( numberFont );
		state.screenState.visitTiles( tile ->
		{
			final int value = 1 << tile.value;
			final Rectangle r = new Rectangle( tile.x , BOARD_Y_OFFSET + tile.y , ScreenState.TILE_WIDTH , ScreenState.TILE_HEIGHT );
			gfx.setColor(Color.BLACK);
			gfx.fillRect( r.x , r.y , r.width, r.height );
			gfx.setColor(Color.WHITE);
			renderCenteredText( Integer.toString( value ) , r , gfx );
		});

		// render score
		gfx.setColor(Color.BLACK);
		gfx.setFont( textFont );
		gfx.drawString( "Score: "+state.score,5,25);

		// render game over screen
		if ( state.gameOver ) {
			gfx.setColor(Color.RED);
			gfx.setFont( gameOverFont );
			renderCenteredText( "GAME OVER !!!", new Rectangle(0,0,WIDTH,HEIGHT ), gfx );
		}
		swapBuffers();
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
}