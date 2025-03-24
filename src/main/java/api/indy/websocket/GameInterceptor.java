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
        final String path = request.getURI().getPath();
        attributes.put("path", path);
        attributes.put("query", request.getURI().getQuery());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {}
}
