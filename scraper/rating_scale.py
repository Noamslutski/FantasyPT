"""Maps a real Israeli chess rating to the 1-100 "card rating" and rarity tier shown in the app.

Kept as pure functions (no I/O) so the mapping is easy to tune once we see the real spread of
ratings across the Hapoel Petah Tikva roster — the constants below are a starting guess, not a
measured calibration.
"""

RATING_FLOOR = 800
RATING_CEILING = 2200

TIER_COMMON = "common"
TIER_RARE = "rare"
TIER_LEGEND = "legend"


def card_rating(rating_israel: int) -> int:
    span = RATING_CEILING - RATING_FLOOR
    scaled = round((rating_israel - RATING_FLOOR) / span * 100)
    return max(1, min(100, scaled))


def tier_for(card_rating_value: int) -> str:
    if card_rating_value >= 90:
        return TIER_LEGEND
    if card_rating_value >= 70:
        return TIER_RARE
    return TIER_COMMON
