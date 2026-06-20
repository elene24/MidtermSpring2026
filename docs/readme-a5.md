# Database & Persistence Guide (Assignment 5)

## Selected technology

- **Database**: H2 (file-based for normal runs, in-memory for tests)
- **ORM**: Hibernate, accessed through standard Jakarta Persistence (JPA) APIs

No raw SQL appears in game logic. All persistence goes through entity classes
(`PlayerEntity`, `GameEntity`, `RoundEntity`, `ScoreEntity`) and repository
classes (`PlayerRepository`, `GameRepository`, `ReportRepository`) in the
`persistence` package, using JPQL queries.

## Schema

| Table          | Purpose                                              |
|----------------|-------------------------------------------------------|
| `players`      | One row per distinct player name ever seen           |
| `games`        | One row per program session (one run of `Main`)       |
| `game_players` | Join table: which players took part in which game     |
| `rounds`       | One row per completed hand (one call to `playGame()`) |
| `scores`       | One row per player's points in a given round          |

`rounds.winner_player_id` references `players` (nullable — null if the
3000-turn safety limit was hit with no winner).

The schema is created/updated automatically on first run
(`hibernate.hbm2ddl.auto=update` in `persistence.xml`) — no manual SQL
scripts need to be run.

## Configuration

Config lives in `src/main/resources/META-INF/persistence.xml`. Two
persistence units are defined:

- `uno-pu` — used by the real game. Points at a file-based H2 database at
  `./data/uno.mv.db` (created automatically, relative to wherever the jar is
  run from). No credentials beyond H2's default local-only `sa` user with no
  password, which is not a real secret and is never used outside local
  development.
- `uno-test-pu` — used only by tests. Points at an isolated in-memory H2
  database (`jdbc:h2:mem:unotest`) with `create-drop`, so every test run
  starts from a completely fresh schema and never touches the real database
  file or any other developer's machine state.

## How to view game history and statistics

Run the game normally one or more times to build up history:

```
java -jar target/uno-game.jar --bots 3 --games 5
```

Then view reports:

```
java -jar target/uno-game.jar --report
```

This prints:
- the 10 most recently completed rounds (with winner and timestamp)
- each player's total win count, most wins first
- the 10 highest individual round scores recorded, across all players

## How to run persistence tests

```
mvn test
```

This runs `PersistenceTest` (in addition to the existing `Assignment 4`
characterization tests) against the isolated in-memory `uno-test-pu` unit.
No setup step is required — the in-memory database is created and destroyed
automatically within each test method.

## Resetting local data

The file database lives under `./data/`. To start over with a clean history:

```
rm -rf data/        # macOS/Linux
Remove-Item -Recurse -Force data\   # Windows PowerShell
```