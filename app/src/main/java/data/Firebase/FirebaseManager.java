package data.Firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.Model.GameResult;
import data.Model.PlayerCard;

/**
 * Realtime Database access for fantasyPT. Uses the app's default RTDB instance (wired up by
 * google-services.json once it's added to the project), so there's no hardcoded DB URL here —
 * unlike RacingManager, which predates the region-scoped default instance and still points at
 * an explicit URL.
 *
 * Schema (see plan): Players/{fedId}, Games/{fedId}/{gameId}, Users/{uid}, Cards/{uid}/{fedId},
 * Squads/{uid}/slot0..slot4, Avatars/boy|girl.
 */
public class FirebaseManager {

    public static final int SQUAD_SIZE = 5;
    public static final int PACK_SIZE = 3;
    public static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private static DatabaseReference root() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public interface SignInCallback { void onSignedIn(String uid); }

    /** Signs the device in anonymously if it isn't already; anonymous accounts persist locally. */
    public static void ensureSignedIn(SignInCallback cb) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getUid();
        if (uid != null) { cb.onSignedIn(uid); return; }
        auth.signInAnonymously().addOnSuccessListener(result -> {
            String newUid = result.getUser() != null ? result.getUser().getUid() : null;
            cb.onSignedIn(newUid);
        });
    }

    public interface PlayersCallback { void onPlayers(List<PlayerCard> players); }

    /** Loads the full Hapoel Petah Tikva roster (kept by the scraper under {@code Players/}). */
    public static void loadAllPlayers(PlayersCallback cb) {
        root().child("Players").get().addOnSuccessListener(snap -> {
            List<PlayerCard> players = new ArrayList<>();
            for (DataSnapshot child : snap.getChildren()) {
                PlayerCard p = child.getValue(PlayerCard.class);
                if (p != null) {
                    if (p.fedId == null) p.fedId = child.getKey();
                    players.add(p);
                }
            }
            cb.onPlayers(players);
        }).addOnFailureListener(e -> cb.onPlayers(new ArrayList<>()));
    }

    public interface GamesCallback { void onGames(List<GameResult> games); }

    /** Loads this week's games for a single player (populated weekly by the scraper). */
    public static void loadGamesForPlayer(String fedId, GamesCallback cb) {
        root().child("Games").child(fedId).get().addOnSuccessListener(snap -> {
            List<GameResult> games = new ArrayList<>();
            for (DataSnapshot child : snap.getChildren()) {
                GameResult g = child.getValue(GameResult.class);
                if (g != null) games.add(g);
            }
            cb.onGames(games);
        }).addOnFailureListener(e -> cb.onGames(new ArrayList<>()));
    }

    public static DatabaseReference userRef(String uid) {
        return root().child("Users").child(uid);
    }

    public static DatabaseReference cardsRef(String uid) {
        return root().child("Cards").child(uid);
    }

    public static DatabaseReference squadRef(String uid) {
        return root().child("Squads").child(uid);
    }

    /**
     * Picks {@link #PACK_SIZE} players from the roster, weighted by rarity tier (common 65%,
     * rare 28%, legend 7%), avoiding duplicates within the same pack when the roster is large
     * enough to do so.
     */
    public static List<PlayerCard> rollPack(List<PlayerCard> pool) {
        List<PlayerCard> result = new ArrayList<>();
        if (pool.isEmpty()) return result;
        Random random = new Random();
        List<PlayerCard> common = new ArrayList<>();
        List<PlayerCard> rare = new ArrayList<>();
        List<PlayerCard> legend = new ArrayList<>();
        for (PlayerCard p : pool) {
            if (PlayerCard.TIER_LEGEND.equals(p.tier)) legend.add(p);
            else if (PlayerCard.TIER_RARE.equals(p.tier)) rare.add(p);
            else common.add(p);
        }
        for (int i = 0; i < PACK_SIZE; i++) {
            List<PlayerCard> tierPool = pickTierPool(random, common, rare, legend);
            if (tierPool.isEmpty()) tierPool = pool;
            PlayerCard pick = tierPool.get(random.nextInt(tierPool.size()));
            result.add(pick);
        }
        return result;
    }

    private static List<PlayerCard> pickTierPool(Random random, List<PlayerCard> common,
            List<PlayerCard> rare, List<PlayerCard> legend) {
        double roll = random.nextDouble();
        if (roll < 0.07 && !legend.isEmpty()) return legend;
        if (roll < 0.35 && !rare.isEmpty()) return rare;
        if (!common.isEmpty()) return common;
        if (!rare.isEmpty()) return rare;
        return legend;
    }

    /** True once at least a week has passed since the last free-pack claim (or none was ever claimed). */
    public static boolean weeklyPackDue(long lastPackClaimAt) {
        return System.currentTimeMillis() - lastPackClaimAt >= WEEK_MILLIS;
    }
}
