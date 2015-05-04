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

import de.codesourcery.j2048.ScreenState.Batch;

/**
 * Subclass of {@link BoardState} that forwards operations
 * to a {@link ScreenState} instance so proper animations
 * can be rendered.
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class BoardWithScreenState extends BoardState
{
	public final ScreenState screenState;
	protected Batch currentBatch;
	
	public BoardWithScreenState(ScreenState screenState)
	{
		super();
		this.screenState = screenState;
		reset();
	}
	
	@Override
	protected void resetScreenState() {
		screenState.reset();
	}	
	
	@Override
	protected void moveTile(int initialX,int initialY,int x,int y) {
		screenState.moveTile(initialX,initialY,x,y);
	}
	
	@Override
	protected void startBatch() {
		currentBatch = screenState.startBatch();
	}
	
	@Override
	protected void sync() {
		currentBatch.syncPoint();
	}	
	
	@Override
	protected void close() {
		currentBatch.close();
		currentBatch = null;
	}
	
	@Override
	protected void clearScreenState(int x,int y) {
		screenState.clear( x, y );
	}
	
	@Override
	protected void setScreenState(int x, int y, int value) {
		screenState.setTileValue( x , y , value );
	}	
}