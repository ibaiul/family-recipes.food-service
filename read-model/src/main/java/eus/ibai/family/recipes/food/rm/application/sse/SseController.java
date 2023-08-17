package eus.ibai.family.recipes.food.rm.application.sse;

import eus.ibai.family.recipes.food.event.DomainEvent;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@AllArgsConstructor
public class SseController {

    private final SseEventPublisher sseEventPublisher;


    @ApiResponse(responseCode = "200", description = "Stream domain events happening after the request", content = {@Content(mediaType = "text/event-stream", examples = {
            @ExampleObject(value = "[{}]")})})
    @ApiResponse(responseCode = "401", description = "Authentication failed")
    @GetMapping("/events/sse")
    public ResponseEntity<Flux<ServerSentEvent<DomainEvent<String>>>> streamEvents() {
        return ResponseEntity.ok()
                .header("X-Accel-Buffering", "no")
                .header("Connection", "keep-alive")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noCache())
                .body(sseEventPublisher.createSubscription());
    }
}
