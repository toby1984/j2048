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
		public boolean anyInput() { return ! pressed.isEmpty(); }
	};

	private final TickListenerContainer tickListeners = new TickListenerContainer();
	private final Random rnd = new Random(System.currentTimeMillis());
	private final MyKeyListener keyListener = new MyKeyListener();

	public static void main(String[] args) {

		new Main().run();
	}

	public void run()
	{
		final ScreenState screenState = new ScreenState( tickListeners );
		final BoardState state = new BoardState( screenState );
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

		mainLoop(state, screenState , panel);
	}

	private void mainLoop(final BoardState state, final ScreenState screenState, final GameScreen panel)
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
			if ( screenState.isInSyncWithBoardState() && keyListener.anyInput() )  // only process input once screen state is in sync with board state
			{
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
			}

			// render
			panel.render( state );

			// sleep some time
			try
			{
				Thread.sleep( 14 );
			}
			catch(InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	private boolean processInput(BoardState state)
	{
		boolean validMove = false;

		if ( keyListener.isPressed( KeyEvent.VK_ENTER ) ) {
			restartGame(state);
		}
		else if ( keyListener.isPressed( KeyEvent.VK_A ) || keyListener.isPressed( KeyEvent.VK_LEFT ) )
		{
			if ( ! state.gameOver ) {
				validMove = state.tiltLeft();
			}
		}
		else if ( keyListener.isPressed( KeyEvent.VK_D ) || keyListener.isPressed( KeyEvent.VK_RIGHT ) )
		{
			if ( ! state.gameOver ) {
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

	private void restartGame(BoardState state)
	{
		keyListener.clearInput();
		state.reset();

		setRandomTile(state);
	}

	private void setRandomTile(BoardState state)
	{
		final int value = rnd.nextFloat() > 0.9 ? 2 : 1;
		if ( state.isBoardFull() ) {
			throw new IllegalStateException("Board is full?");
		}
		int x,y;
		do {
			x = rnd.nextInt( BoardState.GRID_COLS );
			y = rnd.nextInt( BoardState.GRID_ROWS );
		}
		while ( state.isOccupied(x,y) );
		state.setTileValue(x,y,value);
	}
}