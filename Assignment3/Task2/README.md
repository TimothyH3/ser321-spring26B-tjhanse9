# Assignment 3 Task 2: Hangman Game Protocol

**Author:** [Timothy Hansen]
**Date:** [4/10/2026]

---

## How to Run
You can use Gradle to run things, running with ./gradlew is of course also an option
**Server:**
Default
```bash
gradle Server
```

With arguments
```bash
gradle Server -Pport=8888
```

**Client:**
Default but running more quietly on Gradle
```bash
gradle Client --console=plain -q
```

With arguments
```bash
gradle Client -Phost=localhost -Pport=8888
```

---

## Video Demonstration

**Link:** [https://arizonastateu-my.sharepoint.com/:v:/g/personal/tjhanse9_sundevils_asu_edu/IQCkRn3ZyFojSbuOh5UaAKfdAQQ_Jk6-CBm-uHBhckczp2w?e=Ywu3EY]

The video demonstrates:
- Starting server and client
- Complete game playthrough
- All implemented features

---

## Implemented Features Checklist

### Core Features (Required)
- [x] Set Player Name (provided as example)
- [x] Start New Game
- [x] Guess Letter
- [x] Game State
- [x] Win/Lose Detection
- [x] Graceful Quit

### Medium Features (Enhanced Gameplay)
- [x] Hint feature
- [x] Word Guessing
- [x] Guessed Letters Command
- [x] Give Up

### Advanced Features (Competition)
- [x] Scoring System
- [x] Leaderboard

**Note:** Mark [x] for completed features, [ ] for not implemented.

---

## Protocol Specification

### Overview
[Provide a brief overview of your protocol design - what patterns did you use, how does communication work, etc.]

---

### 1. Set Player Name

**Request:**
```json
{
    "type": "name",
    "name": "<string>"
}
```

**Success Response:**
```json
{
    "type": "name",
    "ok": true,
    "message": "Welcome <name>! ..."
}
```

**Error Response:**
```json
{
    "ok": false,
    "message": "Name cannot be empty"
}
```

---

### 2. Start New Game

**Request:**
```json
{
    "type": "newGame"
}
```

**Success Response:**
```json
{
    "ok": true,
    "type": "newGame",
    "message": "New game started",
    "misses": 0,
    "points": 0,
    "stage": "<ascii string>"
}
```

**Error Response(s):**
```json
{
    "ok": false,
    "message": "You are already in a game"
}
```
```json
{
    "ok": false,
    "message": "Error accessing word list"
}
```

---

### 3. Guess Letter

**Request:**
```json
{
    "type": "letter",
    "letter": "a"
}
```

**Success Response (Ongoing Game):**
```json
{
    "ok": true,
    "type": "guessLetter",
    "guessMessage": "Correct guess",
    "letter": "a",
    "inGame": true
}
```

**Success Response (Game Over - Win):**
```json
{
    "ok": true,
    "type": "winGame",
    "message": "You won!",
    "secretWord": "apple",
    "misses": 2,
    "hintsUsed": 0,
    "points": 35,
    "inGame": false
}
```

**Error Response(s):**
```json
{
    "ok": false,
    "message": "Letter has already been guessed"
}
```
```json
{
    "ok": false,
    "message": "Invalid guess"
}
```

---

### 4. Game State

**Request:**
```json
{
    "type": "gameState"
}
```

**Success Response:**
```json
{
    "ok": true,
    "type": "gameState",
    "message": "<ascii string for current hangman stage>",
    "progress": "a _ p _ e ",
    "misses": 2,
    "points": 10,
    "hintsUsed": 1
}
```

---

## Error Handling Strategy

**Server-side validation:**
- **Validations:** The server intercepts every incoming string and passes it through `isValid(String json)` to ensure it can actually be parsed as a JSONObject or JSONArray.
- **Missing fields:** The `testField(JSONObject req, String key)` helper method is invoked in handlers to verify required fields exist before attempting to extract them, preventing runtime exceptions.
- **Invalid data types:** Values are extracted using `.optString()` or type-safe `.getString()` only after field presence is confirmed.
- **Game state errors:** Game logic guards prevent duplicate guesses (checking the `usedLetters` set), block starting a new game while `inGame` is true, and handle empty string inputs safely.

---

## Robustness

**Server robustness:**
- The server loops `in.readObject()` inside a try-catch block. If a client abruptly disconnects or sends malformed serialized objects, it prints "Client disconnect", sets `connected = false`, safely drops the client loop, and resumes waiting for new connections at `serv.accept()`. 

**Client robustness:**
- The `sendRequest` helper wraps stream operations in a try-catch. If the server crashes or drops the connection, it catches the exception, sends an error, and returns `null`.
- Calling functions check for `has("key")` and `if (response == null)` before looking at data to prevent casting crashes from unexpected payloads or missing keys.

---

## Assumptions 

1. The leaderboard is kept in memory and is not meant to persist across server restarts.
2. The user will only be permitted to type single alphabetical characters when guessing letters.
3. The client-side logic can be modified as long as it does not carry the responsibilities of validation, game state, word selection, or scoring

---

## Known Issues

1. The leaderboard does not distinguish between players with the identical names.

---