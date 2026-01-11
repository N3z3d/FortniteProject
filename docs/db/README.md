# JIRA-1B Database Decision (Spike)

Decision
- Create the DB now for local/dev using Postgres + Flyway.
- Keep H2 in-memory for tests.

Why
- Unblocks dashboard and in-game flows that require real data.
- Aligns API contracts with the real schema early.
- Flyway migrations already exist, so setup cost is low.

Risks
- Local setup time and config drift across machines.
- Seed data can diverge between dev and test.

Plan (execution)
1. Start Postgres locally (example below).
2. Run backend with the dev profile so Flyway applies migrations.
3. Use existing seed migrations for fake data (V8/V11).
4. Run frontend and verify API connectivity on /api/games and /api/games/{id}.

Local dev setup (< 15 min)
```bash
docker run --name fortnite-postgres -e POSTGRES_USER=fortnite_user -e POSTGRES_PASSWORD=fortnite_pass -e POSTGRES_DB=fortnite_pronos -p 5432:5432 -d postgres:16
```
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```
```bash
cd frontend && npm start
```

Test DB plan
- Use H2 in-memory with `src/test/resources/application-test.yml` (`mvn test`).
- For dev manual tests, keep Postgres + Flyway seed data.

Reset / truncate (Postgres)
```sql
TRUNCATE TABLE game_participant_players, game_participants, game_region_rules, games,
team_players, trades, scores, teams, players, users, notifications, scrape_runs
RESTART IDENTITY CASCADE;
```

Checklist
- [ ] Postgres running on localhost:5432
- [ ] Flyway migrations applied on startup
- [ ] Seed game visible in /api/games
- [ ] Frontend loads dashboard in < 15 min
- [ ] In-game view works with a seeded game ID
- [ ] Reset command verified on dev DB
