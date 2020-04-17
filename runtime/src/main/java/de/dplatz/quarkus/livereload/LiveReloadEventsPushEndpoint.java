package de.dplatz.quarkus.livereload;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import de.dplatz.quarkus.livereload.watcher.FileModificationEvent;

@ServerEndpoint("/live-reload")
@ApplicationScoped
public class LiveReloadEventsPushEndpoint {

    List<Session> sessions = new CopyOnWriteArrayList<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("-- message from client: " + message);
    }
    
    public void onChangeEvent(@Observes FileModificationEvent event) {
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText("file-changed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}