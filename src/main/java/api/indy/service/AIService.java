package api.indy.service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.apache.http.HttpException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
public class AIService {
    private final Client client;
    private final String[] instructions;

    public AIService(@Value("${ai.apiKey}") String apiKey) {
        this.client = Client.builder().apiKey(apiKey).build();
        this.instructions = new String[]{
                "You are a Tic Tac Toe AI that plays optimally. You are given a 3x3 Tic Tac Toe board represented as a 2D array with values: %d for 'X', %d for 'O'," +
                        "and 0 for an empty space. Your task is to analyze the current state of the board and return the best move for 'X', represented as the row and column index of the" +
                        "best move. If there are multiple optimal moves, choose the one with the smallest row index, and if the row indices are the same, choose the smallest column index." +
                        "The board is represented as follows: \\n",
                "For example, for the board \\n[[1, 0, -1], \\n[0, -1, 1], \\n[1, 0, 0]], \\n the best move would be represented as {'row': 2, 'col': 2}."
        };
    }

    public String getMove(int[][] board, int value) throws HttpException, IOException {
        Content systemInstructions = Content.builder().parts(ImmutableList.of(
                Part.builder().text(instructions[0].formatted(value, -value)).build(),
                Part.builder().text(instructions[1]).build()
        )).build();

        Map<String, Schema> properties = Map.of(
                "row", Schema.builder().type("integer").build(),
                "col", Schema.builder().type("integer").build()
        );

        Schema schema = Schema.builder()
                .type("object")
                .properties(properties)
                .required(Arrays.asList("row", "col")).build();

        GenerateContentConfig generationConfig = GenerateContentConfig.builder()
                .systemInstruction(systemInstructions)
                .temperature(.2F)
                .responseMimeType("application/json")
                .responseSchema(schema).build();

        GenerateContentResponse response = client.models.generateContent("gemini-2.0-flash-lite", Arrays.deepToString(board), generationConfig);

        return response.text();
    }
}
