"""One-off debugging tool — NOT part of the scrape pipeline.

chess.org.il's search/filter forms are ASP.NET postbacks, so the exact field name for e.g. the
"club" dropdown has to be read off the live HTML before scrape_roster.py can drive it. Run this
against the real page once, read the printed field names, then fill in CLUB_FIELD_NAME etc. at
the top of scrape_roster.py.

Usage:
    python inspect_form.py https://www.chess.org.il/players/searchplayers.aspx
"""

import sys

from chess_org_il import new_session, get_soup


def main(url: str) -> None:
    session = new_session()
    soup = get_soup(session, url)

    print(f"== forms on {url} ==")
    for form in soup.find_all("form"):
        print(f"\nform id={form.get('id')} action={form.get('action')}")

    print("\n== input fields ==")
    for tag in soup.find_all("input"):
        if tag.get("type") == "hidden":
            continue
        print(f"  name={tag.get('name')!r} type={tag.get('type')!r} value={tag.get('value')!r}")

    print("\n== select fields (dropdowns) ==")
    for select in soup.find_all("select"):
        print(f"\n  select name={select.get('name')!r} id={select.get('id')!r}")
        for option in select.find_all("option"):
            print(f"    value={option.get('value')!r} text={option.get_text(strip=True)!r}")

    print("\n== links that look like postback triggers (__doPostBack) ==")
    for tag in soup.find_all(href=True):
        if "__doPostBack" in tag["href"]:
            print(f"  text={tag.get_text(strip=True)!r} href={tag['href']!r}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1])
