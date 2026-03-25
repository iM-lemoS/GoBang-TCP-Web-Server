package WEB;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import ponte_TCP_WEB.AbstractClientManager;
import ponte_TCP_WEB.IClientHandler;
import ponte_TCP_WEB.MessageProcessor;
import java.io.IOException;
import TCP.Server;

@ServerEndpoint("/game")
public class ClientManagerWEB extends AbstractClientManager implements IClientHandler {
	private Session session;

// Precisamos de uma forma de aceder ao servidor central.
// Uma maneira simples é usar uma referência estática.
	private static Server serverInstance;

	public static void setServer(Server server) {
		serverInstance = server;
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
		System.out.println("Nova conexão WebSocket: " + session.getId());
		// Este cliente está agora ligado, mas ainda não autenticado.
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		System.out.println("Mensagem WebSocket recebida de " + (playerName != null ? playerName : session.getId())
				+ ": " + message);
		// A lógica para processar a mensagem é a mesma do ClientManagerTCP
		// Vamos refatorar isso para um local comum (MessageProcessor).
		MessageProcessor.process(this, message, serverInstance);
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Conexão WebSocket fechada: " + this.playerName);
		if (playerName != null) {
			serverInstance.getLoggedPlayers().remove(playerName);
			if (currentGame != null) {
				currentGame.relayMessage(this, "QUIT");
			}
		}
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		System.err.println("Erro na conexão WebSocket de " + (playerName != null ? playerName : session.getId()));
		throwable.printStackTrace();
	}

	@Override
	public void sendMessage(String message) {
		try {
			if (session != null && session.isOpen()) {
				this.session.getBasicRemote().sendText(message);
			}
		} catch (IOException e) {
			System.err.println("Erro ao enviar mensagem WebSocket para " + playerName);
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Não aplicável para WebSockets, pois a gestão é por eventos, não por uma
		// thread contínua.
		// Deixar vazio ou lançar uma UnsupportedOperationException.
	}

	@Override
	public void closeConnection() {
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}