package api.indy.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class Game {
    public enum Visibility {
        PUBLIC,
        UNLISTED,
        PRIVATE
    }

    public enum WinState {
        NONE,
        WIN,
        DRAW,
        LOSS
    }

    private static final int SIZE = 3;

    private final List<Integer> idPool = new ArrayList<>();
    private final ObjectMapper serializer = new ObjectMapper();

    private final UUID uuid;
    private final String name;
    private final String owner;
    private final String startingPlayer;
    private final Visibility visibility;
    private final String joinCode;
    private final String password;
    private final Map<WebSocketSession, Player> players;

    private boolean started;
    private Queue<Player> turns;
    private List<List<Integer>> board;

    public Game(UUID uuid, String ownerId, String name, String starting_player, String visibility, String joinCode, String password) {
        this.uuid = uuid;
        this.owner = ownerId;
        this.name = name;
        this.startingPlayer = starting_player.toUpperCase();
        this.visibility = Visibility.valueOf(visibility.toUpperCase());
        this.joinCode = joinCode;
        this.password = password;
        this.players = new ConcurrentHashMap<>();
        this.started = false;
    }

    public boolean join(WebSocketSession session) {
        if(this.players.size() == 2) return false;
        int id = this.idPool.isEmpty() ? this.players.size() : this.idPool.remove(0);

        this.players.put(session, new Player(session, "Player %d".formatted(id + 1), id));

        return this.players.containsKey(session);
    }

    public void leave(WebSocketSession session) {
        this.idPool.add(this.players.get(session).id());
        this.players.remove(session);
    }

    public void start() throws IOException {
        this.started = true;

        this.board = new ArrayList<>();
        for(int i = 0; i < SIZE; i++) {
            this.board.add(new ArrayList<>());
            for(int j = 0; j < SIZE; j++) {
                this.board.get(i).add(0);
            }
        }

        this.turns = new LinkedList<>();
        if(this.startingPlayer.equalsIgnoreCase("RANDOM")) {
            List<Player> playerList = new ArrayList<>(this.players.values());
            Collections.shuffle(playerList);
            this.turns.addAll(playerList);
        } else {
            Player startingPlayer = this.players.values().stream().filter(player -> player.name().equalsIgnoreCase(this.startingPlayer)).findFirst().orElse(null);
            if(startingPlayer != null) {
                this.turns.add(startingPlayer);
                this.players.values().stream().filter(player -> !player.equals(startingPlayer)).forEach(this.turns::add);
            }
        }
        if(this.turns.isEmpty()) this.turns.addAll(this.players.values());

        for(Player player : this.players.values()) {
            player.session().sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                "type", "game_start",
                "player_turn", this.turns.peek().id(),
                "players", this.players.values()
            ))));

            player.setReady(false);
        }

        this.turn();
    }

    public void turn() throws IOException {
        if(!this.started) return;

        Player player = this.turns.peek();
        if(player == null) return;

        player.session().sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
            "type", "game_turn"
        ))));

        this.players.keySet().forEach(p -> {
            try {
                p.sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                    "type", "player_turn",
                    "symbol", player.symbol()
                ))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void move(int row, int col) throws IOException {
        if(!this.started) return;
        if(this.board.get(row).get(col) != 0) return;

        Player player = this.turns.poll();
        if(player == null) return;

        this.board.get(row).set(col, player.value());

        this.players.values().forEach(p -> {
            try {
                p.session().sendMessage(new TextMessage(serializer.writeValueAsString(Map.of(
                    "type", "player_move",
                    "move", Map.of(
                        "row", row,
                        "col", col,
                        "symbol", player.symbol(),
                        "value", player.value()
                    )
                ))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        WinState winState = this.checkWin(player, row, col);
        if(winState.equals(WinState.WIN)) this.endGame(winState, player);
        else if(winState.equals(WinState.DRAW)) this.endGame(winState, null);

        this.turns.add(player);
        this.turn();
    }

    public WinState checkWin(Player player, int row, int col) {
        if(this.board.get(row).stream().allMatch(cell -> cell == player.value())) return WinState.WIN;
        if(this.board.stream().allMatch(r -> r.get(col) == player.value())) return WinState.WIN;
        if(row == col && IntStream.range(0, SIZE).allMatch(i -> this.board.get(i).get(i) == player.value())) return WinState.WIN;
        if(row + col == SIZE - 1 && IntStream.range(0, SIZE).allMatch(i -> this.board.get(i).get(SIZE - i - 1) == player.value())) return WinState.WIN;

        if(this.board.stream().allMatch(r -> r.stream().allMatch(cell -> cell != 0))) return WinState.DRAW;

        return WinState.NONE;
    }

    public void endGame(WinState state, Player winner) throws IOException {
        if(state.equals(WinState.WIN)) {
            Map<String, Object> payload = new HashMap<>(Map.of(
                    "type", "game_end",
                    "state", state.toString(),
                    "winner", winner
            ));

            winner.session().sendMessage(new TextMessage(
                    serializer.writeValueAsString(payload)
            ));

            payload.put("state", WinState.LOSS.toString());

            this.players.values().stream().filter(player -> !player.equals(winner)).forEach(player -> {
                try {
                    player.session().sendMessage(new TextMessage(serializer.writeValueAsString(payload)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            Map<String, Object> payload = new HashMap<>(Map.of(
                    "type", "game_end",
                    "state", state.toString()
            ));

            this.players.values().forEach(player -> {
                try {
                    player.session().sendMessage(new TextMessage(serializer.writeValueAsString(payload)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        this.started = false;
    }

    @JsonProperty("uuid")
    public UUID uuid() {
        return this.uuid;
    }

    @JsonProperty("name")
    public String name() {
        return this.name;
    }

    @JsonProperty("starting_player")
    public String startingPlayer() {
        return this.startingPlayer;
    }

    @JsonProperty("visibility")
    public Visibility visibility() {
        return this.visibility;
    }

    @JsonProperty("join_code")
    public String joinCode() {
        return this.joinCode;
    }

    @JsonProperty("player_count")
    public int playerCount()  {
        return this.players.size();
    }

    @JsonProperty("players")
    public List<Player> playersList() {
        return new ArrayList<>(this.players.values());
    }

    @JsonProperty("started")
    public boolean started() {
        return this.started;
    }

    @JsonIgnore
    public String password() {
        return this.password;
    }

    @JsonIgnore
    public Map<WebSocketSession, Player> players() {
        return this.players;
    }

    @JsonIgnore
    public Player player(WebSocketSession session) {
        return this.players.get(session);
    }

    @JsonIgnore
    public String owner() {
        return this.owner;
    }

    @JsonIgnore
    public Player getPlayerById(int id) {
        return this.players.values().stream().filter(player -> player.id() == id).findFirst().orElse(null);
    }

    @JsonIgnore
    public void playerReady(WebSocketSession session, boolean ready) {
        this.players.get(session).setReady(ready);
    }

    @JsonIgnore
    public boolean allPlayersReady() {
        return this.players.values().stream().allMatch(Player::ready);
    }
}
