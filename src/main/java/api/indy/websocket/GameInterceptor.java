package api.indy.websocket;

import api.indy.model.game.Game;
import api.indy.service.AuthService;
import api.indy.service.GameService;
import api.indy.util.Util;
import com.google.common.net.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameInterceptor implements HandshakeInterceptor {
    private final AuthService authService;
    private final GameService gameService;

    public GameInterceptor(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        System.out.println(request.getHeaders().getFirst(HttpHeaders.COOKIE));

        if(request.getHeaders().getFirst(HttpHeaders.COOKIE) == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        final String[] cookies =  Objects.requireNonNull(request.getHeaders().getFirst(HttpHeaders.COOKIE)).split("; ");
        final String token = Util.getCookie(cookies, "token");
        if(token == null || !authService.verifyToken(UUID.fromString(token))) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        final Pattern pattern = Pattern.compile("/game/join/([^/]+)");
        final String path = request.getURI().getPath();

        Matcher matcher = pattern.matcher(path);
        if(!matcher.find()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }

        final String gameId = matcher.group(1);
        final Game game = this.gameService.getGame(UUID.fromString(gameId));
        if(game == null) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return false;
        }

        if(game.visibility().equals(Game.Visibility.PRIVATE)) {
            final String password = Util.getParam(request.getURI().getQuery(), "pass");

            if(password == null || !authService.verifyPassword(password, game.password())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
        }

        if(game.playerCount() == 2) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        if(game.started()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("token", token);
        attributes.put("gameId", gameId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
}
