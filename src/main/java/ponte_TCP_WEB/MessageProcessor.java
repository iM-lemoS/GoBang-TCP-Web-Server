package ponte_TCP_WEB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import TCP.GameSession;
import TCP.Server;
import XML.PlayerStats;

public class MessageProcessor {
	String playerName;

// Este método pode agora ser chamado por ClientManagerTCP e WebSocketHandler
	public static void process(IClientHandler handler, String message, Server server) {
		String[] parts;

		// Extrai o comando principal (pode usar separador "_#" ou espaço)
		String comand;
		String[] partsCustom = message.split("_#");
		if (partsCustom.length > 1) {
			comand = partsCustom[0];
		} else {
			comand = message.split(" ")[0];
		}

		switch (comand) {
		case "LOGIN":
			parts = message.split(" ");
			handler.setPlayerName(parts[1]);
			handler.setPlayerPassword(parts[2]);

			if (server.getXmlManager().verifyPlayer(handler.getPlayerName(), handler.getPlayerPassword()) == null
					|| server.getLoggedPlayers().contains(handler.getPlayerName())) {
				handler.sendMessage("LOGIN_FAIL");
			} else {
				handler.sendMessage("LOGIN_SUCCESS " + server.getXmlManager().getPlayerGender(handler.getPlayerName()));
				server.getLoggedPlayers().add(handler.getPlayerName());
				System.out.print(server.getLoggedPlayers());
			}
			break;

		case "REGISTO":
			parts = message.split("_#");
			handler.setPlayerName(parts[1]);
			handler.setPlayerNationality(parts[2]);
			handler.setPlayerAge(Integer.parseInt(parts[3]));
			handler.setPlayerPassword(parts[4]);
			handler.setPlayerPhoto(parts[5]);
			handler.setPlayerGender(parts[6]);

			if (server.getXmlManager().addPlayer(handler.getPlayerName(), handler.getPlayerNationality(),
					handler.getPlayerAge(), handler.getPlayerPassword(), handler.getPlayerPhoto(),
					handler.getPlayerGender()) == 2) {
				handler.sendMessage("REGIST_FAIL");
			} else if (server.getXmlManager().addPlayer(handler.getPlayerName(), handler.getPlayerNationality(),
					handler.getPlayerAge(), handler.getPlayerPassword(), handler.getPlayerPhoto(),
					handler.getPlayerGender()) == 3) {
				handler.sendMessage("REGIST_FAIL_COUNTRY");
			} else {
				handler.sendMessage("REGIST_SUCCESS");
			}
			break;

		case "CREATE_GAME":
			String[] gameParts = message.split(" ");

			if (message.trim().equals("CREATE_GAME")) {
				server.addWaitingGame(handler, null);
			} else {
				String password = gameParts[1].trim();
				server.addWaitingGame(handler, password);
			}
			break;

		case "LIST_GAMES":
			String gamesList = "LIST_GAMES " + String.valueOf(server.getWaitingGames().size()) + " ";
			for (IClientHandler game : server.getWaitingGames().keySet()) {
				gamesList += game.getPlayerName() + " ";
			}
			handler.sendMessage(gamesList.trim());
			break;

		case "ACCOUNT":
			String totalGames = server.xmlManagerScores.getTotalGames(handler.getPlayerName());
			String wins = server.xmlManagerScores.getTotalWins(handler.getPlayerName());
			String losses = server.xmlManagerScores.getTotalLosses(handler.getPlayerName());
			String draws = server.xmlManagerScores.getTotalDraws(handler.getPlayerName());
			String photo = server.getXmlManager().getPlayerPhotoBase64(handler.getPlayerName());
			handler.sendMessage("MENU_PROFILE " + totalGames + " " + wins + " " + losses + " " + draws + " " + photo);
			break;

		case "CHANGE_PHOTO":
			String[] photoParts = message.split("_#", 2); // Divide apenas no primeiro _#
			if (photoParts.length > 1) {
				String newPath = photoParts[1];
				server.getXmlManager().updatePhoto(handler.getPlayerName(), newPath);

				// Envia uma confirmação de volta para o cliente
				handler.sendMessage("PHOTO_CHANGED_SUCCESS");
			}
			break;
		case "CHANGE_PASSWORD":
			parts = message.split(" ");
			server.getXmlManager().updatePassword(handler.getPlayerName(), parts[1]);
			handler.sendMessage("PASSWORD_CHANGED_SUCCESS");
			break;
		case "VERIFY_OLD_PASSWORD":
			parts = message.split(" ");
			if (server.getXmlManager().verifyPlayer(handler.getPlayerName(), parts[1]) == null) {
				handler.sendMessage("INCORRECT_OLD_PASSWORD");
			} else {
				handler.sendMessage("CORRECT_OLD_PASSWORD");
			}
			break;

		case "HISTORY":
			ArrayList<String[]> history = server.xmlManagerScores.getHistoryGames(handler.getPlayerName());
			String output = "HISTORY";
			if (history != null) {
				Collections.reverse(history);
				for (String[] game : history) {
					output += "_#" + Arrays.toString(game);
				}
				handler.sendMessage(output);
			} else {
				handler.sendMessage(output);
			}
			break;

		case "GET_LEADERBOARD":
			List<PlayerStats> leaderboard = server.xmlManagerScores.getLeaderboardData();
			StringBuilder response = new StringBuilder("LEADERBOARD_DATA");
			int rank = 1;

			for (PlayerStats stats : leaderboard) {
				// Obter nacionalidade e caminho da bandeira
				String nationality = server.getXmlManager().getPlayerNationality(stats.getNickname());
				String flagPath = "";
				if (nationality != null && !nationality.isEmpty()) {
					flagPath = server.getXmlManager().xmlManagerNationality.getFlagPath(nationality);
					if (flagPath == null)
						flagPath = "";
				}

				// --- NOVO: Obter foto de perfil ---
				String photoBase64 = server.getXmlManager().getPlayerPhotoBase64(stats.getNickname());

				response.append("_#").append(rank++).append(";").append(stats.getNickname()).append(";")
						.append(stats.getWins()).append(";").append(stats.getFormattedAverageTime()).append(";")
						.append(flagPath).append(";") // Elemento 5
						.append(photoBase64); // Elemento 6 (foto)
			}
			handler.sendMessage(response.toString());
			break;

		case "JOIN_GAME":
			parts = message.split(" ");
			String gameName = parts[1];
			handler.setPlayerFoundClient(handler.clientManagerByPlayerName(gameName, server.getWaitingGames()));
			if (handler.getPlayerFoundClient() != null) {
				if (server.getWaitingGames().get(handler.getPlayerFoundClient()) != null) {
					handler.sendMessage("ASK_PASSWORD");
				} else {
					server.createGameSession(handler.getPlayerFoundClient(), handler);
					handler.sendMessage("GAME_FOUND");
				}
			} else {
				handler.sendMessage("GAME_NOT_FOUND");
			}
			break;

		case "RECEIVED_PASSWORD":
			parts = message.split(" ");
			String passwordGame = parts[1];
			if (server.getWaitingGames().get(handler.getPlayerFoundClient()).equals(passwordGame)) {
				server.createGameSession(handler.getPlayerFoundClient(), handler);
				handler.sendMessage("GAME_FOUND");
			} else {
				handler.sendMessage("PASSWORD_INCORRECT");
			}
			break;

		case "MOVE":
			String[] moveParts = message.split(" ");

			if (moveParts.length != 3) {
				handler.sendMessage("ERRO_FORMATO");
				System.err.println("Mensagem MOVE mal formada: " + message);
				break;
			}

			GameSession currentGame = handler.getCurrentGame();
			if (currentGame != null) {
				currentGame.relayMessage(handler, message);
			}

			break;

		case "QUIT":
			server.getLoggedPlayers().remove(handler.getPlayerName());
			break;
		}
	}
}