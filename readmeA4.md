# UNO CLI — Build & Run Guide

## Requirements
- Java 17+
- Maven 3.9+
- Docker (optional)

## Local Build
mvn -B compile

## Local Test
mvn test

## Package
mvn package
# produces target/uno-game.jar

## Local Run
java -jar target/uno-game.jar --bots 3 --games 1

## Docker Build
docker build -t uno-game .

## Docker Run
docker run -it uno-game --bots 3 --games 1

## Logging
Game events (start, player turns, cards played/drawn, invalid input,
round/game end) are written to `game.log` in the working directory.
Console output is reserved exclusively for player-facing CLI text.