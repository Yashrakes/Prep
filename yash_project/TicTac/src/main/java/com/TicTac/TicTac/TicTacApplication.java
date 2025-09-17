package com.TicTac.TicTac;

import com.TicTac.TicTac.Model.Game;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


public class TicTacApplication {

	public static void main(String[] args) {
		Game game = new Game();
		game.initializeGame();
		System.out.println("game winner is: " + game.startGame());

		SpringApplication.run(TicTacApplication.class, args);
	}

}
