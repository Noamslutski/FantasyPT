"""Pushes scraped roster + weekly games into the fantasychess-ad0c0 Realtime Database.

Requires scraper/serviceAccountKey.json (downloaded once from Firebase console -> Project
Settings -> Service Accounts -> Generate new private key). That file is git-ignored — never
commit it.
"""

import time

import firebase_admin
from firebase_admin import credentials, db

from rating_scale import card_rating, tier_for
from scrape_player import fetch_player_games_this_week, fetch_player_profile
from scrape_roster import fetch_roster

DATABASE_URL = "https://fantasychess-ad0c0-default-rtdb.firebaseio.com"


def init_app():
    cred = credentials.Certificate("serviceAccountKey.json")
    firebase_admin.initialize_app(cred, {"databaseURL": DATABASE_URL})


def sync_roster():
    roster = fetch_roster()
    players_ref = db.reference("Players")
    games_ref = db.reference("Games")

    for entry in roster:
        fed_id = entry["fedId"]
        profile = fetch_player_profile(fed_id)
        rating = profile.get("rating") or 0
        card = {
            "fedId": fed_id,
            "name": entry["name"],
            "club": profile.get("club") or "הפועל פתח תקווה",
            "ratingIsrael": rating,
            "cardRating": card_rating(rating),
            "tier": tier_for(card_rating(rating)),
            "lastSynced": int(time.time() * 1000),
        }
        players_ref.child(fed_id).update(card)

        games = fetch_player_games_this_week(fed_id)
        if games:
            games_ref.child(fed_id).set({str(i): g for i, g in enumerate(games)})

        print(f"synced {entry['name']} ({fed_id}) rating={rating}")


if __name__ == "__main__":
    init_app()
    sync_roster()
