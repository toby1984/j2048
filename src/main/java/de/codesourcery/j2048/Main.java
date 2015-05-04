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

import java.util.Random;

import javax.swing.JFrame;

import de.codesourcery.j2048.IInputProvider.Action;

/**
 * Bootstrapping / executable main application.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class Main
{
	public static boolean USE_AI = false;
	
	private final TickListenerContainer tickListeners = new TickListenerContainer();
	private final Random rnd = new Random(System.currentTimeMillis());
	private final IInputProvider inputProvider;
	
	private volatile IInputProvider.Action uiAction = IInputProvider.Action.NONE;
	
	public static void main(String[] args) 
	{
		if ( args.length < 1 || ! args[0].equalsIgnoreCase("-ai" ) ) 
		{
			new Main(new KeyboardInputProvider() ).run();
		} else {
			USE_AI = true;
			new Main(new AIPlayer() ).run();
		}
	}

	public Main(IInputProvider keyListener) {
		this.inputProvider = keyListener;
	}

	public void run()
	{
		final ScreenState screenState = new ScreenState( tickListeners );
		final BoardWithScreenState state = new BoardWithScreenState( screenState );
		
		restartGame(state);

		final JFrame frame = new JFrame("j2048 (C) 2015 by tobias.gierke@code-sourcery.de");
		inputProvider.attach( frame );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final GameScreen panel = new GameScreen();
		panel.getRestartButton().addListener( () -> {
			uiAction = Action.RESTART;
		});
		panel.setFocusable(true);
		panel.setRequestFocusEnabled( true );
		inputProvider.attach(panel);
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setResizable(false);
		frame.setVisible( true );
		frame.setLocationRelativeTo( null );
		panel.requestFocus();

		tickListeners.addTickListener( panel );
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
			if ( screenState.isInSyncWithBoardState() ) // only process input once screen state is in sync with board state
			{
				final IInputProvider.Action action;
				if ( uiAction != Action.NONE ) 
				{
					action = uiAction;
					uiAction = Action.NONE;
				} else {
					action = inputProvider.getAction( state );
				}
				
				if ( action != Action.NONE) 
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
			}

			// render
			panel.render( state );
			
			// sleep some time to not burn all available CPU time
			try
			{
				Thread.sleep( 12 );
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