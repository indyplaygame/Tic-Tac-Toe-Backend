package api.indy.config;

import api.indy.service.AuthService;
import api.indy.service.GameService;
import api.indy.websocket.GameInterceptor;
import api.indy.websocket.GameSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GameSocketHandler gameWebSocketHandler;
    private final AuthService authService;
    private final GameService gameService;

    @Autowired
    public WebSocketConfig(GameSocketHandler gameWebSocketHandler, AuthService authService, GameService gameService) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.authService = authService;
        this.gameService = gameService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/game/join/{gameId}")
                .setAllowedOrigins("*")
                .addInterceptors(new GameInterceptor(this.authService, this.gameService));
    }
}
