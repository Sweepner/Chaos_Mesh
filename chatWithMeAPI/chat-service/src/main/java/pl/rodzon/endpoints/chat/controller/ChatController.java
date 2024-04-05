package pl.rodzon.endpoints.chat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import pl.rodzon.endpoints.chat.model.*;
import pl.rodzon.endpoints.chat.service.ChatService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@ParameterObject MessageRequest messageRequest) {
        return new ResponseEntity<>(this.chatService.sendMessage(messageRequest), HttpStatus.CREATED);
    }

    @GetMapping("/load")
    public ResponseEntity<Object> loadMessage(@Parameter(hidden = true) Pageable pageable, Boolean isPageableEnabled, String roomID, String publicKey) {
        return new ResponseEntity<>(this.chatService.loadMessage(pageable, isPageableEnabled, roomID, publicKey), HttpStatus.OK);
    }

    @PostMapping(path = "/image/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDTO> sendImageMessage(@Parameter(content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)) CreateImageMessageRequest request) {
        MessageDTO messageDTO = this.chatService.sendImageMessage(request);
        if (request.getUsername() != null) {
            log.info("Handling information about send image: to: {}", request.getUsername());
            messagingTemplate.convertAndSend("/topic/messages/image/" + request.getUsername(), request.getSender());
            return new ResponseEntity<>(new MessageDTO(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(messageDTO, HttpStatus.CREATED);
        }
    }

    @GetMapping("/image/latest")
    public ResponseEntity<MessageDTO> getLatestImageMessage(@RequestParam(value = "roomID") String roomID, @RequestParam(value = "publicKey") String publicKey) {
        return new ResponseEntity<>(this.chatService.getLatestImageMessage(roomID, publicKey), HttpStatus.OK);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UsersWithMessagesDTO> getAllUsersFromRoomIds(@PathVariable(value = "userId") String userId) {
        return new ResponseEntity<>(this.chatService.getAllUsersFromRoomIds(UUID.fromString(userId)), HttpStatus.OK);
    }

    @MessageMapping("/chat/{to}")
    public void sendRealTimeMessage(@DestinationVariable(value = "to") String to, MessageRequest messageRequest) {
        log.info("Handling send message: {} to: {}", messageRequest, to);
        MessageDTO messageDTO = this.chatService.sendMessage(messageRequest);
        messageDTO.setUsername(messageRequest.getUsername());
        messagingTemplate.convertAndSend("/topic/messages/" + to, messageDTO);
    }

    @MessageMapping("/chat/image/{to}") // fronted are not use this (replace is in the 44 line)
    public void sendRealTimeImageMessage(@DestinationVariable(value = "to") String to) {
        log.info("Handling information about send image: to: {}", to);
        messagingTemplate.convertAndSend("/topic/messages/image/" + to, "");
    }

    @MessageMapping("/chat/writing/{to}")
    public void sendInfoThatUserIsWriting(@DestinationVariable(value = "to") String to, IsWriting info) {
        log.info("Handling writing info: to: {}",to);
        messagingTemplate.convertAndSend("/topic/writing/" + to, info);
    }

    @DeleteMapping("/message/self/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable(value = "id") String id) {
        log.info("Deleting message with id {}", id);
        return new ResponseEntity<>(this.chatService.deleteMessage(UUID.fromString(id)), HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/message/{time}")
    public ResponseEntity<Void> deleteMessageByCreationTime(@PathVariable(value = "time") String time, String username) {
        log.info("Deleting messages created at time:  {}", time);
        messagingTemplate.convertAndSend("/topic/messages/delete/" + username, time);
        return new ResponseEntity<>(this.chatService.deleteMessageByCreationTime(time), HttpStatus.NO_CONTENT);
    }
}
