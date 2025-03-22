package api.indy.websocket;

import api.indy.model.game.Game;
import api.indy.model.game.Player;
import api.indy.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, UUID> clients;
    private final GameService gameService;
    private final ObjectMapper serializer;

    @Autowired
    public GameSocketHandler(GameService gameService) {
        this.clients = new ConcurrentHashMap<>();
        this.gameService = gameService;
        this.serializer = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = session.getAttributes().get("token").toString();
        UUID gameId = UUID.fromString(session.getAttributes().get("gameId").toString());
        Game game = this.gameService.getGame(gameId);
        if(game == null) {
            session.close();
            return;
        }

        if(!game.join(session)) {
            session.close();
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
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        UUID gameId = this.clients.get(session);
        Game game = this.gameService.getGame(gameId);

        Map<String, Object> data = serializer.readValue(message.getPayload(), Map.class);
        String type = data.get("type").toString();
        switch(type) {
            case "update_readiness" -> {
                boolean player_ready = (Boolean) data.get("ready");

                game.playerReady(session, player_ready);
                for(WebSocketSession player : game.players().keySet()) {
                    player.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                        "type", "player_ready",
                        "player_id", game.players().get(session).id(),
                        "ready", player_ready
                    ))));
                }

                if(game.playerCount() != 2 || !game.allPlayersReady()) return;
                game.start();
            } case "update_symbols" -> {
                if(!Objects.equals(game.owner(), session.getAttributes().get("token").toString())) return;

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
                int row = (Integer) data.get("row");
                int col = (Integer) data.get("col");

                game.move(row, col);
            } default -> {
                for(WebSocketSession player : game.players().keySet()) {
                    player.sendMessage(message);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
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
}
