package pl.rodzon.endpoints.chat.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.rodzon.endpoints.chat.model.*;
import pl.rodzon.endpoints.chat.repository.ChatRepository;
import pl.rodzon.endpoints.chat.repository.ChatSpec;
import pl.rodzon.exceptions.model.ChatProblemMessage;
import pl.rodzon.exceptions.problems.ChatProblem;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ModelMapper mapper;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public MessageDTO sendMessage(MessageRequest messageRequest) {
        MessageDAO messageDAO = new MessageDAO();
        messageDAO.setId(UUID.randomUUID());
        messageDAO.setText(messageRequest.getText());
        messageDAO.setImage(null);
        messageDAO.setMessageTime(LocalDateTime.parse(messageRequest.getTime(), dateTimeFormatter));
        messageDAO.setRoomID(messageRequest.getRoomID());
        messageDAO.setPublicKey(messageRequest.getPublicKey());
        this.chatRepository.save(messageDAO);
        return this.mapper.map(messageDAO, MessageDTO.class);
        /*int numberOfMessage = this.getNumberOfUserMassage(messageRequest.getRoomID(), messageRequest.getPublicKey());
        if(numberOfMessage < 5) {
            this.chatRepository.save(messageDAO);
        } else {
            this.rollMessage(messageRequest.getRoomID(), messageRequest.getPublicKey());
            this.chatRepository.save(messageDAO);
        }*/
    }

    public Object loadMessage(Pageable pageable, Boolean isPageableEnabled, String roomID, String publicKey) {
        ChatSpec spec = new ChatSpec(roomID, publicKey);
        if (isPageableEnabled) {
            log.info("Getting messages pageable from roomId: {}", roomID);
            return this.chatRepository.findAll(spec.getSpecification(), pageable).map(x -> mapper.map(x, MessageDTO.class));
        } else {
            log.info("Getting messages as list from roomId: {}", roomID);
            return MessagesDTO.builder()
                    .messages(this.chatRepository.findAll(spec.getSpecification()).stream().map(x -> mapper.map(x, MessageDTO.class)).toList())
                    .build();
        }
    }

    public UsersWithMessagesDTO getAllUsersFromRoomIds(UUID userId) {
        List<String> roomIDs = this.findAllRoomIDs(userId);
        Set<String> users = new HashSet<>();

        for(String roomID : roomIDs) {
            String userReceiver = roomID.substring(36);
            if (userReceiver.equals(userId.toString())) {
                userReceiver = roomID.substring(0, 36);
            }
            users.add(userReceiver);
        }

        return UsersWithMessagesDTO.builder()
                .usersIDs(users.stream().toList())
                .build();
    }

    public MessageDTO getLatestImageMessage(String roomID, String publicKey) {
        return this.mapper.map(this.chatRepository.findFirstByRoomIDAndPublicKeyAndImageNotNullOrderByMessageTimeDesc(
                roomID, publicKey).orElseThrow(() -> new ChatProblem(new ChatProblemMessage("ex.chat.404.no.image"))), MessageDTO.class);
    }

    public MessageDTO sendImageMessage(CreateImageMessageRequest request) {
        try {
            MessageDAO messageDAO = new MessageDAO();
            messageDAO.setId(UUID.randomUUID());
            messageDAO.setText(null);
            messageDAO.setImage(request.getImage().getBytes());
            messageDAO.setMessageTime(LocalDateTime.parse(request.getTime(), dateTimeFormatter));
            messageDAO.setRoomID(request.getRoomID());
            messageDAO.setPublicKey(request.getPublicKey());
            this.chatRepository.save(messageDAO);
            return this.mapper.map(messageDAO, MessageDTO.class);
        } catch (IOException e) {
            throw new ChatProblem(new ChatProblemMessage("ex.chat.422.cannot.save.picture"));
        }
    }

    public Void deleteMessage(UUID messageId) {
        this.chatRepository.deleteById(messageId);
        return null;
    }

    public Void deleteMessageByCreationTime(String time) {
        LocalDateTime localDateTime = LocalDateTime.parse(time, dateTimeFormatter);
        this.chatRepository.deleteAllByMessageTime(localDateTime);
        return null;
    }

    public String getReverseRoomID(String roomID) {
        String userReceiver = roomID.substring(36);
        String userSender = roomID.substring(0, 36);
        return userReceiver + userSender;
    }

    private List<String> findAllRoomIDs(UUID userId) {
        return this.chatRepository.findAllRoomID(userId);
    }

    private int getNumberOfUserMassage(String roomID, String publicKey) {
        Optional<List<MessageDAO>> userMassage = this.chatRepository.findAllByRoomIDAndPublicKey(roomID, publicKey);
        return userMassage.map(List::size).orElse(-1);
    }

    private void rollMessage(String roomID, String publicKey) {
        Optional<List<MessageDAO>> userMassage = this.chatRepository.findAllByRoomIDAndPublicKey(roomID, publicKey);
        if(userMassage.isPresent()) {
            this.sortByDate(userMassage.get());
            this.chatRepository.delete(userMassage.get().get(0));
        }
    }

    private void sortByDate(List<MessageDAO> messageDAOList) {
        messageDAOList.sort(Comparator.comparing(MessageDAO::getMessageTime));
    }
}
