package com.example.maegi_machugi.guild.service;

import com.example.maegi_machugi.guild.dto.characterDTO;
import com.example.maegi_machugi.guild.dto.guildResponse;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
public class machugiService {
    private static final String API_KEY = System.getenv("API_KEY");
    private static WebClient webClient;

    @Autowired
    public machugiService(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.baseUrl("https://open.api.nexon.com").build();
    }

    //길드명을 통한 oguild_id 받아오기
    public String getGuildId(String guild_name, String world_name) {
        guildResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("maplestory/v1/guild/id")
                        .queryParam("guild_name", guild_name)
                        .queryParam("world_name", world_name)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .retrieve()
                .bodyToMono(guildResponse.class)
                .block();
        return Objects.requireNonNull(response).getOguild_id();
    }

    //oguild_id를 통한 길드원 목록 리스트 받아오기
    public List<String> getGuildMemberList(String oguild_id) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/guild/basic")
                        .queryParam("oguild_id", oguild_id)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("guild_member"))
            return (List<String>) response.get("guild_member");
        return List.of();
    }

    //캐릭터명을 통한 캐릭터 정보 받아오기
    public characterDTO getUserINFO(String character_name) {
        characterDTO response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/id")
                        .queryParam("character_name", character_name)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .retrieve()
                .bodyToMono(characterDTO.class)
                .block();

        if (response != null) {
            System.out.println("유저 정보 불러오기 성공");
            return getCharacterImage(response.getOcid(), character_name);
        }

        return null;
    }

    //ocid 통한 캐릭터 이미지 url 조회 및 DTO 생성
    public characterDTO getCharacterImage(String ocid, String character_name) {
        var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/character/basic")
                        .queryParam("ocid", ocid)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("character_image")) {
            String imageURL = (String) response.get("character_image");
            characterDTO characterInfo = new characterDTO();
            characterInfo.setOcid(ocid);
            characterInfo.setCharacterName(character_name);
            characterInfo.setImageURL(imageURL);
            System.out.println("DTO 생성 성공");
            return characterInfo;
        }

        return null;
    }

    public List<characterDTO> getGuildCharacterList(String guild_name, String world_name) {
        String oguild_id = getGuildId(guild_name, world_name);
        List<String> guildMemberList = getGuildMemberList(oguild_id);
        List<characterDTO> characterDTOList = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            try{
                Thread.sleep(1000);
                characterDTOList.add(getUserINFO(guildMemberList.get(i)));
                if (i % 3 == 0) Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return characterDTOList;
    }
}