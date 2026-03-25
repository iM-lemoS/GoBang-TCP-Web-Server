package ponte_TCP_WEB;

import java.util.HashMap;
import TCP.GameSession;

public abstract class AbstractClientManager implements IClientHandler {
	protected String playerName;
	protected String password;
	protected String nationality;
	protected int age;
	protected String photo;
	protected String gender;
	protected String passwordGameAccess;
	protected GameSession currentGame;
	protected IClientHandler foundClient;

// --- MÉTODOS ABSTRATOS ---
// Estes métodos TÊM de ser implementados pelas classes filhas,
// pois a sua lógica depende do protocolo (TCP ou WebSocket).
	@Override
	public abstract void sendMessage(String message);

	@Override
	public abstract void closeConnection();

// --- MÉTODOS CONCRETOS (COMUNS) ---
// Esta lógica é idêntica para ambos os handlers, por isso fica aqui.

	@Override
	public String getPlayerName() {
		return this.playerName;
	}

	@Override
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	@Override
	public String getPlayerPassword() {
		return this.password;
	}

	@Override
	public void setPlayerPassword(String password) {
		this.password = password;
	}

	@Override
	public String getPlayerNationality() {
		return this.nationality;
	}

	@Override
	public void setPlayerNationality(String nationality) {
		this.nationality = nationality;
	}

	@Override
	public int getPlayerAge() {
		return this.age;
	}

	@Override
	public void setPlayerAge(int age) {
		this.age = age;
	}

	@Override
	public String getPlayerPhoto() {
		return this.photo;
	}

	@Override
	public void setPlayerPhoto(String photo) {
		this.photo = photo;
	}

	@Override
	public String getPlayerGender() {
		return this.gender;
	}

	@Override
	public void setPlayerGender(String gender) {
		this.gender = gender;
	}

	@Override
	public String getPlayerPasswordGameAccess() {
		return this.passwordGameAccess;
	}

	@Override
	public void setPlayerPasswordGameAccess(String passwordGameAccess) {
		this.passwordGameAccess = passwordGameAccess;
	}

	@Override
	public GameSession getCurrentGame() {
		return this.currentGame;
	}

	@Override
	public void setCurrentGame(GameSession game) {
		this.currentGame = game;
	}

	@Override
	public IClientHandler getPlayerFoundClient() {
		return this.foundClient;
	}

	@Override
	public void setPlayerFoundClient(IClientHandler foundClient) {
		this.foundClient = foundClient;
	}

	@Override
	public IClientHandler clientManagerByPlayerName(String gameName, HashMap<IClientHandler, String> waitingGames) {
		for (IClientHandler client : waitingGames.keySet()) {
			if (client.getPlayerName().equals(gameName)) {
				return client;
			}
		}
		return null;
	}

// O método run() do TCP é específico e o do WebSocket é vazio.
// Podemos dar uma implementação vazia aqui que pode ser sobrescrita.
	@Override
	public void run() {
		// Implementação padrão vazia. O ClientManagerTCP vai sobrescrever isto.
	}

}