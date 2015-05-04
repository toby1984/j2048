package de.codesourcery.j2048;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class AIPlayer implements IInputProvider {

	private static final boolean BENCHMARK = true;
	
	private static final int MAX_DEPTH = 8;
	
	private final Map<Integer,Integer> scores = new HashMap<>();
	
	protected static enum Player { AI , RND };

	private final ThreadPoolExecutor executor;
	
	protected volatile long positions = 0;
	
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
				score = alphaBeta( state , MAX_DEPTH , Integer.MIN_VALUE , Integer.MAX_VALUE , Player.RND );
//				score = miniMax( state , MAX_DEPTH , Player.RND );
			} finally {
				latch.countDown();
			}
		}
	}	

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
		if ( state.isGameOver() ) 
		{
			final int highestTileValue = state.getHighestTileValue();
			Integer existing = scores.get( highestTileValue );
			if ( existing == null ) {
				existing = 1;
			} else {
				existing = existing+1;
			}
			scores.put( highestTileValue , existing );
			final int gameCount = scores.values().stream().mapToInt( Integer::intValue ).sum();
			System.out.println("=========== Game "+gameCount+" =========");
			scores.forEach( (value,count) -> 
			{
				float percentage = 100.0f*(count/(float) gameCount);
				System.out.println( value+": "+percentage+" % ("+count+" of out "+gameCount+")");
			});
			return Action.RESTART;
		}
		positions = 0;
		long time;
		if ( BENCHMARK ) {
			time = -System.currentTimeMillis();
		}
		Action result = doGetAction(state);
		if ( BENCHMARK ) {
			time += System.currentTimeMillis();
			float seconds = time/1000f;
			System.out.println("time: "+time+" ms , "+positions+" positions (="+(positions/seconds)+" positions/s))");
		}
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

		System.out.println("Forking "+tasks.size()+" tasks");
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

	/*
function alphabeta(node, depth, α, β, maximizingPlayer)
      if depth = 0 or node is a terminal node
          return the heuristic value of node
      if maximizingPlayer
          v := -∞
          for each child of node
              v := max(v, alphabeta(child, depth - 1, α, β, FALSE))
              α := max(α, v)
              if β ≤ α
                  break (* β cut-off *)
          return v
      else
          v := ∞
          for each child of node
              v := min(v, alphabeta(child, depth - 1, α, β, TRUE))
              β := min(β, v)
              if β ≤ α
                  break (* α cut-off *)
          return v	 
	 */
	
	private int alphaBeta(BoardState state,int currentDepth,int alpha,int beta,Player player) 
	{	
		if ( state.isGameOver() || ( currentDepth <= 0 && positions > 2000000 ) ) {
			return calcScore( state );
		}
		
		int bestValue; 
		if ( player == Player.AI ) 
		{
			bestValue = Integer.MIN_VALUE;

			final List<BoardState> moves = generatePlayerMoves( state );			
			for (int i = 0 , len = moves.size() ; i < len; i++) {
				final BoardState m = moves.get(i);
				bestValue = Math.max( bestValue , alphaBeta( m , currentDepth - 1 , alpha , beta,  Player.RND ) );
				alpha = Math.max( alpha ,bestValue );
				if ( beta <= alpha ) {
					break;
				}
			}
		} 
		else 
		{
			bestValue = Integer.MAX_VALUE;
			final List<BoardState> moves = generateRandomMoves( state );
			for (int i = 0 , len = moves.size() ; i < len ; i++) {
				final BoardState m = moves.get(i);
				bestValue = Math.min(bestValue ,  alphaBeta( m , currentDepth - 1 , alpha , beta , Player.AI ) );
				beta = Math.min(beta , bestValue );
				if ( beta <= alpha ) {
					break;
				}
			}
		}
		return bestValue;		
		
	}

	private int miniMax(BoardState state,int currentDepth,Player maximizingPlayer) 
	{	
		if ( currentDepth == 0 || state.isGameOver() ) {
			return calcScore( state );
		}

		int bestValue; 
		if ( maximizingPlayer == Player.RND) 
		{
			bestValue = Integer.MIN_VALUE;

			final List<BoardState> moves = generateRandomMoves( state );
			for (int i = 0 , len = moves.size() ; i < len; i++) {
				final BoardState m = moves.get(i);
				final int val = miniMax( m , currentDepth - 1 , Player.AI);
				bestValue = Math.max(bestValue,val);
			}
		} 
		else 
		{
			bestValue = Integer.MAX_VALUE;
			final List<BoardState> moves = generatePlayerMoves( state );
			for (int i = 0 , len = moves.size() ; i < len ; i++) {
				final BoardState m = moves.get(i);
				final int val = miniMax( m , currentDepth - 1 , Player.RND );
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
		positions++;
		
		final int freeSlotCount = BoardState.GRID_COLS*BoardState.GRID_ROWS - state.getTileCount();
		if ( state.isGameOver() ) {
			return 0;
		}
		// calculate sum around edges
		int score = 0;
		for ( int y = 0 ; y < BoardState.GRID_ROWS ;y++) 
		{
			for ( int x = 0 ; x < BoardState.GRID_COLS ;x++) 
			{
				int tile = state.getTile(x, y);
				if ( x == 0 || y == 0 || x == BoardState.GRID_COLS-1 || y == BoardState.GRID_ROWS-1 ) {
					score += 8*(1<< (tile & ~0xffffffff));
				} else {
					score += (1<< (tile & ~0xffffffff));
				}
			}
		}

		return score * freeSlotCount;
	}

	private List<BoardState> generateRandomMoves(BoardState state)
	{
		final List<BoardState> moves = new ArrayList<>();
		for ( int y = 0 ; y < BoardState.GRID_ROWS ;y++) 
		{
			for ( int x = 0 ; x < BoardState.GRID_COLS ;x++) 
			{
				if ( state.isEmpty(x, y ) ) 
				{
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
	
	public static void main(String[] args) {
		
		final Random rnd = new Random(0xdeadbeef);
		final BoardState state = new BoardState();
		state.reset();
		
		long actions = 0;
		final long time = System.currentTimeMillis();
		AIPlayer player = new AIPlayer();
outer:		
		while ( true ) 
		{
			state.placeRandomTile( rnd );
			Action action = player.getAction( state );
			boolean valid = false;
			switch(action) 
			{
				case NONE:
					continue outer;
				case RESTART:
					final float elapsed = (System.currentTimeMillis() - time)/1000f;
					System.out.println("Actions/s = "+actions/elapsed);
					state.reset();
					state.placeRandomTile(rnd);					
					continue outer;
				case TILT_DOWN:
					valid = state.tiltDown();
					break;
				case TILT_LEFT:
					valid = state.tiltLeft();
					break;
				case TILT_RIGHT:
					valid = state.tiltRight();
					break;
				case TILT_UP:
					valid = state.tiltUp();
					break;
			}
			if ( ! valid ) {
				System.out.println("*** ERROR ***");
				System.exit(0);
			} else {
				actions++;
				state.placeRandomTile( rnd );
			}
		}
	}
}
