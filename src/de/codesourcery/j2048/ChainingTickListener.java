package de.codesourcery.j2048;

import de.codesourcery.j2048.TickListenerContainer.ITickContext;
import de.codesourcery.j2048.TickListenerContainer.ITickListener;

public class ChainingTickListener implements ITickListener{

	private final ITickListener l1;
	private final ITickListener l2;

	private ITickListener current;

	public ChainingTickListener(ITickListener l1, ITickListener l2)
	{
		this.l1 = l1;
		this.l2 = l2;
		this.current = l1;
	}

	@Override
	public boolean tick(ITickContext ctx)
	{
		boolean result = current.tick(ctx);
		if ( ! result ) {
			if ( current == l1 ) {
				current = l2;
			} else {
				result = false;
			}
		}
		return result;
	}
}