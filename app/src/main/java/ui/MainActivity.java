package ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.noamsl.fantasypt.R;

import data.Firebase.FirebaseManager;
import data.Model.UserProfile;

/** Launcher activity: signs the device in anonymously, then routes to the starter pack (first
 * launch) or straight to the squad screen. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseManager.ensureSignedIn(uid -> {
            if (uid == null) { routeToSquad(); return; }
            FirebaseManager.userRef(uid).get().addOnSuccessListener(snap -> {
                UserProfile profile = snap.getValue(UserProfile.class);
                if (profile == null || !profile.starterPackClaimed) {
                    Intent intent = new Intent(MainActivity.this, PackOpeningActivity.class);
                    intent.putExtra(PackOpeningActivity.EXTRA_IS_STARTER, true);
                    startActivity(intent);
                } else {
                    routeToSquad();
                }
                finish();
            }).addOnFailureListener(e -> { routeToSquad(); finish(); });
        });
    }

    private void routeToSquad() {
        startActivity(new Intent(this, SquadActivity.class));
    }
}
