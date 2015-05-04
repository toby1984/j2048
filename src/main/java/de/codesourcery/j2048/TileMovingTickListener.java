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

import de.codesourcery.j2048.ScreenState.Tile;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

/**
 * An {@link ITickListener} responsible for animating/moving a tile by
 * continously updating its screen position. 
 *
 * @author tobias.gierke@code-sourcery.de
 */
public final class TileMovingTickListener implements ITickListener
{
	private static final float MOVEMENT_SPEED = 640;
	
	public static final boolean MOVE_INSTANTLY = Main.USE_AI;
	
	private final Tile tile;

	private final int destX;
	private final int destY;

	private final float deltaX;
	private final float deltaY;
	
	private float currentX;
	private float currentY;
	
	public TileMovingTickListener(Tile t,int destTileX,int destTileY)
	{
		this.tile = t;
		this.currentX = t.x;
		this.currentY = t.y;
		
		final int xBorderOffset = destTileX * GameScreen.BORDER_THICKNESS;
		final int yBorderOffset = destTileY * GameScreen.BORDER_THICKNESS;
		
		this.destX = GameScreen.BORDER_THICKNESS + destTileX * ScreenState.TILE_WIDTH  + xBorderOffset;
		this.destY = GameScreen.BORDER_THICKNESS + destTileY * ScreenState.TILE_HEIGHT + yBorderOffset;

		if ( t.tileX != destTileX ) {
			this.deltaX = destTileX > t.tileX ? MOVEMENT_SPEED : -MOVEMENT_SPEED; 
		} else {
			this.deltaX = 0;
		}
		if ( t.tileY != destTileY ) {
			this.deltaY = destTileY > t.tileY ? MOVEMENT_SPEED : -MOVEMENT_SPEED;
		} else {
			this.deltaY = 0;
		}
	}

	@Override
	public boolean tick(float deltaSeconds)
	{
		this.currentX += (deltaX*deltaSeconds);
		this.currentY += (deltaY*deltaSeconds);

		tile.x = (int) this.currentX;
		tile.y = (int) this.currentY;
		final boolean deltaXOk = deltaX == 0 || (deltaX > 0 && this.currentX >= destX ) || (deltaX < 0 && this.currentX <= destX );
		final boolean deltaYOk = deltaY == 0 || (deltaY > 0 && this.currentY >= destY ) || (deltaY < 0 && this.currentY <= destY );		
		if ( MOVE_INSTANTLY || deltaXOk && deltaYOk )
		{
			tile.x = destX;
			tile.y = destY;
			return false;
		}
		return true;
	}
}