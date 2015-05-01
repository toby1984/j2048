package de.codesourcery.j2048;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public final class KeyboardInputProvider extends KeyAdapter implements IInputProvider  {

	public final Set<Integer> pressed = new HashSet<>();
	
	@Override
	public Action getAction(BoardState state) 
	{
		try 
		{
			if ( isPressed( KeyEvent.VK_ENTER ) ) {
				return Action.RESTART;
			}
			if ( isPressed( KeyEvent.VK_A ) || isPressed( KeyEvent.VK_LEFT ) )
			{
				return Action.TILT_LEFT;
			}
			if ( isPressed( KeyEvent.VK_D ) || isPressed( KeyEvent.VK_RIGHT ) )
			{
				return Action.TILT_RIGHT;
			}
			if ( isPressed( KeyEvent.VK_W ) || isPressed( KeyEvent.VK_UP ) )
			{
				return Action.TILT_DOWN;
			}
			if ( isPressed( KeyEvent.VK_S ) || isPressed( KeyEvent.VK_DOWN ) )
			{
				return Action.TILT_UP;
			}
			return Action.NONE;
		} finally {
			clearInput();
		}
	}

	@Override
	public void attach(Component peer) {
		peer.addKeyListener( this );
	}
	
	@Override 
	public void keyReleased(KeyEvent e) { 
		pressed.remove( e.getKeyCode() ); 
	}
	
	@Override 
	public void keyPressed(KeyEvent e) { 
		pressed.add( e.getKeyCode() ); 
	}
	
	private boolean isPressed(int keyCode) { return pressed.contains(keyCode); }
	
	private void clearInput() { pressed.clear(); }	
}