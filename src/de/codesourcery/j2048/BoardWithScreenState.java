package de.codesourcery.j2048;

import de.codesourcery.j2048.ScreenState.Batch;

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
