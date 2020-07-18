package systems.dmx.core.service.websocket;

import java.util.function.Predicate;



public interface WebSocketService {

    void sendToOrigin(String message);

    void sendToAll(String message);

    void sendToAllButOrigin(String message);

    void sendToReadAllowed(String message, long objectId);

    void sendToSome(String message, Predicate<WebSocketConnection> connectionFilter);

    // ---

    String getWebSocketURL();
}
