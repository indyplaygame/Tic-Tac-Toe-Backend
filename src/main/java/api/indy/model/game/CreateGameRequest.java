package api.indy.model.game;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateGameRequest {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("starting_player")
    private final String starting_player;

    @JsonProperty("visibility")
    private final String visibility;

    @JsonProperty("password")
    private final String password;

    public CreateGameRequest(String name, String starting_player, String visibility, String password) {
        this.name = name;
        this.starting_player = starting_player;
        this.visibility = visibility;
        this.password = password;
    }

    public String name() {
        return this.name;
    }

    public String starting_player() {
        return this.starting_player;
    }

    public String visibility() {
        return this.visibility;
    }

    public String password() {
        return this.password;
    }
}
