package de.codesourcery.j2048;

import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class DelayedTickListener implements ITickListener {

	private final ITickListener delegate;
	private final float delaySeconds;
	private float elapsedTime;

	public DelayedTickListener(ITickListener delegate,float delaySeconds)
	{
		this.delaySeconds = delaySeconds;
		this.delegate = delegate;
	}

	@Override
	public boolean tick(ITickContext ctx)
	{
		elapsedTime += ctx.getDeltaSeconds();
		if ( elapsedTime >= delaySeconds )
		{
			elapsedTime -= delaySeconds;
			return delegate.tick( ctx );
		}
		return true;
	}
}