package de.codesourcery.j2048;

import de.codesourcery.j2048.ScreenState.Tile;
import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public final class TileMovingTickListener implements ITickListener
{
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

		final float speed = 20;
		if ( t.tileX != destTileX ) {
			this.deltaX = destTileX > t.tileX ? speed : -speed; // (destX - currentX) / 12.0f;
		} else {
			this.deltaX = 0;
		}
		if ( t.tileY != destTileY ) {
			this.deltaY = destTileY > t.tileY ? speed : -speed; // (destY - currentY ) / 12.0f;
		} else {
			this.deltaY = 0;
		}
		System.out.println("Moving tile from ("+this.currentX+","+this.currentY+") -> ("+this.destX+","+this.destY+") , dx="+deltaX+", dy="+deltaY);		
	}

	@Override
	public boolean tick(ITickContext ctx)
	{
		this.currentX += deltaX;
		this.currentY += deltaY;

		tile.x = (int) this.currentX;
		tile.y = (int) this.currentY;
		final boolean deltaXOk = deltaX == 0 || (deltaX > 0 && this.currentX >= destX ) || (deltaX < 0 && this.currentX <= destX );
		final boolean deltaYOk = deltaY == 0 || (deltaY > 0 && this.currentY >= destY ) || (deltaY < 0 && this.currentY <= destY );		
		if ( deltaXOk && deltaYOk )
		{
			tile.x = destX;
			tile.y = destY;
			return false;
		}
		return true;
	}
}