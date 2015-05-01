package de.codesourcery.j2048;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AIPlayer implements IInputProvider {

	private static final int MAX_DEPTH = 6;

	private final ThreadPoolExecutor executor;

	protected static final class DynamicLatch {

		private int counter = 0;

		private final Object LOCK = new Object();

		public void countDown() 
		{
			synchronized( LOCK ) {
				counter--;
				LOCK.notifyAll();
			}
		}

		public void addThread() 
		{
			synchronized( LOCK ) {
				counter++;
			}
		}

		public void await() 
		{
			synchronized( LOCK ) 
			{
				while ( counter > 0 ) 
				{
					try {
						LOCK.wait();
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}
	public AIPlayer() 
	{
		final int threads = 1+Runtime.getRuntime().availableProcessors();
		final ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(300);

		final ThreadFactory threadFactory = new ThreadFactory() 
		{
			private final AtomicLong ID = new AtomicLong(0);
			@Override
			public Thread newThread(Runnable r) 
			{
				final Thread t = new Thread(r);
				t.setName("minimax-"+ID.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		};
		executor = new ThreadPoolExecutor(threads,threads,60,TimeUnit.SECONDS,
				workQueue, threadFactory , new ThreadPoolExecutor.CallerRunsPolicy() );
	}

	@Override
	public Action getAction(BoardState state) 
	{
		long time = -System.currentTimeMillis();
		Action result = doGetAction(state);
		time += System.currentTimeMillis();
//		System.out.println("time: "+time+" ms");
		return result;
	}

	private Action doGetAction(BoardState state) 
	{
		BoardState copy = state.createCopy();

		final DynamicLatch latch = new DynamicLatch();
		final List<CalcTask> tasks = new ArrayList<>();
		if ( copy.tiltDown() ) 
		{
			tasks.add( new CalcTask( Action.TILT_DOWN , copy , latch ) );
			copy = state.createCopy();
		}
		if ( copy.tiltUp() ) {
			tasks.add( new CalcTask( Action.TILT_UP , copy , latch ) );
			copy = state.createCopy();
		}	
		if ( copy.tiltLeft() ) {
			tasks.add( new CalcTask( Action.TILT_LEFT , copy , latch ) );
			copy = state.createCopy();
		}	
		if ( copy.tiltRight() ) {
			tasks.add( new CalcTask( Action.TILT_RIGHT, copy , latch ) );
		}			

		tasks.forEach( executor::submit );

		latch.await();

		IInputProvider.Action bestAction = Action.NONE;
		float bestScore = 0;
		for ( CalcTask task : tasks ) 
		{
			if ( bestAction == Action.NONE || task.score > bestScore ) 
			{
				bestAction = task.action;
				bestScore= task.score;
			}
		}
		return bestAction;
	}

	protected final class CalcTask implements Runnable {

		private final DynamicLatch latch;
		private final BoardState state;
		public final Action action;
		public float score;

		public CalcTask(Action action,BoardState state,DynamicLatch latch) {
			this.action = action;
			this.latch = latch;
			this.state = state;
			latch.addThread();
		}

		@Override
		public void run() {
			try {
				score = miniMax( state , MAX_DEPTH , true );
			} finally {
				latch.countDown();
			}
		}
	}

	private int miniMax(BoardState state,int currentDepth,boolean maximizingPlayer) 
	{	
		if ( currentDepth == 0 || state.isGameOver() ) {
			return calcScore( state );
		}

		int bestValue; 
		if ( maximizingPlayer ) 
		{
			bestValue = Integer.MIN_VALUE;

			final List<BoardState> moves = generateRandomMoves( state );
			for (int i = 0 , len = moves.size() ; i < len; i++) {
				final BoardState m = moves.get(i);
				final int val = miniMax( m , currentDepth - 1 , false );
				bestValue = Math.max(bestValue,val);
			}
		} 
		else 
		{
			bestValue = Integer.MAX_VALUE;
			final List<BoardState> moves = generatePlayerMoves( state );
			for (int i = 0 , len = moves.size() ; i < len ; i++) {
				final BoardState m = moves.get(i);
				final int val = miniMax( m , currentDepth - 1 , true );
				bestValue = Math.min(bestValue,val);
			}
		}
		return bestValue;
	}

	/*
function minimax(node, depth, maximizingPlayer)
    if depth = 0 or node is a terminal node
        return the heuristic value of node
    if maximizingPlayer
        bestValue := -∞
        for each child of node
            val := minimax(child, depth - 1, FALSE)
            bestValue := max(bestValue, val)
        return bestValue
    else
        bestValue := +∞
        for each child of node
            val := minimax(child, depth - 1, TRUE)
            bestValue := min(bestValue, val)
        return bestValue	 
	 */	

	private List<BoardState> generatePlayerMoves(BoardState state)
	{
		final List<BoardState> moves = new ArrayList<>();
		BoardState copy = state.createCopy();
		if ( copy.tiltDown() ) {
			moves.add( copy );
			copy = state.createCopy();
		}
		if ( copy.tiltUp() ) {
			moves.add( copy );
			copy = state.createCopy();
		}	
		if ( copy.tiltLeft() ) 
		{
			moves.add( copy );
			copy = state.createCopy();
		}	
		if ( copy.tiltRight() ) {
			moves.add( copy );
		}		
		return moves;
	}

	private int calcScore(BoardState state) 
	{
		// number of free slots
		final int totalSlots = BoardState.GRID_COLS*BoardState.GRID_ROWS;
		final int tileCount = state.getTileCount();
		final int freeSlots = totalSlots - tileCount;
		if ( freeSlots == 0 && state.isGameOver() ) {
			return 0;
		}
		return freeSlots*state.getScore();
	}

	private List<BoardState> generateRandomMoves(BoardState state)
	{
		final List<BoardState> moves = new ArrayList<>();
		for ( int y = 0 ; y < BoardState.GRID_ROWS ;y++) 
		{
			for ( int x = 0 ; x < BoardState.GRID_COLS ;x++) 
			{
				if ( state.isEmpty(x, y ) ) {
					BoardState copy1 = state.createCopy();
					copy1.setTileValue(x, y, 1); // set 2
					moves.add( copy1 );
					copy1 = state.createCopy();
					copy1.setTileValue(x, y, 2); // set 4
					moves.add( copy1 );
				}
			}
		}
		return moves;
	}

	@Override
	public void attach(Component peer) { /* NOP */ }
}
