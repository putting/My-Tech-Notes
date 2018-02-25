# Problem: Scrabble

## TODO: Set the problem as a MUCH simplified version of the game 
  - Must have simple inputs
  - NB letter count and value could be an external api - but nort really too simple
  - Not sure how you can evolve this quest to real world problems (its a basic modelloing ques)
  -BTW: investigate here for problem solving (https://www.reddit.com/r/dailyprogrammer/)
  
## Some refs:
[long impl](https://github.com/sagnew/WordsWithoutFriends/blob/master/Scrabble.java)
Look further into this:
[dzone-scrabble sets](https://dzone.com/articles/java-code-solution-scrabble-sets)
(https://github.com/ozan/solutions/tree/master/exercism/java/scrabble-score)
(https://codereview.stackexchange.com/questions/39922/scrabble-algorithm-review-and-performance-suggestions)
[Problem defn](http://wiki.openhatch.org/Scrabble_challenge)

## Some design patterns
The basic structure of your game engine uses the State Pattern. 
**Very much a Game State - With Phases/turns **
The items of your game box are singletons of various classes. 
The structure of each state may use Strategy Pattern or the Template Method.
Each turn for a player can have a Strategy

A Factory is used to create the players which are inserted into a list of players, another singleton. The GUI will keep 
watch on the Game Engine by using the Observer pattern and interact with this by using one of several 
Command object created using the Command Pattern. The use of Observer and Command can be used with in the context of a 
Passive View But just about any MVP/MVC pattern could be used depending on your preferences. When you save the game you 
need to grab a memento of it's current state
  
## Game Characteristics
  - 2 Player (for starters)
  - Game must randolmnly assign 10 letters to each player
  - They take a **Turn** (word must be checked against a dictionary)
  - Score calculated for each letter (NB. OO each )
```java
public class Letter
Char letter;
int score

public class Turn {

}

public class Player{
}

public class Scrabble {
List<Player> players;

//State
Player nextTurn;
Map<Player, List<Score>> scoreboard

void startGame() {
  allocate letters to players.
  assign nextTurn to PlayerX
  Player x plays
  Score added
  Player Y



```
