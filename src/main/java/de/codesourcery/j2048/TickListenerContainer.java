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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Thread-safe container for {@link ITickListener} instances that takes care of
 * propagating {@link ITickListener#tick(ITickContext)} calls to each of the registered listeners. 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class TickListenerContainer
{
	/**
	 * Listens to game ticks.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	@FunctionalInterface
	public interface ITickListener
	{
		/**
		 * Method that gets called periodically.
		 * 
		 * @param deltaSeconds time ins seconds that elapsed since the last tick
		 * @return <code>true</code> if this listener wants to receive further tick events, <code>false</code>
		 * if this listener can be discarded/does not need to receive any more events
		 */
		public boolean tick(float deltaSeconds);
	}

	private final List<ITickListener> tickListeners = new ArrayList<>();

	public void addTickListener(ITickListener l)
	{
		synchronized( tickListeners ) {
			this.tickListeners.add( l );
		}
	}

	public void removeTickListener(ITickListener l) {
		synchronized( tickListeners ) {
			this.tickListeners.remove( l );
		}
	}

	public void invokeTickListeners(float deltaSeconds)
	{
		synchronized( tickListeners )
		{
			for (Iterator<ITickListener> it = tickListeners.iterator(); it.hasNext();)
			{
				final ITickListener l = it.next();
				if ( ! l.tick( deltaSeconds ) ) {
					it.remove();
				}
			}
		}
	}
}