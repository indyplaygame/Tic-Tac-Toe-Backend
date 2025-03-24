package api.indy.websocket;

import org.springframework.web.socket.CloseStatus;

public class WebSocketStatus {
    private enum Status {
        BAD_REQUEST(4400, "Bad Request"),
        UNAUTHORIZED(4401, "Unauthorized"),
        FORBIDDEN(4403, "Forbidden"),
        NOT_FOUND(4404, "Not Found"),
        INTERNAL_SERVER_ERROR(4500, "Internal Server Error");

        private final CloseStatus closeStatus;

        Status(int code, String reason) {
            this.closeStatus = new CloseStatus(code, reason);
        }

        public CloseStatus status() {
            return this.closeStatus;
        }
    }

    public static final CloseStatus BAD_REQUEST = Status.BAD_REQUEST.status();
    public static final CloseStatus UNAUTHORIZED = Status.UNAUTHORIZED.status();
    public static final CloseStatus FORBIDDEN = Status.FORBIDDEN.status();
    public static final CloseStatus NOT_FOUND = Status.NOT_FOUND.status();
    public static final CloseStatus INTERNAL_SERVER_ERROR = Status.INTERNAL_SERVER_ERROR.status();
}