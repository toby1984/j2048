package de.codesourcery.j2048;

import de.codesourcery.j2048.ScreenState.Tile;
import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class TileMovingTickListener implements ITickListener
{
	private final Tile tile;

	private final int destX;
	private final int destY;

	private float currentX;
	private float currentY;

	private final float deltaX;
	private final float deltaY;

	public TileMovingTickListener(Tile t,int destX,int destY)
	{
		this.tile = t;
		this.currentX = t.x;
		this.currentY = t.y;
		this.destX = destX;
		this.destY = destY;
		this.deltaX = (destX - currentX) / 60;
		this.deltaY = (destY - currentY) / 60;
	}

	@Override
	public boolean tick(ITickContext ctx)
	{
		this.currentX += deltaX;
		this.currentY += deltaY;

		tile.x = (int) currentX;
		tile.y = (int) currentY;
		if ( tile.x == destX && tile.y == destY ) {
			return false;
		}
		return true;
	}
}