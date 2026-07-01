package data.Model;

/** A signed-in account's pack/progress state, stored at {@code Users/<uid>}. */
public class UserProfile {

    public String displayName;
    public int freePacksAvailable;
    public long lastPackClaimAt;
    public boolean starterPackClaimed;

    public UserProfile() {
        // Required no-arg constructor for Firebase deserialization.
    }
}
