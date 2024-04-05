package pl.rodzon.chatwithme.utils;

import io.reactivex.disposables.CompositeDisposable;
import ua.naiksoftware.stomp.StompClient;

public class ChatContext {
    /*private static final String serverAuthorizationURL = "https://chat-authorization.loca.lt/api/";
    private static final String serverMessageURL = "https://chat-chat.loca.lt/api/";
    private static final String websocketServerMessageURL = "wss://chat-chat.loca.lt/chat";
    private static final String websocketServerCallURL = "wss://chat-call.loca.lt/call";*/
    private static final String serverAuthorizationURL = "http://fedora:8081/api/";
    private static final String serverMessageURL = "http://fedora:8082/api/";
    private static final String websocketServerMessageURL = "ws://fedora:8082/chat";
    private static final String websocketServerCallURL = "ws://fedora:8083/call";
    private static String publicKey;
    private static String privateKey;
    private static String viewFullImage;
    private static String receiverUsername;
    private static boolean isMicrophoneMuted;
    private static StompClient stompClientCall;
    private static StompClient stompClientChat;
    public static CompositeDisposable compositeDisposable = new CompositeDisposable();
    private static String tempUsername;
    private static String tempPassword;
    private static Boolean isWrongPrivateKey = false;

    public static String getPublicKey() {
        return publicKey;
    }

    public static void setPublicKey(String publicKey) {
        ChatContext.publicKey = publicKey;
    }

    public static String getPrivateKey() {
        return privateKey;
    }

    public static void setPrivateKey(String privateKey) {
        ChatContext.privateKey = privateKey;
    }

    public static String getServerAuthorizationURL() {
        return serverAuthorizationURL;
    }

    public static String getServerMessageURL() {
        return serverMessageURL;
    }
    public static String getWebsocketServerMessageURL() {
        return websocketServerMessageURL;
    }
    public static String getWebsocketServerCallURL() {
        return websocketServerCallURL;
    }

    public static String getViewFullImage() {
        return viewFullImage;
    }

    public static void setViewFullImage(String viewFullImage) {
        ChatContext.viewFullImage = viewFullImage;
    }

    public static String getReceiverUsername() {
        return receiverUsername;
    }

    public static void setReceiverUsername(String receiverUsername) {
        ChatContext.receiverUsername = receiverUsername;
    }

    public static boolean isIsMicrophoneMuted() {
        return isMicrophoneMuted;
    }

    public static void setIsMicrophoneMuted(boolean isMicrophoneMuted) {
        ChatContext.isMicrophoneMuted = isMicrophoneMuted;
    }

    public static StompClient getStompClientCall() {
        return stompClientCall;
    }

    public static void setStompClientCall(StompClient stompClientCall) {
        ChatContext.stompClientCall = stompClientCall;
    }

    public static StompClient getStompClientChat() {
        return stompClientChat;
    }

    public static void setStompClientChat(StompClient stompClientChat) {
        ChatContext.stompClientChat = stompClientChat;
    }

    public static CompositeDisposable getCompositeDisposable() {
        return compositeDisposable;
    }

    public static String getTempUsername() {
        return tempUsername;
    }

    public static void setTempUsername(String tempUsername) {
        ChatContext.tempUsername = tempUsername;
    }

    public static String getTempPassword() {
        return tempPassword;
    }

    public static void setTempPassword(String tempPassword) {
        ChatContext.tempPassword = tempPassword;
    }

    public static Boolean getIsWrongPrivateKey() {
        return isWrongPrivateKey;
    }

    public static void setIsWrongPrivateKey(Boolean isWrongPrivateKey) {
        ChatContext.isWrongPrivateKey = isWrongPrivateKey;
    }
}
