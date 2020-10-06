package scau.godelgnis.realtimebackend.comment;

import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ServerEndpoint("/websocket")
public class WebSocketServer {
    private Session session;
    private static List<Session> sessions = new CopyOnWriteArrayList<>();

    @OnOpen
    public void onOpen(Session session) throws IOException{
        this.session = session;
        sessions.add(session);
        session.getBasicRemote().sendText("欢迎使用websocket连接服务器。。。。");
    }

    @OnClose
    public void onClose(){
        sessions.remove(session);
    }
    @OnError
    public void onError(Session session, Throwable error){
        error.printStackTrace();
    }

    public static synchronized void snedToAll(String message) throws IOException {
        for (Session session : sessions) {
            session.getBasicRemote().sendText(message);
        }
    }
}
