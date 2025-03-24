package api.indy.websocket;

import api.indy.model.game.Game;
import api.indy.model.game.Player;
import api.indy.service.AuthService;
import api.indy.service.GameService;
import api.indy.util.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, UUID> clients;
    private final AuthService authService;
    private final GameService gameService;
    private final ObjectMapper serializer;
    private final Logger logger;

    @Autowired
    public GameSocketHandler(AuthService authService, GameService gameService) {
        this.clients = new ConcurrentHashMap<>();
        this.authService = authService;
        this.gameService = gameService;
        this.serializer = new ObjectMapper();
        this.logger = LoggerFactory.getLogger(GameSocketHandler.class);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        final Pattern pattern = Pattern.compile("/game/join/([a-z\\d-]+)");
        final String path = session.getAttributes().get("path").toString();
        final String query = session.getAttributes().get("query") == null ? "" : session.getAttributes().get("query").toString();

        Matcher matcher = pattern.matcher(path);
        if(!matcher.find()) {
            session.close(WebSocketStatus.BAD_REQUEST);
            return;
        }

        final String gameId = matcher.group(1);
        final Game game = this.gameService.getGame(UUID.fromString(gameId));

        if(game == null) {
            session.close(WebSocketStatus.NOT_FOUND);
            return;
        }

        if(game.visibility().equals(Game.Visibility.PRIVATE)) {
            final String password = Util.getParam(query, "pass");

            if(password == null || !authService.verifyPassword(password, game.password())) {
                session.close(WebSocketStatus.FORBIDDEN);
                return;
            }
        }

        if(game.playerCount() == 2) {
            session.close(WebSocketStatus.FORBIDDEN);
            return;
        }

        if(game.started()) {
            session.close(WebSocketStatus.FORBIDDEN);
            return;
        }

        session.getAttributes().put("gameId", gameId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, Object> data = serializer.readValue(message.getPayload(), Map.class);
        String type = data.get("type").toString();

        if(type.equalsIgnoreCase("auth")) {
            try {
                String token = data.get("token").toString();
                if(!authService.verifyToken(UUID.fromString(token))) {
                    session.close(WebSocketStatus.UNAUTHORIZED);
                    return;
                }

                session.getAttributes().put("token", token);

                UUID gameId = UUID.fromString(session.getAttributes().get("gameId").toString());
                Game game = this.gameService.getGame(gameId);
                if(game == null) {
                    session.close(WebSocketStatus.NOT_FOUND);
                    return;
                }

                if(!game.join(session)) {
                    session.close(WebSocketStatus.INTERNAL_SERVER_ERROR);
                    return;
                }

                session.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                        "type", "on_join",
                        "game", game,
                        "is_owner", game.owner().equals(token)
                ))));

                for(WebSocketSession player : game.players().keySet()) {
                    if(player == session) continue;

                    player.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                            "type", "player_join",
                            "player", game.players().get(session)
                    ))));
                }

                this.clients.put(session, gameId);
                return;
            } catch(IllegalArgumentException e) {
                session.close(WebSocketStatus.UNAUTHORIZED);
                return;
            } catch(NullPointerException e) {
                session.close(WebSocketStatus.BAD_REQUEST);
                return;
            }
        }

        if(session.getAttributes().get("token") == null) {
            session.close(WebSocketStatus.UNAUTHORIZED);
            return;
        }

        UUID gameId = this.clients.get(session);
        Game game = this.gameService.getGame(gameId);

        switch(type.toLowerCase()) {
            case "update_readiness" -> {
                if(!(data.get("ready") instanceof Boolean)) {
                    this.sendErrorMessage(session, "Expected boolean value for 'ready' (Received %s instead).".formatted(data.get("ready").getClass().getSimpleName()), "console");
                    return;
                }
                boolean player_ready = (Boolean) data.get("ready");

                game.playerReady(session, player_ready);
                for(WebSocketSession player : game.players().keySet()) {
                    player.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                        "type", "player_ready",
                        "player_id", game.players().get(session).id(),
                        "ready", player_ready
                    ))));
                }

                if(game.playerCount() == 2 && game.allPlayersReady()) game.start();
            } case "update_symbols" -> {
                if(!Objects.equals(game.owner(), session.getAttributes().get("token").toString())) {
                    this.sendErrorMessage(session, "notAllowed", "user");
                    return;
                }

                List<Map<String, Object>> entries = (List<Map<String, Object>>) data.get("symbols");
                for(Map<String, Object> entry : entries) {
                    int player_id = (Integer) entry.get("player_id");
                    String symbol = (String) entry.get("symbol");

                    Player player = game.getPlayerById(player_id);
                    if(player == null) continue;
                    player.setSymbol(symbol);

                    for(WebSocketSession playerSession : game.players().keySet()) {
                        if(playerSession == session) continue;
                        playerSession.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                            "type", "player_symbol",
                            "player_id", player_id,
                            "symbol", symbol
                        ))));
                    }
                }
            } case "move" -> {
                if(!(data.get("row") instanceof Integer) || !(data.get("col") instanceof Integer)) {
                    this.sendErrorMessage(session, "Expected integer values for 'row' and 'col' (Received %s and %s instead)."
                            .formatted(data.get("row").getClass().getSimpleName(), data.get("col").getClass().getSimpleName()), "console");
                    return;
                }
                int row = (Integer) data.get("row");
                int col = (Integer) data.get("col");

                game.move(row, col);
            } default -> {
                this.sendErrorMessage(session, "Invalid message type", "console");
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        logger.info("Connection closed {} {}", status.getCode(), status.getReason() == null ? "" : status.getReason().toUpperCase().replace(" ", "_"));

        UUID gameId = this.clients.get(session);
        Game game = this.gameService.getGame(gameId);
        if(game == null) return;

        Player player = game.players().get(session);
        game.leave(session);
        this.clients.remove(session);

        if(game.playerCount() == 0) this.gameService.deleteGame(gameId);
        else if(game.playerCount() == 1 && game.started()) {
            for(WebSocketSession playerSession : game.players().keySet()) playerSession.close();
            this.gameService.deleteGame(gameId);
        } else {
            for(WebSocketSession playerSession : game.players().keySet()) {
                try {
                    playerSession.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                        "type", "player_leave",
                        "player_id", player.id()
                    ))));
                } catch (Exception ignored) {}
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String message, String target) throws IOException {
        session.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
            "type", "error",
            "target", target,
            "error", message
        ))));
    }
}
