# Final Project: Full UNO Product

A CLI UNO game built on the midterm's refactored architecture, extended with
fuller rules: UNO call/penalty and multi-round scoring to a target score.

## Architecture

- `Card` — parses a card code (e.g. `R5`, `GS`, `B+2`, `W`, `W4`) into color,
  rank, number, and point value.
- `Rules` — pure rule functions: legal-play checks, hand scoring, and
  match-level helpers (target score reached, leader index).
- `GameState` — mutable round state: deck, discard, hands, turn order,
  up-card, called color, and UNO-call tracking. No I/O.
- `BotStrategy` — bot card/color choices as pure functions.
- `ConsoleView` — all console I/O; contains no rule decisions.
- `Main` — orchestrates rounds and the overall match.

Game logic is fully testable without console input; see `docs/final-report.md`.

## Compile & Run

### Local Build
```bash
mvn -B compile
```

### Local Test
```bash
mvn test
```

### Package
```bash
mvn package
# produces target/uno-game.jar (shaded — includes slf4j/logback,
# runnable directly with `java -jar`)
```

### Run Bot-Only Match (to target score)
```bash
java -jar target/uno-game.jar --bots 3 --target 500
```

### Run Interactive Game
```bash
java -jar target/uno-game.jar --human --bots 2 --target 500
```

Verified end-to-end on a real run: human + 2 bots, illegal plays penalized,
Draw Two/Draw Four/Skip/Reverse/Wild all firing correctly, UNO called and
announced, and round scoring carrying a winner (187 points) into Round 2
without stopping, confirming the multi-round-to-target loop works as
designed.

### Flags
```text
--bots N     number of bot players (default 3)
--games N    caps the match at N rounds (0 = no cap, default; play until target)
--target N   score needed to win the match (default 500)
--human      add a human-controlled player
--quiet      suppress per-turn console output
--seed N     fixed RNG seed for deterministic/test runs
--self-test  run the legacy inline self-test
--help       show usage
```

### Docker
```bash
docker build -t uno-game .
docker run -it uno-game --bots 3 --target 500
```

## Card Input (Human Player)

```text
R5   red 5
YS   yellow skip
BR   blue reverse
G+2  green draw two
W    wild
W4   wild draw four
draw draw a card
```

When asked to call UNO after dropping to one card, answer `y`/`yes` or `n`/`no`.
Declining is treated as a missed call and is penalized immediately
(see `docs/rules-supported.md`).

## Logging

Game events are written to `game.log` in the working directory. Console
output is reserved for player-facing CLI text.

## Deliverables

- Source code: `src/main/java`
- Tests: `src/test/java`
- `README.md` (this file)
- `docs/rules-supported.md`
- `docs/final-report.md`

## Rules

See `docs/rules-supported.md` for the rule-by-rule implementation status,
and `Final_Project_UNO_rules_reference.md` for the grading reference.
