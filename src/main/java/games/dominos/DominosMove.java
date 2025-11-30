package games.dominos;

import iialib.games.model.IMove;

public class DominosMove implements IMove {
	
	public final int x;
    public final int y;
    
    DominosMove(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Move{" + x + "," + y + "}";
    }
}
