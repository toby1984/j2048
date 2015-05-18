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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import de.codesourcery.j2048.TickListenerContainer.ITickListener;

/**
 * The class responsible for rendering the game.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class GameScreen extends JPanel implements ITickListener
{
	protected static final Color COLOR_SCORE = Color.BLACK;

	protected static final Color COLOR_BACKGROUND = Color.LIGHT_GRAY;
	protected static final Color COLOR_GRID = Color.DARK_GRAY;

	protected static final Color COLOR_TILE_BACKGROUND = Color.WHITE;
	protected static final Color COLOR_TILE_FOREGROUND = Color.blue;

	protected static final Color COLOR_GAMEOVER = Color.RED;

	protected final Color[] colors = createGradient(Color.WHITE,Color.RED,15);

	public static final int BORDER_THICKNESS = 4;

	public static final int BOARD_Y_OFFSET = 40;

	public static final int WIDTH  = BoardState.GRID_COLS * ScreenState.TILE_WIDTH  + (BoardState.GRID_COLS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;
	public static final int HEIGHT = BOARD_Y_OFFSET + BoardState.GRID_ROWS * ScreenState.TILE_HEIGHT + (BoardState.GRID_ROWS-1)*BORDER_THICKNESS + 2*BORDER_THICKNESS;

	protected final RenderedButton restartButton;

	protected final Font numberFont;
	protected final Font textFont;
	protected final Font gameOverFont;

	// off-screen buffers
	private final Object BUFFER_LOCK = new Object();

	// @GuardedBy( BUFFER_LOCK )
	private final BufferedImage[] buffers = new BufferedImage[2];
	// @GuardedBy( BUFFER_LOCK )
	private final Graphics2D[] bufferGfxs = new Graphics2D[2];

	private int bufferIndex;

	public GameScreen()
	{
		setBackground(COLOR_BACKGROUND);

		final Dimension windowSize = new Dimension(WIDTH,HEIGHT);
		setPreferredSize( windowSize );
		setMinimumSize( windowSize );
		setMaximumSize( windowSize );

		numberFont = getFont().deriveFont( Font.BOLD , 24  );
		textFont = getFont().deriveFont( Font.BOLD , 24  );
		gameOverFont = getFont().deriveFont( Font.BOLD , 32  );
		restartButton = new RenderedButton( "Restart" , numberFont , 210,5,105,30 );

		addMouseListener( new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if ( e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1 )
				{
					if ( restartButton != null && restartButton.contains( e.getPoint() ) )
					{
						restartButton.click();
					}
				}
			}
		});
	}

	public RenderedButton getRestartButton() {
		return restartButton;
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

	@SuppressWarnings("unused")
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

		setHQ(bufferGfxs[0]);
		setHQ(bufferGfxs[1]);

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

		// clear screen
		gfx.setColor( COLOR_GRID );
		gfx.fillRect( 0 , 0 , WIDTH , HEIGHT );

		// draw grid with blank tiles
		gfx.setBackground( COLOR_GRID );
		gfx.setColor( Color.WHITE );
		final Point p = new Point();
		final Rectangle r = new Rectangle();
		final int ARC = 20;
		for ( int y = 0 ; y < BoardState.GRID_ROWS ; y++ )
		{
			for ( int x = 0 ; x < BoardState.GRID_COLS ; x++ )
			{
				ScreenState.getTileLocation( x , y , p );
				final int px = p.x;
				final int py = BOARD_Y_OFFSET + p.y;
				gfx.fillRoundRect( px,py,ScreenState.TILE_WIDTH,ScreenState.TILE_HEIGHT , ARC , ARC );
			}
		}

		// draw tiles
		gfx.setFont( numberFont );
		state.screenState.visitOccupiedTiles( tile ->
		{
			r.x = tile.x;
			r.y = BOARD_Y_OFFSET + tile.y;
			r.width = ScreenState.TILE_WIDTH;
			r.height = ScreenState.TILE_HEIGHT;
			final int value = 1 << tile.value;

			gfx.setColor( colors[tile.value-1] );
			gfx.fillRoundRect( r.x , r.y , r.width, r.height , ARC , ARC  );

			gfx.setColor(COLOR_TILE_FOREGROUND);
			renderCenteredText( Integer.toString( value ) , r , gfx );

			gfx.setColor( COLOR_GRID );
			gfx.drawRoundRect( r.x , r.y , r.width, r.height , ARC , ARC  );
		});

		// render score
		gfx.setFont( textFont );
		final String text = "Score: "+state.getScore();
		gfx.setColor( Color.WHITE );
		gfx.fillRect(0,0,getWidth(), BOARD_Y_OFFSET );
		gfx.setColor(COLOR_SCORE);
		gfx.drawString( text,5,30);

		// render game over screen
		if ( state.isGameOver() ) {
			gfx.setColor(COLOR_GAMEOVER);
			gfx.setFont( gameOverFont );
			renderCenteredText( "GAME OVER !!!", new Rectangle(0,0,WIDTH,HEIGHT ), gfx );
		}

		// render button
		restartButton.render(gfx);

		swapBuffers();
	}

	public static final class RenderedButton implements ITickListener
	{
		private final Rectangle rect;
		private final BufferedImage image;
		private final Graphics2D graphics;
		private final String label;
		private final Stroke STROKE = new BasicStroke(2);

		// @GuardedBy(listeners)
		private final List<Runnable> listeners = new ArrayList<>();

		// @GuardedBy(listeners)
		private boolean isPressed;
		// @GuardedBy(listeners)
		private float timeUntilRelease;

		private final Rectangle tempRect = new Rectangle();

		public RenderedButton(String label,Font font,int x,int y,int width, int height)
		{
			this(label,font,new Rectangle(x,y,width,height));
		}

		public RenderedButton(String label,Font font,Rectangle rect)
		{
			this.label = label;
			this.rect = new Rectangle(rect);
			this.image = new BufferedImage(rect.width,rect.height,BufferedImage.TYPE_INT_RGB);
			this.graphics = image.createGraphics();
			this.graphics.setFont( font );
			graphics.setStroke( STROKE );
			setHQ( graphics );
		}

		public boolean contains(Point p) {
			return rect.contains( p );
		}

		public void addListener(Runnable r)
		{
			synchronized(listeners) {
				listeners.add(r);
			}
		}

		public void render(Graphics2D gfx)
		{
			gfx.drawImage( image, rect.x , rect.y , null );
		}

		public void click()
		{
			synchronized( listeners)
			{
				if ( ! isPressed ) {
					isPressed = true;
					listeners.forEach( Runnable::run );
					timeUntilRelease=0.1f;
				}
			}
		}

		@Override
		public boolean tick(float deltaSeconds)
		{
			final Color front;
			final Color back;
			synchronized(listeners)
			{
				if ( isPressed)
				{
					timeUntilRelease -= deltaSeconds;
					isPressed = timeUntilRelease > 0;
				}
				front = isPressed ? Color.WHITE : Color.BLACK;
				back  = isPressed ? Color.BLACK : Color.WHITE;
			}
			// fill background
			graphics.setColor( back );
			graphics.fillRect( 0 , 0 , rect.width , rect.height );
			// draw outline
			graphics.setColor( front );
			graphics.drawRoundRect( 1 , 1 , rect.width-2 , rect.height-2 ,10,10);

			// draw label
			tempRect.x = 2;
			tempRect.y = 1;
			tempRect.width = rect.width-3;
			tempRect.height = rect.height;

			renderCenteredText(label, tempRect ,graphics);
			return true;
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

	protected static void setHQ(Graphics2D graphics) {
		graphics.getRenderingHints().put(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		graphics.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.getRenderingHints().put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
	}

	@Override
	public boolean tick(float deltaSeconds) {
		restartButton.tick(deltaSeconds);
		return true;
	}
}