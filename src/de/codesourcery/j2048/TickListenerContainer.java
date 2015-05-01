package de.codesourcery.j2048;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TickListenerContainer
{
	public interface ITickContext {

		public void add(ITickListener l);
		public float getDeltaSeconds();
	}

	@FunctionalInterface
	public interface ITickListener
	{
		public boolean tick(ITickContext ctx);
	}

	private final List<ITickListener> tickListeners = new ArrayList<>();

	protected static final class MyTickCtx implements ITickContext
	{
		public List<ITickListener> toAdd;
		public final float deltaSeconds;

		public MyTickCtx(float deltaSeconds) {
			this.deltaSeconds = deltaSeconds;
		}

		@Override
		public float getDeltaSeconds() {
			return deltaSeconds;
		}

		@Override
		public void add(ITickListener l) {
			if ( toAdd == null ) {
				toAdd = new ArrayList<>();
			}
			toAdd.add( l );
		}
	}

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
		final MyTickCtx ctx = new MyTickCtx(deltaSeconds);
		synchronized( tickListeners )
		{
			for (Iterator<ITickListener> it = tickListeners.iterator(); it.hasNext();)
			{
				final ITickListener l = it.next();
				if ( ! l.tick( ctx ) ) {
					it.remove();
				}
			}
			if ( ctx.toAdd != null ) {
				tickListeners.addAll( ctx.toAdd );
			}
		}
	}
}