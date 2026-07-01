"""Scrapes one player's profile (rating, club) and this week's games from chess.org.il.

NOTE: the "משחקים" (games) tab's exact mechanism (separate query param vs. another postback)
wasn't confirmed while researching this site from outside a browser — run inspect_form.py
against a real Player.aspx?Id=<id> page and check the tab links' hrefs before trusting
GAMES_TAB_QUERY_PARAM below.
"""

import re
from datetime import datetime, timedelta

from chess_org_il import BASE_URL, new_session, get_soup

PLAYER_URL = f"{BASE_URL}/Players/Player.aspx?Id={{fed_id}}"
GAMES_TAB_QUERY_PARAM = "TabId=Games"  # TODO verify against the real "משחקים" tab link


def fetch_player_profile(fed_id: str, session=None) -> dict:
    session = session or new_session()
    soup = get_soup(session, PLAYER_URL.format(fed_id=fed_id))

    text = soup.get_text(" ", strip=True)
    rating_match = re.search(r"מד\s*כושר\s*ישראלי\D{0,10}(\d{3,4})", text)
    rating = int(rating_match.group(1)) if rating_match else None

    club_match = re.search(r"מועדון[:\s]+([^\n|]{2,40})", text)
    club = club_match.group(1).strip() if club_match else None

    return {"fedId": fed_id, "rating": rating, "club": club}


def fetch_player_games_this_week(fed_id: str, session=None) -> list:
    session = session or new_session()
    url = f"{PLAYER_URL.format(fed_id=fed_id)}&{GAMES_TAB_QUERY_PARAM}"
    soup = get_soup(session, url)

    week_ago = datetime.now() - timedelta(days=7)
    games = []
    # Expect the games tab to render as an ASP.NET GridView (one <tr> per game) — each row's
    # exact column order needs confirming against the live page before this is trustworthy.
    for row in soup.select("table tr"):
        cells = [c.get_text(strip=True) for c in row.find_all("td")]
        if len(cells) < 3:
            continue
        games.append({"raw_cells": cells})

    return games  # TODO: map raw_cells -> {opponent, result, date, tournament} once column order is confirmed


if __name__ == "__main__":
    import sys
    fed_id = sys.argv[1] if len(sys.argv) > 1 else "30"
    print(fetch_player_profile(fed_id))
    print(fetch_player_games_this_week(fed_id))
