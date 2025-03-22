package api.indy.model.ai;

public class AIRequest {
    private final int[][] board;
    private final int value;

    public AIRequest(int[][] board, int value) {
        this.board = board;
        this.value = value;
    }

    public int[][] board() {
        return this.board;
    }

    public int value() {
        return this.value;
    }
}
