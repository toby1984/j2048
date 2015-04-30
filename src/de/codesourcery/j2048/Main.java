package de.codesourcery.j2048;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;

public class Main
{
	protected static final class MyKeyListener extends KeyAdapter
	{
		public final Set<Integer> pressed = new HashSet<>();
		@Override public void keyReleased(KeyEvent e) { pressed.remove( e.getKeyCode() ); }
		@Override public void keyPressed(KeyEvent e) { pressed.add( e.getKeyCode() ); }
		public boolean isPressed(int keyCode) { return pressed.contains(keyCode); }
		public void clearInput() { pressed.clear(); }
	};

	private final TickListenerContainer tickListeners = new TickListenerContainer();
	private final Random rnd = new Random(System.currentTimeMillis());
	private final MyKeyListener keyListener = new MyKeyListener();

	public static void main(String[] args) {

		new Main().run();
	}

	public void run()
	{
		final GameState state = new GameState( tickListeners );
		restartGame(state);

		final GameScreen panel = new GameScreen();

		final JFrame frame = new JFrame("test");
		frame.addKeyListener( keyListener );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		panel.setFocusable(true);
		panel.setRequestFocusEnabled( true );
		panel.addKeyListener( keyListener );
		panel.setPreferredSize( new Dimension(640,480));
		panel.setMinimumSize( new Dimension(200,200));
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setVisible( true );
		panel.requestFocus();

		gameLoop(state, panel);
	}

	private void gameLoop(final GameState state, final GameScreen panel)
	{
		// main loop
		long time = System.currentTimeMillis();
		while ( true )
		{
			final long now = System.currentTimeMillis();
			final float deltaSeconds = (now-time)/1000.0f;
			time = now;
			tickListeners.invokeTickListeners( deltaSeconds );

			// process input and advance game state
			final boolean validMove = processInput( state );
			if ( validMove )
			{
				if ( state.isBoardFull() )
				{
					state.gameOver = true;
				} else {
					setRandomTile(state);
				}
			}
			else if ( ! state.gameOver && state.isBoardFull() ) {
				state.gameOver = true;
			}

			// render
			panel.render( state );

			// sleep some time
			try
			{
				Thread.sleep( 16 );
			}
			catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	private boolean processInput(GameState state)
	{
		boolean validMove = false;

		if ( keyListener.isPressed( KeyEvent.VK_ENTER ) ) {
			restartGame(state);
		}
		else if ( keyListener.isPressed( KeyEvent.VK_A ) || keyListener.isPressed( KeyEvent.VK_LEFT ) )
		{
			if ( ! state.gameOver ) {
				System.out.println("left");
				validMove = state.tiltLeft();
			}
		}
		else if ( keyListener.isPressed( KeyEvent.VK_D ) || keyListener.isPressed( KeyEvent.VK_RIGHT ) )
		{
			if ( ! state.gameOver ) {
				System.out.println("right");
				validMove = state.tiltRight();
			}
		}
		else if ( keyListener.isPressed( KeyEvent.VK_W ) || keyListener.isPressed( KeyEvent.VK_UP ) )
		{
			if ( ! state.gameOver ) {
				validMove = state.tiltDown();
			}
		}
		else if ( keyListener.isPressed( KeyEvent.VK_S ) || keyListener.isPressed( KeyEvent.VK_DOWN ) )
		{
			if ( ! state.gameOver ) {
				validMove = state.tiltUp();
			}
		}
		keyListener.clearInput();
		return validMove;
	}

	private void restartGame(GameState state)
	{
		keyListener.clearInput();
		state.reset();

		//		state.setTile( 0 , 3 ,1 );
		//		state.setTile( 1 , 3 ,1 );
		//		state.setTile( 2 , 3 ,1 );
		//		state.setTile( 3 , 3 ,1 );
		setRandomTile(state);
	}

	private void setRandomTile(GameState state)
	{
		final int value = rnd.nextFloat() > 0.9 ? 2 : 1;
		if ( state.isBoardFull() ) {
			throw new IllegalStateException("Board is full?");
		}
		int x,y;
		do { // TODO: This approach is quite wasteful when there are a lot of tiles on the board
			x = rnd.nextInt( GameState.GRID_COLS );
			y = rnd.nextInt( GameState.GRID_ROWS );
		}
		while ( state.isOccupied(x,y) );
		state.setTileValue(x,y,value);
	}
}