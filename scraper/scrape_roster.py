"""Scrapes the Hapoel Petah Tikva roster from chess.org.il's advanced player search.

NOTE ON FIELD NAMES: the search form is an ASP.NET postback, and the exact `name` attribute of
the club dropdown / search button below (CLUB_SELECT_NAME, SUBMIT_BUTTON_NAME) is a best guess
based on the site's other controls (e.g. "ctl00$ContentPlaceHolder1$AdvancedSearchLinkButton").
Before relying on this script, run:

    python inspect_form.py https://www.chess.org.il/players/searchplayers.aspx

...click "advanced search" once by hand in a browser with devtools open (or extend this script
to postback AdvancedSearchLinkButton first), and correct the constants below to match what's
actually printed.
"""

from chess_org_il import BASE_URL, new_session, get_soup, postback, player_id_from_href

SEARCH_URL = f"{BASE_URL}/players/searchplayers.aspx"

ADVANCED_SEARCH_LINK = "ctl00$ContentPlaceHolder1$AdvancedSearchLinkButton"
CLUB_SELECT_NAME = "ctl00$ContentPlaceHolder1$ClubDropDownList"  # TODO verify via inspect_form.py
SUBMIT_BUTTON_NAME = "ctl00$ContentPlaceHolder1$SearchButton"  # TODO verify via inspect_form.py
HAPOEL_PETAH_TIKVA_CLUB_ID = "30"  # confirmed via clubs/Club.aspx?Id=30


def fetch_roster(club_id: str = HAPOEL_PETAH_TIKVA_CLUB_ID):
    session = new_session()
    soup = get_soup(session, SEARCH_URL)

    # Reveal the advanced-search fields (club filter lives behind this toggle).
    soup = postback(session, SEARCH_URL, soup, ADVANCED_SEARCH_LINK, {})

    # Submit the club filter.
    soup = postback(session, SEARCH_URL, soup, SUBMIT_BUTTON_NAME, {CLUB_SELECT_NAME: club_id})

    roster = []
    seen_ids = set()
    for link in soup.find_all("a", href=True):
        if "Player.aspx" not in link["href"]:
            continue
        fed_id = player_id_from_href(link["href"])
        if not fed_id or fed_id in seen_ids:
            continue
        name = link.get_text(strip=True)
        if not name:
            continue
        seen_ids.add(fed_id)
        roster.append({"fedId": fed_id, "name": name})

    return roster


if __name__ == "__main__":
    for player in fetch_roster():
        print(player)
