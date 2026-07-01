package ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.noamsl.fantasypt.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import data.Firebase.FirebaseManager;
import data.Model.GameResult;
import data.Model.PlayerCard;
import data.Model.UserProfile;

/** Main screen: 5-slot squad grid, free-pack banner, and a weekly-games panel for whichever
 * card was last tapped. Mirrors the mockup shown while planning this feature. */
public class SquadActivity extends AppCompatActivity {

    private String uid;
    private final Map<String, PlayerCard> playersById = new HashMap<>();
    private final String[] squadFedIds = new String[FirebaseManager.SQUAD_SIZE];
    private final Set<String> ownedFedIds = new HashSet<>();
    @Nullable private String selectedFedId;

    private LinearLayout squadRow;
    private LinearLayout packBanner;
    private TextView packBannerText;
    private TextView totalPointsView;
    private TextView detailName;
    private TextView detailRating;
    private LinearLayout gamesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squad);

        squadRow = findViewById(R.id.squad_row);
        packBanner = findViewById(R.id.pack_banner);
        packBannerText = findViewById(R.id.pack_banner_text);
        totalPointsView = findViewById(R.id.total_points);
        detailName = findViewById(R.id.detail_name);
        detailRating = findViewById(R.id.detail_rating);
        gamesList = findViewById(R.id.games_list);

        uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        FirebaseManager.loadAllPlayers(players -> {
            for (PlayerCard p : players) playersById.put(p.fedId, p);
            loadUserState();
        });
    }

    private void loadUserState() {
        FirebaseManager.userRef(uid).get().addOnSuccessListener(snap -> {
            UserProfile profile = snap.getValue(UserProfile.class);
            if (profile == null) profile = new UserProfile();
            checkWeeklyPack(profile);
        });
    }

    private void checkWeeklyPack(UserProfile profile) {
        if (FirebaseManager.weeklyPackDue(profile.lastPackClaimAt)) {
            profile.freePacksAvailable += 1;
            profile.lastPackClaimAt = System.currentTimeMillis();
            FirebaseManager.userRef(uid).setValue(profile);
        }
        renderBanner(profile);
        loadCards();
    }

    private void renderBanner(UserProfile profile) {
        if (profile.freePacksAvailable > 0) {
            packBanner.setVisibility(View.VISIBLE);
            packBannerText.setText(getString(R.string.free_packs_waiting, profile.freePacksAvailable));
            findViewById(R.id.open_pack_button).setOnClickListener(v -> {
                Intent intent = new Intent(this, PackOpeningActivity.class);
                intent.putExtra(PackOpeningActivity.EXTRA_IS_STARTER, false);
                startActivity(intent);
                finish();
            });
        } else {
            packBanner.setVisibility(View.GONE);
        }
    }

    private void loadCards() {
        FirebaseManager.cardsRef(uid).get().addOnSuccessListener(snap -> {
            ownedFedIds.clear();
            for (com.google.firebase.database.DataSnapshot child : snap.getChildren()) {
                ownedFedIds.add(child.getKey());
            }
            loadSquad();
        });
    }

    private void loadSquad() {
        FirebaseManager.squadRef(uid).get().addOnSuccessListener(snap -> {
            for (int i = 0; i < squadFedIds.length; i++) {
                com.google.firebase.database.DataSnapshot child = snap.child("slot" + i);
                squadFedIds[i] = child.getValue(String.class);
            }
            renderSquad();
            computeTotalPoints();
        });
    }

    private void renderSquad() {
        squadRow.removeAllViews();
        for (int i = 0; i < squadFedIds.length; i++) {
            final int slotIndex = i;
            View slotView = LayoutInflater.from(this).inflate(R.layout.item_squad_slot, squadRow, false);
            TextView emptyLabel = slotView.findViewById(R.id.empty_label);
            LinearLayout filledGroup = slotView.findViewById(R.id.filled_group);

            String fedId = squadFedIds[i];
            PlayerCard card = fedId != null ? playersById.get(fedId) : null;

            if (card != null) {
                emptyLabel.setVisibility(View.GONE);
                filledGroup.setVisibility(View.VISIBLE);
                ((TextView) slotView.findViewById(R.id.slot_name)).setText(card.name);
                ((TextView) slotView.findViewById(R.id.slot_rating)).setText(String.valueOf(card.cardRating));
                ((TextView) slotView.findViewById(R.id.slot_tier)).setText(tierLabel(card.tier));
                ((android.widget.ImageView) slotView.findViewById(R.id.slot_avatar))
                        .setImageResource(AvatarUtil.resFor(card.gender));
                slotView.setOnClickListener(v -> selectCard(card.fedId));
            } else {
                emptyLabel.setVisibility(View.VISIBLE);
                filledGroup.setVisibility(View.GONE);
                slotView.setOnClickListener(v -> showPickerDialog(slotIndex));
            }
            slotView.setOnLongClickListener(v -> { showPickerDialog(slotIndex); return true; });

            squadRow.addView(slotView);
        }

        if (selectedFedId == null) {
            for (String fedId : squadFedIds) {
                if (fedId != null) { selectedFedId = fedId; break; }
            }
        }
        if (selectedFedId != null) selectCard(selectedFedId);
    }

    private String tierLabel(String tier) {
        if (PlayerCard.TIER_LEGEND.equals(tier)) return getString(R.string.tier_legend);
        if (PlayerCard.TIER_RARE.equals(tier)) return getString(R.string.tier_rare);
        return getString(R.string.tier_common);
    }

    private void selectCard(String fedId) {
        selectedFedId = fedId;
        PlayerCard card = playersById.get(fedId);
        if (card == null) return;
        detailName.setText(card.name + " · " + getString(R.string.this_weeks_games));
        detailRating.setText(getString(R.string.rating_format, card.cardRating));
        FirebaseManager.loadGamesForPlayer(fedId, this::renderGames);
    }

    private void renderGames(List<GameResult> games) {
        gamesList.removeAllViews();
        if (games.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_games_this_week);
            empty.setTextColor(getColor(R.color.fpt_text_muted));
            empty.setTextSize(13f);
            gamesList.addView(empty);
            return;
        }
        for (GameResult game : games) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_game_row, gamesList, false);
            ((TextView) row.findViewById(R.id.game_opponent)).setText("vs " + game.opponent);
            TextView badge = row.findViewById(R.id.game_result_badge);
            TextView points = row.findViewById(R.id.game_points);
            int pts = pointsForResult(game.result);
            points.setText((pts > 0 ? "+" : "") + pts + "pt");
            if (GameResult.RESULT_WIN.equals(game.result)) {
                badge.setText(R.string.result_win);
                badge.setBackgroundResource(R.drawable.badge_success_bg);
                badge.setTextColor(getColor(R.color.fpt_success));
            } else if (GameResult.RESULT_DRAW.equals(game.result)) {
                badge.setText(R.string.result_draw);
                badge.setBackgroundResource(R.drawable.badge_warning_bg);
                badge.setTextColor(getColor(R.color.fpt_warning));
            } else {
                badge.setText(R.string.result_loss);
                badge.setBackgroundResource(R.drawable.badge_danger_bg);
                badge.setTextColor(getColor(R.color.fpt_danger));
            }
            gamesList.addView(row);
        }
    }

    private int pointsForResult(String result) {
        if (GameResult.RESULT_WIN.equals(result)) return 10;
        if (GameResult.RESULT_DRAW.equals(result)) return 4;
        return -2;
    }

    /** Sums this week's scoring across the whole squad (win/draw/loss only — the monthly
     * rating-gain bonus needs rating history the scraper doesn't record yet). */
    private void computeTotalPoints() {
        List<String> fedIds = new ArrayList<>();
        for (String fedId : squadFedIds) if (fedId != null) fedIds.add(fedId);
        if (fedIds.isEmpty()) { totalPointsView.setText("0"); return; }

        int[] total = {0};
        int[] remaining = {fedIds.size()};
        for (String fedId : fedIds) {
            FirebaseManager.loadGamesForPlayer(fedId, games -> {
                for (GameResult g : games) total[0] += pointsForResult(g.result);
                remaining[0] -= 1;
                if (remaining[0] == 0) {
                    totalPointsView.setText(String.format(Locale.getDefault(), "%d", total[0]));
                }
            });
        }
    }

    private void showPickerDialog(int slotIndex) {
        List<String> options = new ArrayList<>();
        List<String> optionFedIds = new ArrayList<>();

        if (squadFedIds[slotIndex] != null) {
            options.add("— remove —");
            optionFedIds.add(null);
        }

        for (String fedId : ownedFedIds) {
            boolean alreadyInSquad = false;
            for (String s : squadFedIds) if (fedId.equals(s)) { alreadyInSquad = true; break; }
            if (alreadyInSquad) continue;
            PlayerCard card = playersById.get(fedId);
            if (card == null) continue;
            options.add(card.name + " (" + card.cardRating + ")");
            optionFedIds.add(fedId);
        }

        if (options.isEmpty()) {
            Toast.makeText(this, R.string.no_owned_cards, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_a_card)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String chosen = optionFedIds.get(which);
                    squadFedIds[slotIndex] = chosen;
                    FirebaseManager.squadRef(uid).child("slot" + slotIndex).setValue(chosen);
                    renderSquad();
                    computeTotalPoints();
                })
                .show();
    }
}
