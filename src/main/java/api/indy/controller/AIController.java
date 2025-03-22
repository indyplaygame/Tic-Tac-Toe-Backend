package api.indy.controller;

import api.indy.model.ai.AIRequest;
import api.indy.service.AIService;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AIController {
    private final AIService aiService;

    @Autowired
    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/get-move")
    public ResponseEntity<String> getMove(@RequestBody AIRequest body) throws HttpException, IOException {
        return new ResponseEntity<>(aiService.getMove(body.board(), body.value()), HttpStatus.OK);
    }
}
