# Refactoring Report

## What behavior did you characterize before refactoring?

Before touching any code, I ran the original game with several seeds and read every method in `Main.java`.
The `selfTest` method already provided nine assertions; I used those as a baseline contract.

I then identified the behaviors most at risk during refactoring and wrote characterization tests for them:

| Behavior area | Tests written |
|---|---|
| Card parsing (color, rank, number, points) | 18 assertions |
| Rule legality: color match | 5 |
| Rule legality: number match | 4 |
| Rule legality: action-type match | 5 |
| Wild always legal | 4 |
| Called color after a wild play | 4 |
| Scoring (face, action, wild point values) | 3 |
| Bot priority ordering (DRAW_TWO > SKIP > NUMBER > WILD) | 4 |
| Bot draws when no legal card | 1 |
| Bot color selection (most frequent color wins) | 3 |
| Deal: each player starts with 7 cards | 3 |
| Initial up-card is never a wild (30 seeds tested) | 30 |
| Draw reshuffles discard when deck exhausted | 1 |
| Turn advance and direction reversal | 2 |
| scoreAllOpponents sums correctly | 1 |
| Hand formatting matches original `join()` output | 3 |
| Deterministic score for seed 42 (regression) | 1 |
| Safety limit terminates game within 3000 turns | 1 |
| Reverse acts as skip in 2-player game | 1 |
| W4 is always legal (no challenge rule) | 2 |
| Bot uses called color after wild | 1 |
| Zero-card scores 0, not ASCII | 2 |
| Every pair of same-rank actions across all colors | 24 |

**Total: 142 assertions across 23 test groups, all passing before and after refactoring.**

## What were the worst design problems you found?

**1. Duplicated legality logic — highest risk.**  
The full legal-play condition appeared in three places: `isLegal()`, the game loop's `ok` block, and `chooseBotCard()`.  All three had to agree, and any one could silently diverge.

**2. Global mutable state.**  
`upCard`, `calledColor`, `currentPlayer`, `direction`, `deck`, `discard`, `hands` were all static fields.  There was no boundary between "game in progress" and "no game"; tests that mutated these fields interfered with each other.

**3. Console I/O mixed into rule execution.**  
`System.out.println` calls were scattered throughout the turn loop.  To test any game logic, a test had to tolerate console output or add a `--quiet` flag.

**4. Bot decisions embedded in the turn loop.**  
`chooseBotCard()` duplicated the legal-play condition rather than calling `isLegal()`, so a bug fix in one would not fix the other.

**5. Primitive card representation.**  
Every place that needed a card's color, rank, or point value duplicated the same `if (card.endsWith("S"))` chain.  There was no shared concept of "a card"; just string manipulation.

**6. Long turn loop (~130 lines).**  
The loop handled: drawing, bot decisions, human prompts, legality validation, card effects, scoring, UNO announcements, and loop termination.  Every responsibility was entangled.

## Which refactorings did you perform?

All changes were made in small, compilable, test-passing steps.

### Step 1 — Extract `Card` value object
`color()`, `rank()`, `number()`, `points()`, `isWild()` moved from static methods in `Main` into a focused, immutable `Card` class.  `Main`'s original methods were preserved in the `selfTest` path during this step.

### Step 2 — Extract `Rules` (centralize legality)
`isLegal()` and `scoreHand()` moved into a stateless `Rules` class.  The boolean condition is now written once.  `chooseBotCard()` (later `BotStrategy`) calls `Rules.isLegal()` instead of repeating the condition.

### Step 3 — Extract `GameState`
Deck, discard, hands, currentPlayer, direction, upCard, calledColor became fields of a non-static `GameState` object.  Global state was eliminated.  The object owns all mutations to cards in play and exposes `drawCard()`, `advancePlayer()`, `reverseDirection()`, `playCard()`, `addPenaltyCard()`, and `scoreAllOpponents()`.

### Step 4 — Extract `ConsoleView`
All `System.out.println` and `scanner.nextLine()` calls moved into a `ConsoleView` class.  The view knows how to display state but contains no rule logic.  The `quiet` flag lives here.

### Step 5 — Extract `BotStrategy`
`chooseBotCard()` and `chooseBotColor()` became static methods in `BotStrategy`.  Bot logic now calls `Rules.isLegal()` and `Card.rank()` rather than reimplementing them.  The priority order (DRAW_TWO > SKIP > NUMBER > WILD) is now explicit and testable.

### Step 6 — Refactor `Main.playGame()`
The turn loop was split into clearly labelled phases: choose-card, handle-draw, penalty-for-bad-index, validate-legality, play-card, apply-effect.  `applyCardEffect()` replaced the long if-else chain with a `switch` on `Card` rank constants.

### Step 7 — Write characterization tests (`test/CharacterizationTest.java`)
142 assertions covering all behaviors listed above, runnable via `scripts/test.sh`.

## What behavior did you intentionally preserve?

Every documented quirk from `docs/rules.html` is preserved and tested:

- **All hands printed each turn** — `ConsoleView.showTurnHeader` shows every player's hand.
- **Human may type `draw` even with a legal card** — `askHumanCardChoice` returns -1 for "DRAW" unconditionally.
- **Integer-index input is not pre-validated** — if the human types an in-range index for an illegal card, the game loop detects it and applies a penalty.  Card-code input is checked for legality before returning.
- **Bot plays a drawn card automatically when legal** — the `chosen = hand.size() - 1` path in the draw block is unchanged.
- **Reverse acts as skip in 2-player games** — `applyCardEffect` calls `advancePlayer()` twice after reversing in a 2-player game.
- **Safety limit of 3000 turns** — the `guard` loop variable is unchanged.
- **W4 is always legal** — `Rules.isLegal` returns `true` for any wild without checking the player's hand.
- **Deterministic output for a given seed** — confirmed by `testDeterministicGameScore`: seed 42 still produces Bot3 winning with 87 points.

## What risks remain?

**1. Static scores and player setup in `Main`.**  
`scores[]`, `playerNames`, and `humanPlayers` are still static fields on `Main`.  Running two games in the same JVM process could accumulate scores incorrectly.  Fixing this would require passing scores into `playGame()` explicitly — straightforward but not done here.

**2. `GameState` is not deeply cloned.**  
`handOf(i)` returns the live `ArrayList`.  External code that modifies the returned list mutates game state.  Defensive copies would add safety.

**3. Human input tested only through integration.**  
`ConsoleView.askHumanCardChoice` requires a live `Scanner`.  Testing it requires either mocking `System.in` or extracting an `InputSource` interface.  It is partially covered by the end-to-end deterministic game test, but not by isolated unit tests.

**4. Bot strategy is first-match-wins within priority.**  
The bot plays the first legal DRAW_TWO it finds, not the strategically best one (e.g., saving a card that matches the up-card's color).  This is preserved original behavior, not a regression.
