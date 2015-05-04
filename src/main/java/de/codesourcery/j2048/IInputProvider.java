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

import java.awt.Component;

/**
 * Abstraction for receiving either input from either the user or the AI,
 *
 * @author tobias.gierke@code-sourcery.de
 */
public interface IInputProvider 
{
	/**
	 * Possible actions.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	public static enum Action 
	{
		NONE,TILT_DOWN,TILT_UP,TILT_LEFT,TILT_RIGHT,RESTART;
	}
	
	/**
	 * Returns the current action for a given board state.
	 * 
	 * @param state
	 * @return
	 */
	public Action getAction(BoardState state);
	
	/**
	 * Attaches this input provider to its UI peer. 
	 * @param peer
	 */
	public void attach(Component peer);
}