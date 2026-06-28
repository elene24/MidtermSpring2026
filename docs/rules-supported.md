# Rules Supported

This maps each rule in `Final_Project_UNO_rules_reference.md` to how this
implementation behaves. Anything not listed as a variant follows the
reference document exactly.

## Deck Composition — Implemented

`GameState.buildDeck()` builds the full 108-card deck: 4 colors, one `0` and
two each of `1-9`, two Skip / Reverse / Draw Two per color, four Wild, and
four Wild Draw Four. Verified by `FinalProjectFeatureTest`.

## Legal Play Validation — Implemented

`Rules.isLegal` matches by color, number, action type, or wild status, and
respects the called color after a wild. Covered by `CharacterizationTest`.

## Skip — Implemented

Next player loses their turn (`Main.applyCardEffect`, `Card.SKIP` case).

## Reverse — Implemented, with a documented 2-player variant

- 3-4 players: direction flips, play continues with the new next player.
- **2-player variant:** Reverse acts as a Skip (the same player effectively
  goes again). This is a standard, explicitly-allowed simplification from the
  reference document and is exercised by
  `testReverseActsAsSkipInTwoPlayerGame`.

## Draw Two — Implemented

Next player draws two cards and loses their turn. No stacking variant is
implemented (acceptable simplification).

## Wild — Implemented

Player chooses the next active color via `setCalledColor`; subsequent
`Rules.isLegal` calls honor it.

## Wild Draw Four — Implemented, simplified

Next player draws four cards and loses their turn. **No challenge rule** is
implemented — Wild Draw Four is always legal regardless of whether the
player holds a matching color (`testWildW4IsAlwaysLegal`). This is an
explicitly acceptable simplification.

## Draw/Pass Behavior — Implemented

Variant chosen: **draw one card, then play it immediately if legal,
otherwise pass.** Bots auto-play a legal drawn card; humans are asked
(`ConsoleView.askPlayDrawn`).

## UNO Call And Missed-UNO Penalty — Implemented, simplified timing

- One-card state is detected directly from hand size.
- Bots always call UNO automatically the instant they reach one card
  (documented simplification — bots cannot be caught missing a call).
- Humans are prompted immediately after dropping to one card
  (`ConsoleView.askCallUno`). Declining is treated as the missed-call window
  closing immediately, and the penalty (`Rules.MISSED_UNO_PENALTY_CARDS` = 2
  cards) is applied right away via `GameState.checkAndPenalizeMissedUno`.
- This is a simplification of "before the next relevant action" — rather than
  waiting for another player to "catch" the lapse, the check happens at the
  same point in the turn the call would have been made.

## Round Scoring And Multi-Round Target — Implemented

- At round end, `GameState.scoreAllOpponents` sums opponents' remaining card
  values using the standard point table (number = face value, Skip/Reverse/
  Draw Two = 20, Wild/Wild Draw Four = 50).
- `Main` plays successive rounds, dealing a fresh `GameState` each round and
  accumulating scores in a persistent `int[] scores` array, until
  `Rules.targetReached` reports a player has reached the target score
  (`--target`, default `500`) or an optional `--games` round cap is hit.
- The final winner is `Rules.leaderIndex(scores)` (highest score; ties break
  toward the lower player index — documented in
  `leaderIndexBreaksTiesTowardLowerIndex`).

## Acceptable Simplifications In Use

- No Wild Draw Four challenge rule.
- No draw-card stacking on Draw Two.
- Simple, deterministic-by-seed bot behavior (priority list, not adaptive).
- Text-only CLI interaction.
- Configurable but defaulted fixed target score (500), with an optional
  round cap for short test runs.
- Deterministic deck setup is supported for tests via a fixed `Random` seed.
