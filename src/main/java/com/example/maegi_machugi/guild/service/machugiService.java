package com.example.maegi_machugi.guild.service;

import com.example.maegi_machugi.guild.dto.characterDTO;
import com.example.maegi_machugi.guild.dto.guildResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;


@Service
public class machugiService {
    private static final String API_KEY = System.getenv("API_KEY");
    private static WebClient webClient;

    @Autowired
    public machugiService(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.baseUrl("https://open.api.nexon.com").build();
    }

    //길드명을 통한 oguild_id 받아오기
    public Mono<ResponseEntity<guildResponse>> getGuildId(String guild_name, String world_name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("maplestory/v1/guild/id")
                        .queryParam("guild_name", guild_name)
                        .queryParam("world_name", world_name)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .exchangeToMono(clientResponse -> {
                    //System.out.println("STATUS CODE: " + clientResponse.statusCode());
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    //System.out.println("RESPONSE BODY: " + body);
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        guildResponse result = mapper.readValue(body, guildResponse.class);
                                        return Mono.just(ResponseEntity.status(clientResponse.statusCode()).body(result));
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("Guild ID JSON 파싱 실패", e));
                                    }
                                });
                    } else {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    //System.out.println("RESPONSE BODY: " + errorBody);
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "길드명을 정확하게 입력해 주세요."));
                                });
                    }
                })
                .onErrorResume(e ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(null))
                );
    }

    //oguild_id를 통한 길드원 목록 리스트 받아오기
    public Flux<String> getGuildMemberList(String oguild_id) {
        return webClient.get().uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/guild/basic")
                        .queryParam("oguild_id", oguild_id)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .exchangeToMono(response -> {
                    //System.out.println("STATUS CODE: " + response.statusCode());
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    //System.out.println("RESPONSE BODY: " + body);
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        Map<String, Object> map = mapper.readValue(body, Map.class);
                                        return Mono.just(map);
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("JSON 파싱 실패", e));
                                    }
                                });
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    //System.out.println("RESPONSE BODY: " + errorBody);
                                    return Mono.error(new ResponseStatusException(response.statusCode(), "길드원 목록 조회 실패"));
                                });
                    }
                })
                .flatMapMany(map -> {
                    Object rawMembers = map.get("guild_member");
                    if (rawMembers instanceof List<?> rawList) {
                        try {
                            List<String> names = new ArrayList<>();
                            for (Object obj : rawList) {
                                names.add(obj.toString());
                            }
                            return Flux.fromIterable(names);
                        } catch (Exception e) {
                            return Flux.error(new RuntimeException("길드원 이름 추출 실패", e));
                        }
                    } else {
                        return Flux.error(new RuntimeException("Unexpected guild_member structure: " + rawMembers));
                    }
                });
    }

    //캐릭터명을 통한 캐릭터 정보 받아오기
    public Mono<characterDTO> getUserINFO(String character_name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/id")
                        .queryParam("character_name", character_name)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .exchangeToMono(clientResponse -> {
                    return clientResponse.bodyToMono(String.class)
                        .doOnNext(body -> {
                            //System.out.println("STATUS CODE: " + clientResponse.statusCode());
                            //System.out.println("RESPONSE BODY: " + body);
                        })
                        .flatMap(body -> {
                            if (clientResponse.statusCode().is2xxSuccessful()) {
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    characterDTO dto = mapper.readValue(body, characterDTO.class);
                                    return Mono.just(dto);
                                } catch (Exception e) {
                                    return Mono.error(new RuntimeException("User info JSON 파싱 실패", e));
                                }
                            } else {
                                return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "캐릭터 정보 조회 실패"));
                            }
                        });
                })
                .flatMap(dto -> getCharacterImage(dto.getOcid(), character_name));
    }

    //ocid 통한 캐릭터 이미지 url 조회 및 DTO 생성
    public Mono<characterDTO> getCharacterImage(String ocid, String character_name) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maplestory/v1/character/basic")
                        .queryParam("ocid", ocid)
                        .build())
                .header("x-nxopen-api-key", API_KEY)
                .exchangeToMono(clientResponse -> {
                    //System.out.println("STATUS CODE: " + clientResponse.statusCode());
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    //System.out.println("RESPONSE BODY: " + body);
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        Map<String, Object> responseMap = mapper.readValue(body, Map.class);
                                        String imageURL = (String) responseMap.get("character_image");

                                        characterDTO dto = new characterDTO();
                                        dto.setOcid(ocid);
                                        dto.setCharacterName(character_name);
                                        dto.setImageURL(imageURL);

                                        return Mono.just(dto);
                                    } catch (Exception e) {
                                        return Mono.error(new RuntimeException("JSON 파싱 오류", e));
                                    }
                                });
                    } else {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    //System.out.println("RESPONSE BODY: " + errorBody);
                                    return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "캐릭터 이미지 조회 실패"));
                                });
                    }
                });
    }

    public Flux<characterDTO> getGuildCharacterList(String guild_name, String world_name, int numOfCharacter) {
        return getGuildId(guild_name, world_name)
                .flatMapMany(response -> {
                    if (response.getBody() == null || response.getBody().getOguild_id() == null) {
                        return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "길드명, 월드명을 정확히 입력해 주세요"));
                    }
                    return getGuildMemberList(response.getBody().getOguild_id());
                })
                .collectList()
                .flatMapMany(memberList -> {
                    Collections.shuffle(memberList);
                    List<String> selectedMembers = memberList.subList(0, Math.min(numOfCharacter, memberList.size()));
                    return Flux.fromIterable(selectedMembers)
                            .delayElements(Duration.ofMillis(500))
                            .flatMap(this::getUserINFO);
                });
    }
}