package ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.noamsl.fantasypt.R;

import java.util.List;

import data.Firebase.FirebaseManager;
import data.Model.PlayerCard;
import data.Model.UserProfile;

/** Reveals a 3-card pack (starter or weekly) and records the new ownership + pack-claim state. */
public class PackOpeningActivity extends AppCompatActivity {

    public static final String EXTRA_IS_STARTER = "is_starter";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pack_opening);

        boolean isStarter = getIntent().getBooleanExtra(EXTRA_IS_STARTER, false);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) { goToSquad(); return; }

        LinearLayout cardsRow = findViewById(R.id.cards_row);
        TextView title = findViewById(R.id.pack_title);
        Button continueButton = findViewById(R.id.continue_button);

        FirebaseManager.loadAllPlayers(pool -> {
            List<PlayerCard> pack = FirebaseManager.rollPack(pool);
            title.setText(getString(R.string.pack_opened_title));
            for (PlayerCard card : pack) {
                cardsRow.addView(buildCardView(card));
            }
            continueButton.setVisibility(View.VISIBLE);
            continueButton.setOnClickListener(v -> claimPack(uid, isStarter, pack));
        });
    }

    private View buildCardView(PlayerCard card) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_pack_card, null);
        TextView name = view.findViewById(R.id.card_name);
        TextView tier = view.findViewById(R.id.card_tier);
        TextView rating = view.findViewById(R.id.card_rating);
        ImageView avatar = view.findViewById(R.id.card_avatar);

        avatar.setImageResource(AvatarUtil.resFor(card.gender));
        name.setText(card.name);
        rating.setText(String.valueOf(card.cardRating));
        if (PlayerCard.TIER_LEGEND.equals(card.tier)) {
            tier.setText(R.string.tier_legend);
        } else if (PlayerCard.TIER_RARE.equals(card.tier)) {
            tier.setText(R.string.tier_rare);
        } else {
            tier.setText(R.string.tier_common);
        }
        return view;
    }

    private void claimPack(String uid, boolean isStarter, List<PlayerCard> pack) {
        for (PlayerCard card : pack) {
            FirebaseManager.cardsRef(uid).child(card.fedId).child("acquiredAt")
                    .setValue(System.currentTimeMillis());
        }

        FirebaseManager.userRef(uid).get().addOnSuccessListener(snap -> {
            UserProfile profile = snap.getValue(UserProfile.class);
            if (profile == null) profile = new UserProfile();
            profile.starterPackClaimed = true;
            if (!isStarter && profile.freePacksAvailable > 0) {
                profile.freePacksAvailable -= 1;
            }
            FirebaseManager.userRef(uid).setValue(profile).addOnCompleteListener(t -> goToSquad());
        }).addOnFailureListener(e -> goToSquad());
    }

    private void goToSquad() {
        startActivity(new Intent(this, SquadActivity.class));
        finish();
    }
}
