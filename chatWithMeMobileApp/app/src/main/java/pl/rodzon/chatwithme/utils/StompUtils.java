package pl.rodzon.chatwithme.utils;

import android.util.Log;


import ua.naiksoftware.stomp.StompClient;


public class StompUtils {
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CheckResult"})
    public static void lifecycle(StompClient stompClient) {
        stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.d("OPENED", "Stomp connection opened");
                    break;

                case ERROR:
                    Log.e("ERROR", "Error", lifecycleEvent.getException());
                    break;

                case CLOSED:
                    Log.d("CLOSED", "Stomp connection closed");
                    break;
            }
        });
    }
}
