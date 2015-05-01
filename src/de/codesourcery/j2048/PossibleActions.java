package de.codesourcery.j2048;

public final class PossibleActions 
{
	public static final int MOVE_LEFT    =   1;
	public static final int MOVE_RIGHT   =   2;
	public static final int MOVE_DOWN    =   4;
	public static final int MOVE_UP      =   8;
	public static final int MERGE_LEFT   =  16;
	public static final int MERGE_RIGHT  =  32;
	public static final int MERGE_DOWN   =  64;
	public static final int MERGE_UP     = 128;
	
	public static boolean canMoveLeft(int value) { return (value & MOVE_LEFT) != 0; }
	
	public static boolean canMoveRight(int value) { return (value & MOVE_RIGHT) != 0; }
	
	public static boolean canMoveUp(int value) { return (value & MOVE_UP) != 0; }
	
	public static boolean canMoveDown(int value) { return (value & MOVE_DOWN) != 0; }
	
	public static boolean canMergeLeft(int value) { return (value & MERGE_LEFT) != 0; }
	
	public static boolean canMergeRight(int value) { return (value & MERGE_RIGHT) != 0; }
	
	public static boolean canMergeUp(int value) { return (value & MERGE_UP) != 0; }
	
	public static boolean canMergeDown(int value) { return (value & MERGE_DOWN) != 0; }
	
	@SuppressWarnings("incomplete-switch")
	public static boolean isValidAction(IInputProvider.Action action,int mask) 
	{
		switch( action ) 
		{
			case TILT_DOWN:
				return canMoveDown(mask) || canMergeDown(mask);
			case TILT_LEFT:
				return canMoveLeft(mask) || canMergeLeft(mask);
			case TILT_RIGHT:
				return canMoveRight(mask) || canMergeRight(mask);
			case TILT_UP:
				return canMoveUp(mask) || canMergeUp(mask);
		}
		return false;
	}
}