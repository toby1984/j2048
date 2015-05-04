# j2048

This is a simple 2048 clone in Java, written mostly because I wanted to implement an 'AI' that would play the game but also because I wanted to experiment with general game architecture stuff like getting the animations coded in a nice/clean way.

Architecture-wise I'm rather pleased with the solution, unfortunately the scoring function I'm using for the AI (which is just minimax with alpha-beta pruning) is pretty lackluster to say the least (you're lucky to see it getting tile values larger than 1024).

<img src="https://github.com/toby1984/j2048/blob/master/screenshot.png?raw=true" />

## Build requirements 

* JDK >= 1.8
* Apache Maven >= 3

## How to build

Just run
```
  mvn package
```

## How to run

Building the project will generate a self-executable JAR in the 'target' folder.

Execute it (assuming java is on your path) by simply running

```
java -jar target/j2048.jar [-ai]
```

The optional '-ai' option starts the game in 'auto-play' mode where the AI will try to solve the game.

## Controls

You may either use WASD or your cursor keys to control the game. Hitting the ENTER key or pressing the 'Restart' button will restart the game.




