package com.example.maegi_machugi.guild.api;

import com.example.maegi_machugi.guild.dto.characterDTO;
import com.example.maegi_machugi.guild.dto.guildResponse;
import com.example.maegi_machugi.guild.service.machugiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/guild")
@CrossOrigin(origins = "http://localhost:5173")
public class machugiController {

    @Autowired
    private machugiService machugiService;

    @GetMapping("/game")
    public Flux<characterDTO> getMemberList(
            @RequestParam(name = "guild_name") String guild_name,
            @RequestParam(name = "world_name") String world_name,
            @RequestParam(name = "numOfCharacter") int numOfCharacter) {
        return machugiService.getGuildCharacterList(guild_name, world_name, numOfCharacter);
    }

    @GetMapping("/test")
    public Mono<List<String>> getMember(
            @RequestParam(name = "oguild_id") String oguild_id) {
        return machugiService.getGuildMemberList(oguild_id).collectList();
    }
}