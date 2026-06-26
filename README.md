# Midterm UNO CLI

This is a standalone CLI UNO-like game.

The code is written as plausible feature-grown Java: almost everything lives in one procedural `Main` class. It works, but it has mixed responsibilities, duplicated rule logic, primitive-heavy card handling, global state, and condition-heavy gameplay code. The goal is to refactor it safely, not rewrite it.

## Compile

```bash
scripts/compile.sh
```

## Run Bot Games

```bash
scripts/run.sh --bots 3 --games 5 --quiet
```

## Run Interactive Game

```bash
scripts/run.sh --human --bots 2 --games 1
```

Card input examples:

```text
R5   red 5
YS   yellow skip
BR   blue reverse
G+2  green draw two
W    wild
W4   wild draw four
draw draw a card
```

## Characterization Checks

```bash
scripts/test.sh
```

## A4 — Build & Run Guide

### Requirements
- Java 17+
- Maven 3.9+
- Docker (optional)

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
# produces target/uno-game.jar
```

### Local Run
```bash
java -jar target/uno-game.jar --bots 3 --games 1
```

### Docker Build
```bash
docker build -t uno-game .
```

### Docker Run
```bash
docker run -it uno-game --bots 3 --games 1
```

### Logging
Game events (start, player turns, cards played/drawn, invalid input, round/game end) are written to `game.log` in the working directory. Console output is reserved exclusively for player-facing CLI text.

## Submission

Submit your work through GitHub:

1. Fork this repository to your GitHub account.
2. Clone your fork locally.
3. Complete the midterm work in your fork.
4. Commit your changes with clear commit messages.
5. Push your branch to GitHub.
6. Open a pull request from your fork back to the original repository.

Your pull request must include:

* refactored source code
* characterization tests
* `docs/refactoring-report.md`
* `docs/extension-readiness.md`

Do not submit a zip file instead of a pull request unless the instructor explicitly asks for it.

## Rules

See `docs/rules.html` for the implemented game rules.

## Midterm Materials

* `docs/midterm-exam.md`: midterm brief
* `docs/rubric.md`: grading rubric
* `docs/refactoring-guide.md`: suggested refactoring path