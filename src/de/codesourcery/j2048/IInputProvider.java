package de.codesourcery.j2048;

import java.awt.Component;

public interface IInputProvider 
{
	public static enum Action 
	{
		NONE,TILT_DOWN,TILT_UP,TILT_LEFT,TILT_RIGHT,RESTART;
	}
	
	public Action getAction(BoardState state);
	
	public void attach(Component peer);
}
