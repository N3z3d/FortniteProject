package com.fortnite.pronos.adapter.out.scraping;

/** Represents one scraped leaderboard row from FortniteTracker.com. */
record ScrapedRow(String nickname, String region, int points, int rank) {}
