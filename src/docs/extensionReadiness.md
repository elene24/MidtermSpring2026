# Extension Readiness

## Which extension would your design support best?

**Adding a smarter bot strategy** — or multiple named strategies — is the easiest extension.

A close second is **adding a replay log**, since all game events now pass through `ConsoleView`.

## Where would that change be implemented?

### Smarter bot strategy

`BotStrategy.java` is a stateless utility class with two public methods:

```java
public static int    chooseCard(ArrayList<String> hand, String upCard, String calledColor)
public static String chooseColor(ArrayList<String> hand)
```

To add a new strategy (e.g., "defensive bot that always plays the lowest-scoring card"):

1. Add a second class `DefensiveBotStrategy` with the same two-method signature.
2. Change `Main.playGame()` to select the strategy per player:
   ```java
   int chosen = player.strategy().chooseCard(hand, state.upCard(), state.calledColor());
   ```
3. No other file needs to change.

No rule logic or console code needs to move.  The existing tests for `BotStrategy` also serve as the template for testing the new strategy.

To support **multiple strategies cleanly**, extract an interface:

```java
public interface BotStrategy {
    int    chooseCard(List<String> hand, String upCard, String calledColor);
    String chooseColor(List<String> hand);
}
```

`GameState` already stores per-player human flags (`humanFlags`); a parallel `List<BotStrategy>` would require only local changes to `Main.setupPlayers()` and `Main.playGame()`.

### Replay log

All game events are announced through `ConsoleView`.  A `ReplayLogger` that implements the same event methods (or wraps a `ConsoleView`) could intercept and write structured log lines without touching `GameState` or `Rules`.

## What part of your design still makes change difficult?

**1. `Main.playGame()` still does too much.**  
The turn loop handles drawing, validating, playing, scoring, and effect application.  `applyCardEffect()` was extracted, but the top-level loop is still ~80 lines.  Adding a new rule variant (e.g., stacking draw cards) would require editing this loop.

A `TurnResult` value object (or a small state machine) would let each phase be replaced independently, making variant rules easier to slot in.

**2. Card effects are handled with a `switch` in `Main`, not in the card itself.**  
If a new card type is added (e.g., "Swap Hands"), `Main.applyCardEffect()`, `Rules.isLegal()`, `Card.rank()`, and `Card.points()` all need to change.  Polymorphic card effects (each `Card` subclass knows its own effect) would isolate that change, but the cost of introducing an inheritance hierarchy needs to be weighed against project scale.

**3. Human input is not injectable.**  
`ConsoleView` requires a live `Scanner`.  Integration tests that exercise the human turn path must either pipe input through `System.in` or the view must accept an `InputSource` interface.  This is the main obstacle to replacing the CLI view with, say, a network-based view or a GUI.
