package ponte_TCP_WEB;

import java.util.HashMap;
import TCP.GameSession;

public interface IClientHandler {
	/**
	 * Envia uma mensagem para o cliente final.
	 * 
	 * @param message A mensagem a ser enviada.
	 */
	void sendMessage(String message);

	/**
	 * Retorna o nome do jogador associado a este handler.
	 * 
	 * @return O nickname do jogador.
	 */
	String getPlayerName();

	/**
	 * Define o nome do jogador (geralmente após o login/registo).
	 * 
	 * @param playerName O nickname do jogador.
	 */
	void setPlayerName(String playerName);

	/**
	 * Retorna a password do jogador associado a este handler.
	 * 
	 * @return A password do jogador.
	 */
	String getPlayerPassword();

	/**
	 * Define a password do jogador (geralmente após o login/registo).
	 * 
	 * @param password A password do jogador.
	 */
	void setPlayerPassword(String password);

	/**
	 * Retorna a nacionalidade do jogador associado a este handler.
	 * 
	 * @return A nacionalidade do jogador.
	 */
	String getPlayerNationality();

	/**
	 * Define a nacionalidade do jogador (geralmente após o login/registo).
	 * 
	 * @param nationality A nacionalidade do jogador.
	 */
	void setPlayerNationality(String nationality);

	/**
	 * Retorna a idade do jogador associado a este handler.
	 * 
	 * @return A idade do jogador.
	 */
	int getPlayerAge();

	/**
	 * Define a idade do jogador (geralmente após o login/registo).
	 * 
	 * @param age A idade do jogador.
	 */
	void setPlayerAge(int age);

	/**
	 * Retorna a foto do jogador associado a este handler.
	 * 
	 * @return A foto do jogador.
	 */
	String getPlayerPhoto();

	/**
	 * Define a foto do jogador (geralmente após o login/registo).
	 * 
	 * @param photo A foto do jogador.
	 */
	void setPlayerPhoto(String photo);

	/**
	 * Retorna o sexo do jogador associado a este handler.
	 * 
	 * @return O sexo do jogador.
	 */
	String getPlayerGender();

	/**
	 * Define o sexo do jogador (geralmente após o login/registo).
	 * 
	 * @param gender O sexo do jogador.
	 */
	void setPlayerGender(String gender);

	/**
	 * Retorna a password do jogo do jogador associado a este handler.
	 * 
	 * @return A password de acesso ao jogo do jogador.
	 */
	String getPlayerPasswordGameAccess();

	/**
	 * Define a password de acessoa o jogo do jogador.
	 * 
	 * @param passwordGameAccess A password de acesso ao jogo do jogador.
	 */
	void setPlayerPasswordGameAccess(String passwordGameAccess);

	/**
	 * Retorna o jogo do jogador associado a este handler.
	 * 
	 * @return O jogo do jogador.
	 */
	GameSession getCurrentGame();

	/**
	 * Associa este handler a uma sessão de jogo.
	 * 
	 * @param game A sessão de jogo atual.
	 */
	void setCurrentGame(GameSession game);

	/**
	 * Retorna o oponente do jogador associado a este handler.
	 * 
	 * @return O oponente do jogador.
	 */
	IClientHandler getPlayerFoundClient();

	/**
	 * Define o oponente do jogador .
	 * 
	 * @param foundClient O oponente do jogador.
	 */
	void setPlayerFoundClient(IClientHandler foundClient);

	/**
	 * Inicia o processamento de mensagens do cliente (relevante para a
	 * implementação baseada em threads).
	 */
	void run();

	/**
	 * Fecha a conexão com o cliente e liberta os recursos.
	 */
	void closeConnection();

	IClientHandler clientManagerByPlayerName(String gameName, HashMap<IClientHandler, String> waitingGames);

}