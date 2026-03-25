package XML;

//Adicione esta classe no final do ficheiro XMLManagerScores.java
public class PlayerStats implements Comparable<PlayerStats> {
	private String nickname;
	private int wins;
	protected double averageTimeInSeconds;

	public PlayerStats(String nickname, int wins, double averageTimeInSeconds) {
		this.setNickname(nickname);
		this.setWins(wins);
		this.averageTimeInSeconds = averageTimeInSeconds;
	}

	// Lógica de comparação para ordenação
	@Override
	public int compareTo(PlayerStats other) {
		// 1. Ordenar por vitórias (descendente)
		if (this.getWins() != other.getWins()) {
			return Integer.compare(other.getWins(), this.getWins());
		}
		// 2. Em caso de empate, ordenar por tempo médio (ascendente - menos tempo é
		// melhor)
		return Double.compare(this.averageTimeInSeconds, other.averageTimeInSeconds);
	}

	// Método para formatar o tempo de volta para MM:SS
	public String getFormattedAverageTime() {
		long totalSeconds = (long) this.averageTimeInSeconds;
		long minutes = totalSeconds / 60;
		long seconds = totalSeconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public int getWins() {
		return wins;
	}

	public void setWins(int wins) {
		this.wins = wins;
	}

}