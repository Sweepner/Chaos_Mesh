package pl.rodzon.application_context;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

public class ChatContext {
    static private String USER_ID;
    static private String USERNAME;

    public static String getUserId() {
        return USER_ID;
    }

    public static void setUserId(String userId) {
        USER_ID = userId;
    }

    public static String getUsername() {
        return USERNAME;
    }

    public static void setUsername(String USERNAME) {
        ChatContext.USERNAME = USERNAME;
    }

    public static UUID getOptionalUserId() {
        if(USER_ID != null) {
            return UUID.fromString(USER_ID);
        } else return null;
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getResponse();
        } else {
            return null;
        }
    }
}
