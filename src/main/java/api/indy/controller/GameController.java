package api.indy.controller;

import api.indy.auth.AuthRequired;
import api.indy.model.ErrorResponse;
import api.indy.model.game.CreateGameRequest;
import api.indy.model.game.Game;
import api.indy.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/game")
public class GameController {
    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @AuthRequired
    @PostMapping("/create")
    public ResponseEntity<Object> createGame(@RequestBody CreateGameRequest body, @RequestHeader("Authorization") String token) {
        if(body.name() == null || body.starting_player() == null || body.visibility() == null)
            return new ResponseEntity<>(new ErrorResponse("Missing required fields"), HttpStatus.BAD_REQUEST);

        String name = body.name();
        if(!name.matches("[a-zA-Z0-9 ]{3,20}"))
            return new ResponseEntity<>(new ErrorResponse("Invalid name format"), HttpStatus.BAD_REQUEST);

        String startingPlayer = body.starting_player();
        if(!startingPlayer.equalsIgnoreCase("X") && !startingPlayer.equalsIgnoreCase("O") && !startingPlayer.equalsIgnoreCase("random"))
            return new ResponseEntity<>(new ErrorResponse("Invalid starting player"), HttpStatus.BAD_REQUEST);

        try {
            Game.Visibility visibility = Game.Visibility.valueOf(body.visibility().toUpperCase());
            Game game;
            if(visibility.equals(Game.Visibility.PRIVATE)) {
                if(body.password() == null)
                    return new ResponseEntity<>(new ErrorResponse("Missing required fields"), HttpStatus.BAD_REQUEST);

                String password = body.password();
                if(!password.matches("[a-zA-Z0-9!@#$%^&*-_]{6,20}"))
                    return new ResponseEntity<>(new ErrorResponse("Invalid password format"), HttpStatus.BAD_REQUEST);

                game = this.gameService.createGame(token, name, startingPlayer, body.visibility(), password);
            } else game = this.gameService.createGame(token, name, startingPlayer, body.visibility());

            return new ResponseEntity<>(Map.of(
                "game_id", game.uuid().toString(),
                "join_code", game.joinCode()
            ), HttpStatus.CREATED);
        } catch(IllegalArgumentException e) {
            return new ResponseEntity<>(new ErrorResponse("Invalid visibility"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/resolve/{code}")
    public ResponseEntity<Object> resolveGame(@PathVariable String code) {
        if(!code.matches("[a-zA-Z0-9]{6}")) return new ResponseEntity<>(new ErrorResponse("Invalid code format"), HttpStatus.BAD_REQUEST);

        Game game = this.gameService.resolveGame(code);
        if(game == null) return new ResponseEntity<>(new ErrorResponse("Couldn't find the game with code: %s".formatted(code)), HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(game, HttpStatus.OK);
    }

    @GetMapping("/get/{gameId}")
    public ResponseEntity<Object> getGame(@PathVariable("gameId") String gameId) {
        try {
            Game game = this.gameService.getGame(UUID.fromString(gameId));
            if(game == null) return new ResponseEntity<>(new ErrorResponse("Couldn't find the game with id: %s".formatted(gameId)), HttpStatus.NOT_FOUND);

            return new ResponseEntity<>(game, HttpStatus.OK);
        } catch(IllegalArgumentException e) {
            return new ResponseEntity<>(new ErrorResponse("Invalid game id format"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorResponse(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<Game>> listGames() {
        return new ResponseEntity<>(this.gameService.listGames(), HttpStatus.OK);
    }
}
