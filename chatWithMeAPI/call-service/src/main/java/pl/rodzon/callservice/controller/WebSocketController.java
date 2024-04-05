package pl.rodzon.callservice.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import pl.rodzon.callservice.model.AudioData;
import pl.rodzon.callservice.model.CallSignal;
import pl.rodzon.callservice.model.CallingInfo;

import java.util.Arrays;


@Slf4j
@RestController
@AllArgsConstructor
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/call/sendAudio/{to}")
    public void handleAudio(@DestinationVariable(value = "to") String to, AudioData audioData) {
        // Obsługa przesyłanych danych audio
        System.out.println("Audio data received");
        System.out.println("Sending audio data to: " + to);
        System.out.println(Arrays.toString(audioData.getBase64Data()));
        messagingTemplate.convertAndSend("/topic/receiveAudio/" + to, audioData);
    }

    @MessageMapping("/call/audio/{to}")
    public void sendInfoThatUserIsCallingAudio(@DestinationVariable(value = "to") String to, CallingInfo info) {
        log.info("Handling sending info: {} about call audio: to: {}", info.getCallingInformation(), to);
        messagingTemplate.convertAndSend("/topic/calling/audio/" + to, info);
    }

    @MessageMapping("/call/p2p/signal/{to}")
    public void sendCallP2PSignal(@DestinationVariable(value = "to") String to, CallSignal callSignal) {
        log.info("Handling sending call signal with description: {} about call audio: to: {}", callSignal.getSessionDescription(), to);
        messagingTemplate.convertAndSend("/topic/p2p/signal/" + to, callSignal);
    }
}