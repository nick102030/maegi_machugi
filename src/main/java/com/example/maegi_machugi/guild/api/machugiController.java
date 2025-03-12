package com.example.maegi_machugi.guild.api;

import com.example.maegi_machugi.guild.dto.characterDTO;
import com.example.maegi_machugi.guild.service.machugiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/guild")
public class machugiController {

    @Autowired
    private machugiService machugiService;

    @GetMapping("/game")
    public List<characterDTO> getMemberList(
            @RequestParam(name = "guild_name") String guild_name,
            @RequestParam(name = "world_name") String world_name) {
        return machugiService.getGuildCharacterList(guild_name, world_name);
    }
}