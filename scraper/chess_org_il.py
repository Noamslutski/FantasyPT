"""Shared helpers for scraping chess.org.il (Israeli Chess Federation).

The site is classic ASP.NET WebForms: forms use __VIEWSTATE / __EVENTVALIDATION postbacks
instead of a JSON API, so every "search" or "filter" action here is really a POST that
resubmits the whole page with different field values. These helpers only depend on the
generic ASP.NET machinery (which never changes name), not on any page-specific field name.
"""

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://www.chess.org.il"
HEADERS = {"User-Agent": "Mozilla/5.0 (fantasyPT roster sync; contact via github.com/Noamslutski/FantasyPT)"}


def new_session() -> requests.Session:
    session = requests.Session()
    session.headers.update(HEADERS)
    return session


def get_soup(session: requests.Session, url: str) -> BeautifulSoup:
    resp = session.get(url, timeout=20)
    resp.raise_for_status()
    return BeautifulSoup(resp.text, "html.parser")


def hidden_fields(soup: BeautifulSoup) -> dict:
    """Every ASP.NET postback must echo back __VIEWSTATE / __VIEWSTATEGENERATOR /
    __EVENTVALIDATION (and any other hidden input) exactly as the server last sent them."""
    fields = {}
    for tag in soup.find_all("input", type="hidden"):
        name = tag.get("name")
        if name:
            fields[name] = tag.get("value", "")
    return fields


def postback(session: requests.Session, url: str, soup: BeautifulSoup, event_target: str,
             extra_fields: dict, event_argument: str = "") -> BeautifulSoup:
    """Simulates clicking/selecting an ASP.NET WebForms control that triggers __doPostBack.

    event_target is the control's `name` attribute (e.g. "ctl00$ContentPlaceHolder1$SearchButton").
    extra_fields should include the current value of every visible input/select on the form
    (checkboxes, textboxes, dropdowns) so the server sees a consistent submission.
    """
    data = hidden_fields(soup)
    data["__EVENTTARGET"] = event_target
    data["__EVENTARGUMENT"] = event_argument
    data.update(extra_fields)
    resp = session.post(url, data=data, timeout=20)
    resp.raise_for_status()
    return BeautifulSoup(resp.text, "html.parser")


def player_id_from_href(href: str):
    """Pulls the numeric player id out of a link like 'Player.aspx?Id=200154'."""
    if not href or "Id=" not in href:
        return None
    return href.split("Id=")[-1].split("&")[0]
