package de.codesourcery.j2048;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public final class GameScreen extends JPanel
{
	protected static final Color COLOR_SCORE = Color.BLACK;
	
	protected static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;
	protected static final Color COLOR_GRID = Color.DARK_GRAY;

	protected static final Color COLOR_TILE_BACKGROUND = Color.WHITE;
	protected static final Color COLOR_TILE_FOREGROUND = Color.blue;
	
	protected static final Color COLOR_GAMEOVER = Color.RED;
	
	// 1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384
	protected final Color[] colors = createGradient(Color.WHITE,Color.RED,15);
			
	public static final int BORDER_THICKNESS = 3;

	public static final int BOARD_Y_OFFSET = 40;

	public static final int WIDTH  = BoardState.GRID_COLS * ScreenState.TILE_WIDTH  + (BoardState.GRID_COLS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;
	public static final int HEIGHT = BOARD_Y_OFFSET + BoardState.GRID_ROWS * ScreenState.TILE_HEIGHT + (BoardState.GRID_ROWS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;

	private static final Stroke LINE_STROKE = new BasicStroke( BORDER_THICKNESS );

	protected Font numberFont;
	protected Font textFont;
	protected Font gameOverFont;
	
	// off-screen buffers
	private final Object BUFFER_LOCK = new Object();

	// @GuardedBy( BUFFER_LOCK )
	private final BufferedImage[] buffers = new BufferedImage[2];
	// @GuardedBy( BUFFER_LOCK )
	private final Graphics2D[] bufferGfxs = new Graphics2D[2];

	private int bufferIndex;

	public GameScreen() {
		setBackground(COLOR_BACKGROUND);
	}
	
	private static Color[] createGradient(Color start,Color end,int steps) 
	{
		float r1 = start.getRed()/255f;
		float r2 = end.getRed()/255f;
		
		float g1 = start.getGreen()/255f;
		float g2 = end.getGreen()/255f;
		
		float b1 = start.getBlue()/255f;
		float b2 = end.getBlue()/255f;
		
		float dr = (r2-r1) / steps;
		float dg = (g2-g1) / steps;
		float db = (b2-b1) / steps;
		final Color[] gradient = new Color[steps];
		float r = r1;
		float g = g1;
		float b = b1;
		for ( int i = 0 ; i < steps ; i++ ) {
			gradient[i]= new Color(r,g,b);
			r += dr;
			g += dg;
			b += db;
		}
		return gradient;
	}
	
	private static final Color fromHex(String s) {
		if ( s.startsWith("#" ) ) {
			s = s.substring(1);
		}
		int value = Integer.parseInt( s , 16 );
		return new Color((value >> 16) & 0xff , (value>>8) & 0xff, value & 0xff );
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
			Toolkit.getDefaultToolkit().sync();			
			BUFFER_LOCK.notifyAll();
		}
	}

	public void render(BoardWithScreenState state)
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

	private void doRender(BoardWithScreenState state)
	{
		final Graphics2D gfx = getBackBufferGfx();
		if ( numberFont == null ) {
			numberFont = getFont().deriveFont( Font.BOLD , 24  );
			textFont = getFont().deriveFont( Font.BOLD , 32  );
			gameOverFont = getFont().deriveFont( Font.BOLD , 32  );
		}

		// clear screen
		gfx.setColor( COLOR_BACKGROUND );
		gfx.fillRect( 0 , 0 , WIDTH , HEIGHT );

		// draw grid lines
		final int boardWidth = WIDTH;
		final int boardHeight = HEIGHT - BOARD_Y_OFFSET-1;

		gfx.setColor(COLOR_GRID);
		Stroke old = gfx.getStroke();
		gfx.setStroke( LINE_STROKE );
		for ( int y = BOARD_Y_OFFSET ,i = 0 ; i <= BoardState.GRID_ROWS ; i++, y+= ScreenState.TILE_HEIGHT+BORDER_THICKNESS )
		{
			gfx.drawLine( 0, y , boardWidth , y );
		}
		for ( int x=0,i = 0 ; i <= BoardState.GRID_COLS ; i++, x += ScreenState.TILE_WIDTH + BORDER_THICKNESS )
		{
			gfx.drawLine( x , BOARD_Y_OFFSET , x , BOARD_Y_OFFSET+boardHeight );
		}
		gfx.setStroke(old);

		// draw tiles
		gfx.setFont( numberFont );
		state.screenState.visitOccupiedTiles( tile ->
		{
			final int value = 1 << tile.value;
			final Rectangle r = new Rectangle( tile.x , BOARD_Y_OFFSET + tile.y , ScreenState.TILE_WIDTH , ScreenState.TILE_HEIGHT );
			gfx.setColor( colors[tile.value-1] );
			gfx.fillRect( r.x , r.y , r.width, r.height );
			gfx.setColor(COLOR_TILE_FOREGROUND);
			renderCenteredText( Integer.toString( value ) , r , gfx );
		});

		// render score
		gfx.setColor(COLOR_SCORE);
		gfx.setFont( textFont );
		gfx.drawString( "Score: "+state.getScore(),5,30);

		// render game over screen
		if ( state.isGameOver() ) {
			gfx.setColor(COLOR_GAMEOVER);
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