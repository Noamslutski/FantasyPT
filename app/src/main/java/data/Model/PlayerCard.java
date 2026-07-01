package data.Model;

/**
 * A Hapoel Petah Tikva chess player, mirrored from the federation site by the scraper into
 * {@code Players/<fedId>}. {@code cardRating} (1-100) and {@code tier} are derived from the real
 * Israeli rating at scrape time so the app never has to recompute them.
 */
public class PlayerCard {

    public static final String TIER_COMMON = "common";
    public static final String TIER_RARE = "rare";
    public static final String TIER_LEGEND = "legend";

    public String fedId;
    public String name;
    public String club;
    public int ratingIsrael;
    public int cardRating;
    public String tier;
    public String gender; // "boy" or "girl" — picks which of the two generic avatars to show.

    public PlayerCard() {
        // Required no-arg constructor for Firebase deserialization.
    }
}
