package de.codesourcery.j2048;

import java.awt.Dimension;
import java.util.Random;

import javax.swing.JFrame;

import de.codesourcery.j2048.IInputProvider.Action;

public class Main
{
	private final TickListenerContainer tickListeners = new TickListenerContainer();
	private final Random rnd = new Random(System.currentTimeMillis());
	private final IInputProvider inputProvider;

	public static void main(String[] args) 
	{
		new Main(new KeyboardInputProvider() ).run();
	}

	public Main(IInputProvider keyListener) {
		this.inputProvider = keyListener;
	}

	public void run()
	{
		final ScreenState screenState = new ScreenState( tickListeners );
		final BoardState state = new BoardState( screenState );
		
		restartGame(state);

		final JFrame frame = new JFrame("test");
		inputProvider.attach( frame );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final GameScreen panel = new GameScreen();
		panel.setFocusable(true);
		panel.setRequestFocusEnabled( true );
		inputProvider.attach(panel);
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
		long time = System.currentTimeMillis();
		while ( true )
		{
			final long now = System.currentTimeMillis();
			final float deltaSeconds = (now-time)/1000.0f;
			time = now;
			tickListeners.invokeTickListeners( deltaSeconds );

			// process input and advance game state
			final IInputProvider.Action action = inputProvider.getAction( state );
			if ( screenState.isInSyncWithBoardState() && action != IInputProvider.Action.NONE ) // only process input once screen state is in sync with board state
			{
				final boolean validMove = processInput( state , action );
				if ( validMove && ! state.isGameOver() )
				{
					placeRandomTile(state);
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

	private boolean processInput(BoardState board,IInputProvider.Action action)
	{
		if (action == Action.RESTART) 
		{
			restartGame(board);
			return false;
		}
		if ( board.isGameOver() || action == Action.NONE) {
			return false;
		}
		
		if (action == Action.TILT_DOWN) {
			return board.tiltDown();
		} 
		if (action == Action.TILT_LEFT) {
			return board.tiltLeft();
		} 
		if (action == Action.TILT_RIGHT) {
			return board.tiltRight(); 
		} 
		if (action == Action.TILT_UP) {
			return board.tiltUp();
		}
		return false;
	}

	private void restartGame(BoardState state)
	{
		state.reset();
		placeRandomTile(state);
	}

	private void placeRandomTile(BoardState state)
	{
		if ( state.isBoardFull() ) {
			return;
		}
		
		final int value = rnd.nextFloat() > 0.9 ? 2 : 1;

		int x,y;
		do {
			x = rnd.nextInt( BoardState.GRID_COLS );
			y = rnd.nextInt( BoardState.GRID_ROWS );
		}
		while ( state.isOccupied(x,y) );
		state.setTileValue(x,y,value);
	}
}