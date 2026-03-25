<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="pt">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<title>GoBang Online</title>
	<!-- Ligação para a fonte do Google (mais eficiente que @import) -->
	<link
		href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap"
		rel="stylesheet">
	<!-- Ligação para o nosso ficheiro de estilos externo -->
	<link rel="stylesheet" href="gobang-style.css">
</head>
<body>
	<h1>GoBang</h1>

	<div class="container">
		<!-- Telas de Login e Registo -->
		<div id="login-screen" class="screen active">
			<h2>Login</h2>
			<div>
				<input type="text" id="nickname" placeholder="Nickname">
			</div>
			<div>
				<input type="password" id="password" placeholder="Password">
			</div>
			<button onclick="login()">Login</button>
			<p>
				Não tens conta?
				<button class="link-button" onclick="showScreen('register-screen')">Regista-te
					aqui</button>
			</p>
		</div>
		<div id="register-screen" class="screen">
			<h2>Registo de novo jogador</h2>
			<div>
				<input type="text" id="reg-nickname" placeholder="Nickname">
			</div>
			<div>
				<input type="password" id="reg-password" placeholder="Password">
			</div>
			<div>
				<input type="text" id="reg-country" placeholder="País">
			</div>
			<div>
				<input type="number" id="reg-age" placeholder="Idade">
			</div>
			<div>
				<input type="text" id="reg-gender"
					placeholder="Sexo (Masculino/Feminino)">
			</div>
			<div>
				<input type="text" id="reg-photo"
					placeholder="(Opcional) Caminho para foto (ex: C:/img.png)">
			</div>
			<button onclick="register()">Registar</button>
			<p>
				Já tens conta?
				<button class="link-button" onclick="showScreen('login-screen')">Faz
					login</button>
			</p>
		</div>

		<!-- Lobby  -->
		<div id="lobby-screen" class="screen">
			<h2 id="lobby-title"></h2>
			<button style="margin-bottom: 5px;" onclick="showProfile()">Perfil</button>
			<button style="margin-bottom: 5px;" onclick="showLeaderboard()">Quadro de Honra</button>
			<button style="margin-bottom: 5px;" onclick="showSettingsModal()">Personalização</button>
			<button style="margin-bottom: 5px;" onclick="createGame()">Criar jogo público</button>
			<button style="margin-bottom: 5px;" onclick="createPrivateGame()">Criar jogo privado</button>

			<hr style="width: 100%; margin: 1em 0;">

			<h3>Lista de jogos disponíveis</h3>
			<div id="game-list"></div>
			<button onclick="listGames()">Atualizar lista de jogos</button>

			<!-- SECÇÃO ADICIONADA PARA LIGAR MANUALMENTE -->
			<div id="join-game-area"
				style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 20px; text-align: center;">
				<h4>Ligar a um jogo específico</h4>
				<!-- Wrapper para o Autocomplete -->
				<div class="autocomplete" style="width: 270px; margin: 0 auto;">
					<input type="text" id="host-name-input"
						placeholder="Nome do Anfitrião">
				</div>
				<button onclick="joinGame()">Entrar no jogo</button>
			</div>

		</div>

		<div id="profile-screen" class="screen">
			<h2>Perfil de Jogador</h2>
			<img id="profile-pic-display" class="profile-main-pic" src=""
				alt="Foto de Perfil" onclick="promptChangePhoto()">
			<p>
				<strong>Total de jogos:</strong> <span id="stats-total">0</span>
			</p>
			<p>
				<strong>Vitórias:</strong> <span id="stats-wins">0</span> | <strong>Derrotas:</strong>
				<span id="stats-losses">0</span> | <strong>Empates:</strong> <span
					id="stats-draws">0</span>
			</p>
			<button style="margin-bottom: 5px;" onclick="promptChangePassword()">Trocar password</button>
			<button onclick="showScreen('lobby-screen')">Voltar ao Lobby</button>
			<div id="history-container" style="width: 100%;"></div>
		</div>

		<div id="leaderboard-screen" class="screen">
			<div id="leaderboard-container"
				style="width: 100%; text-align: center;"></div>
			<br>
			<button style="margin-bottom: 5px;" onclick="downloadLeaderboard()">Fazer download do Quadro de Honra</button>
			<button onclick="showScreen('lobby-screen')">Voltar ao Lobby</button>
		</div>

		<div id="game-screen" class="screen">
			<div class="game-container">
				<div>
					<h3 id="game-status"></h3>
					<canvas id="board" width="451" height="451"></canvas>
				</div>
				<div class="game-info">
					<div class="timers">
						<h4 style="margin-top: 0;">Tempos de jogo</h4>
						<div id="time-black">Pretas (X): 00:00</div>
						<div id="time-white">Brancas (O): 00:00</div>
						<div id="time-total">Total: 00:00</div>
					</div>
					<div class="log-area">
						<h4>Consola de jogo</h4>
						<div id="log"></div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Modal -->
	<div id="password-modal" class="modal-overlay">
		<div class="modal-content">
			<h3 id="modal-title">Insere a password do jogo</h3>
			<input type="password" id="modal-password-input"
				placeholder="Password">
			<button id="modal-confirm-btn">Entrar</button>
			<button id="modal-cancel-btn" class="link-button">Cancelar</button>
		</div>
	</div>

	<!-- Modal para Configurações -->
	<div id="settings-modal" class="modal-overlay">
		<div class="modal-content">
			<h3>Personalização</h3>

			<!-- Opção de Tema Geral -->
			<div>
				<label for="theme-select">Tema geral:</label> <select
					id="theme-select" onchange="changeTheme()">
					<option value="theme-light">Claro</option>
					<option value="theme-dark">Escuro</option>
					<option value="theme-sepia">Sépia</option>
				</select>
			</div>

			<br></br>

			<button onclick="closeSettingsModal()">Fechar</button>
		</div>
	</div>

	<!-- Inclusão do nosso ficheiro JavaScript externo -->
	<script src="gobang-app.js"></script>
</body>
</html>