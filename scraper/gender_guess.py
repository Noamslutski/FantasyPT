"""Fallback gender guess from a Hebrew first name, used only if the scraper can't find an
explicit gender field on chess.org.il for a given player (TODO: check the advanced search
form / a men-vs-women rankings split via inspect_form.py — the user believes the federation's
data does expose gender somewhere, but the profile page HTML sampled while planning this didn't
show it). Unknown names default to "boy" since this is just an avatar pick, not gameplay data.
"""

GIRL_FIRST_NAMES = {
    "מיה", "נועה", "תמר", "שירה", "יעל", "רוני", "ליה", "הילה", "אביגיל", "שני",
    "עדן", "אור", "גאיה", "איילה", "נגה", "דניאלה", "מעיין", "עמית", "ליבי", "רותם",
}


def guess_gender(full_name: str) -> str:
    if not full_name:
        return "boy"
    first_name = full_name.strip().split()[0]
    return "girl" if first_name in GIRL_FIRST_NAMES else "boy"
