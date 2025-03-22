package api.indy.service;

import api.indy.model.game.Game;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {
    private final AuthService authService;
    private final Map<UUID, Game> games;
    private final Map<String, UUID> gameCodes;

    public GameService(AuthService authService) {
        this.authService = authService;
        this.games = new HashMap<>();
        this.gameCodes = new HashMap<>();
    }

    private String generateCode() {
        String code = UUID.randomUUID().toString().substring(0, 6);
        while(this.gameCodes.containsKey(code.toUpperCase())) code = UUID.randomUUID().toString().substring(0, 6);

        return code.toUpperCase();
    }

    public Game createGame(String ownerId, String name, String starting_player, String visibility) {
        UUID gameId = UUID.randomUUID();
        String code = this.generateCode();

        Game game = new Game(gameId, ownerId, name, starting_player, visibility, code, null);
        this.games.put(gameId, game);
        this.gameCodes.put(code, gameId);

        return game;
    }

    public Game createGame(String ownerId, String name, String starting_player, String visibility, String password) {
        UUID gameId = UUID.randomUUID();
        String code = this.generateCode();

        Game game = new Game(gameId, ownerId, name, starting_player, visibility, code, authService.hashPassword(password));
        this.games.put(gameId, game);
        this.gameCodes.put(code, gameId);
        return game;
    }

    public void deleteGame(UUID gameId) {
        Game game = this.games.get(gameId);
        if(game == null) return;

        this.games.remove(gameId);
        this.gameCodes.remove(game.joinCode());
    }

    public Game getGame(UUID gameId) {
        return this.games.get(gameId);
    }

    public Game resolveGame(String code) {
        UUID gameId = this.gameCodes.get(code.toUpperCase());
        if(gameId == null) return null;

        return this.games.get(gameId);
    }

    public List<Game> listGames() {
        return new ArrayList<>(this.games.values()).stream()
                .filter(game -> game.visibility() == Game.Visibility.PUBLIC)
                .collect(Collectors.toList());
    }
}
