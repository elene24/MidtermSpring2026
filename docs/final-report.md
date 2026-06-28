# Final Report

## What UNO Rules Are Implemented

All rule-feature menu items are implemented:

- Correct 108-card deck composition (4 colors, numbers 0-9, Skip, Reverse,
  Draw Two, Wild, Wild Draw Four).
- Legal play validation by color, number, action type, or wild status,
  including the called color after a wild.
- Skip, Reverse (with a documented 2-player-acts-as-skip variant), Draw Two,
  Wild, and Wild Draw Four, all with their next-player effects.
- Draw/pass behavior: draw one card, play it immediately if legal, else pass.
- UNO call and missed-call penalty (two-card draw), with a documented,
  simplified detection window.
- Round scoring using the standard UNO point table, and a multi-round game
  that continues until a player reaches a target score (default 500,
  configurable with `--target`).

See `docs/rules-supported.md` for the full point-by-point mapping and every
documented variant or simplification.

## How The Game Is Played From The CLI

```bash
scripts/run.sh --human --bots 2 --target 500
```

Each turn shows the up card, any called color, and the current player's
hand as `index:code` pairs (e.g. `0:R5 1:GS 2:W`). The human player types
either:

- a hand index (e.g. `1`),
- a card code directly (e.g. `R5`), which is checked for legality, or
- `draw` to draw a card, after which they are asked whether to play it if it
  turns out to be legal.

When a wild is played, the player is asked to call a color (`R/Y/G/B`).
When a player drops to one card, they are asked whether to call UNO; saying
no triggers the documented missed-call penalty immediately. When a round
ends, scores are added to a running total; the match continues with fresh
rounds until someone reaches the target score, at which point final scores
and the match winner are printed.

Run `scripts/run.sh --help` for all flags (`--bots`, `--games`, `--target`,
`--human`, `--quiet`, `--seed`).

## How The Architecture Separates Game Logic From CLI Interaction

- **`Card`** — parses a card code into color/rank/number/points. No I/O, no
  game state.
- **`Rules`** — pure functions: legal-play checks, hand scoring, and
  match-level helpers (`targetReached`, `leaderIndex`). No I/O.
- **`GameState`** — owns the mutable round data (deck, discard, hands, turn
  order, up-card, called color, UNO-call flags). All mutation methods are
  callable and testable directly, with no console interaction.
- **`BotStrategy`** — bot decision-making (card choice, color choice) as pure
  functions over a hand and the current up-card/called color.
- **`ConsoleView`** — the only class that touches `System.in`/`System.out`.
  It formats output and parses human input, but contains no rule decisions
  beyond calling `Rules.isLegal` to give immediate feedback on a typed card
  code.
- **`Main`** — orchestrates: parses CLI args, runs rounds via `GameState`,
  and calls `ConsoleView` for all display/prompts. The per-round loop
  (`playGame`) and the match loop (`main`) contain no rule logic of their
  own beyond sequencing calls to `Rules`/`GameState`/`BotStrategy`.

Because all rule and state logic lives outside `ConsoleView`, every rule
behavior in this report is tested directly against `GameState`/`Rules`
without simulating console input.

## What Tests Were Added

- `CharacterizationTest` (carried over from the midterm, unchanged): card
  parsing, legal-play matching rules, scoring, bot behavior, deal/draw/turn
  mechanics, a deterministic end-to-end game, and documented quirks (e.g.
  Reverse-as-Skip in 2-player, no Wild Draw Four challenge).
- `FinalProjectFeatureTest` (new): deck composition counts (108 total, exact
  per-rank and per-color counts), UNO one-card detection, valid/invalid call
  attempts, missed-call penalty application, call-flag reset, round scoring
  of opponents' remaining hands, target-score detection, leader/tie-break
  behavior, and a full multi-round run that confirms a match terminates once
  a score reaches the target.
- `CharacterizationTestJUnitTest` runs the legacy suite through `mvn test`.

### Verified Build And Run

`mvn clean test` and `mvn package` were both run successfully on the actual
submission machine:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
[INFO] Building jar: target\uno-game.jar
[INFO] Including org.slf4j:slf4j-api ... in the shaded jar.
```

(142 characterization checks + 13 new final-project feature tests, all
passing, with the shaded jar produced at `target/uno-game.jar`.)

A live interactive run (`java -jar target/uno-game.jar --human --bots 2
--target 500`) was also exercised by hand and confirmed: Skip, Reverse, Draw
Two, Draw Four, Wild color selection, illegal-play penalties, the UNO call
and announcement, end-of-round scoring (one round ended with a 187-point
win), and the match continuing into Round 2 without stopping — confirming
the multi-round-to-target loop behaves as designed rather than only in
unit tests.

## What Limitations Remain

- No Wild Draw Four challenge rule (a player can never be challenged for
  playing it with another legal card in hand).
- No Draw Two/Wild Draw Four stacking.
- The UNO missed-call window is simplified: it is enforced immediately when
  a human declines the prompt rather than waiting for another player's
  turn to "catch" the lapse. Bots never miss a call.
- Bot strategy is a fixed priority list, not adaptive or difficulty-tunable.
- The CLI is text-only; there is no graphical card display.
- The match-ending tie-break (equal scores at/above target) goes to the
  lower player index rather than a sudden-death round.
