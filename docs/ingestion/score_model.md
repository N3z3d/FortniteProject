# Score model (PR ingestion)

Objectif: clarifier la separation entre score annuel courant et historique.

## Regles metier
- Table `scores`: 1 ligne par joueur + saison (score annuel courant).
- Table `pr_snapshots`: historique des points par region + date.
- Les graphs et historiques doivent s appuyer sur `pr_snapshots`.

## Comportement d ingestion
- Chaque ligne ingeree met a jour `scores` pour la saison donnee.
- Les snapshots sont conserves par (player_id, region, snapshot_date).

## Consequences
- Une nouvelle ingestion pour la meme saison ecrase le score courant.
- L historique reste disponible via les snapshots.
