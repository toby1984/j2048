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
//		new Main(new KeyboardInputProvider() ).run();
		new Main(new AIPlayer() ).run();
	}

	public Main(IInputProvider keyListener) {
		this.inputProvider = keyListener;
	}

	public void run()
	{
		final ScreenState screenState = new ScreenState( tickListeners );
		final BoardWithScreenState state = new BoardWithScreenState( screenState );
		
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

	private void mainLoop(final BoardWithScreenState state, final ScreenState screenState, final GameScreen panel)
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
			if ( action != IInputProvider.Action.NONE && screenState.isInSyncWithBoardState() ) // only process input once screen state is in sync with board state
			{
				if (action == Action.RESTART) 
				{
					restartGame(state);
				} 
				else if ( ! state.isGameOver() )
				{
					final boolean validMove = processInput( state , action );
					if ( validMove && ! state.isGameOver() )
					{
						state.placeRandomTile(rnd);
					}
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
		if ( board.isGameOver() ) {
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
		state.placeRandomTile( rnd );
	}
}