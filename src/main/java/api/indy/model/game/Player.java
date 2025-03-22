package api.indy.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.socket.WebSocketSession;

public class Player {
    private final WebSocketSession session;
    private final int id;
    private final String name;
    private String symbol;
    private int value;
    private boolean ready;

    public Player(WebSocketSession session, String name, int id) {
        this.id = id;
        this.session = session;
        this.name = name;
        this.ready = false;
    }

    @JsonIgnore
    public WebSocketSession session() {
        return this.session;
    }

    @JsonProperty("id")
    public int id() {
        return this.id;
    }

    @JsonProperty("name")
    public String name() {
        return this.name;
    }

    @JsonProperty("ready")
    public boolean ready() {
        return this.ready;
    }

    @JsonProperty("symbol")
    public String symbol() {
        return this.symbol;
    }

    @JsonProperty("value")
    public int value() {
        return this.value;
    }

    @JsonIgnore
    public void setSymbol(String symbol) {
        this.symbol = symbol;
        this.value = symbol.equalsIgnoreCase("x") ? 1 : -1;
    }

    @JsonIgnore
    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
