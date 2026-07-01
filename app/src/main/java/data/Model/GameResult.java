package data.Model;

/**
 * One game a player played this week, mirrored from the federation site's "משחקים" tab into
 * {@code Games/<fedId>/<gameId>}.
 */
public class GameResult {

    public static final String RESULT_WIN = "win";
    public static final String RESULT_DRAW = "draw";
    public static final String RESULT_LOSS = "loss";

    public String opponent;
    public String result;
    public String date;
    public String tournament;
    public String weekKey;

    public GameResult() {
        // Required no-arg constructor for Firebase deserialization.
    }
}
