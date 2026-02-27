package com.fortnite.pronos.service.ingestion;

record PrIngestionCounters(
    int playersCreated,
    int playersUpdated,
    int snapshotsWritten,
    int scoresWritten,
    int skippedRows) {

  static PrIngestionCounters empty() {
    return new PrIngestionCounters(0, 0, 0, 0, 0);
  }
}
