var nicknameInput = document.getElementById('nickname');
var passwordInput = document.getElementById('password');
var boardCanvas = document.getElementById('board');
var ctx = boardCanvas.getContext('2d');
var TILE_SIZE = 30;
var BOARD_SIZE = 15;
var myPiece = '', myTurn = false, playerName = '';
var availableHosts = [];
var host = window.location.host; // Isto vai capturar "192.168.1.10:8080" ou "localhost:8080"
var wsUrl = 'ws://' + host + '/game';
var ws;
var bodyElement = document.body;
var boardElement = document.getElementById('board');
var themeSelect = document.getElementById('theme-select');
var currentLeaderboardData = [];
function connectWebSocket() {
	ws = new WebSocket(wsUrl);
	ws.onopen = function() { console.log('Conectado ao servidor.'); };
	ws.onmessage = function(event) {
		console.log('<< Servidor: ' + event.data);
		handleServerMessage(event.data);
	};
	ws.onclose = function() { console.log('Desconectado do servidor.'); };
	ws.onerror = function(error) {
		console.log('Erro na conexão WebSocket.');
		console.error('WebSocket Error:', error);
	};
}
connectWebSocket();
function handleServerMessage(message) {
	var command = message.split(' ')[0].split('_#')[0];
	var partsWithSpaces = message.split(' ');
	var partsWithHashes = message.split('_#');
	var args = partsWithSpaces.slice(1);
	switch (command) {
		case "LOGIN_SUCCESS":
			playerName = nicknameInput.value;
			var gender = args[0];
			var welcomeMessage;
			if (gender && gender.toUpperCase() === 'MASCULINO') {
				welcomeMessage = 'Bem-vindo de volta, ' + playerName + '!';
			} else {
				welcomeMessage = 'Bem-vinda de volta, ' + playerName + '!';
			}
			document.getElementById('lobby-title').innerText = welcomeMessage;
			showScreen('lobby-screen');
			listGames();
			break;
		case "LOGIN_FAIL":
			alert('Login falhou: ' + args.join(' '));
			break;
		case "REGIST_SUCCESS":
			alert("Registo bem-sucedido! Já podes fazer login.");
			showScreen('login-screen');
			break;
		case "REGIST_FAIL":
		case "REGIST_FAIL_COUNTRY":
			alert('Registo falhou: ' + args.join(' '));
			break;
		case "LIST_GAMES":
			var gameList = document.getElementById('game-list');
			gameList.innerHTML = '';
			availableHosts = [];
			var numGames = parseInt(args[0] || "0");
			if (numGames === 0) {
				gameList.innerHTML = '<p>Nenhum jogo disponível.</p>';
			} else {
				for (var i = 0; i < numGames; i++) {
					var hostName = args[i + 1];
					if (hostName != playerName) {
						availableHosts.push(hostName);
						var button = document.createElement('button');
						button.textContent = 'Juntar a ' + hostName;
						(function(hn) {
							button.onclick = function() { joinGame(hn); };
						})(hostName);
						gameList.appendChild(button);
					}
				}
			}
			break;
		case "GAME_NOT_FOUND":
			alert("Jogo não encontrado ou o anfitrião já saiu.");
			break;
		case "ASK_PASSWORD":
			showPasswordModal(args[0]);
			break;
		case "PASSWORD_INCORRECT":
			alert("Password incorreta!");
			break;
		case "MENU_PROFILE":
			document.getElementById('stats-total').innerText = partsWithSpaces[1];
			document.getElementById('stats-wins').innerText = partsWithSpaces[2];
			document.getElementById('stats-losses').innerText = partsWithSpaces[3];
			document.getElementById('stats-draws').innerText = partsWithSpaces[4];
			var photoBase64 = partsWithSpaces.slice(5).join('');
			var profilePicElement = document.getElementById('profile-pic-display');
			if (photoBase64 && photoBase64.trim() !== '') {
				profilePicElement.src = 'data:image/jpeg;base64,' + photoBase64;
				profilePicElement.style.display = 'block';
			} else {
				profilePicElement.src = 'perfil-avatar.jpg';
				profilePicElement.style.display = 'block';
			}
			ws.send("HISTORY");
			showScreen('profile-screen');
			break;
		case "INCORRECT_OLD_PASSWORD":
			alert("Password antiga incorreta. Tente novamente.");
			break;
		case "CORRECT_OLD_PASSWORD":
			// A password antiga está correta, agora pedimos a nova
			var newPassword = prompt("Password antiga correta. Por favor, insere a nova password:", "");
			if (newPassword && newPassword.trim() !== '') {
				// Validação básica para não permitir espaços na password
				if (newPassword.includes(' ')) {
					alert("A nova password não pode conter espaços.");
				} else {
					ws.send("CHANGE_PASSWORD " + newPassword);
				}
			}
			break;
		case "PASSWORD_CHANGED_SUCCESS":
			alert("A tua password foi alterada com sucesso!");
			break;
		case "HISTORY":
			var historyContainer = document.getElementById('history-container');
			historyContainer.innerHTML = '<h4>Histórico de Partidas</h4>';
			// Usa 'partsWithHashes' que é o separador correto para esta mensagem
			var games = partsWithHashes.slice(1);
			if (games.length === 0 || (games.length === 1 && games[0].trim() === '')) {
				historyContainer.innerHTML += '<p>Sem histórico para mostrar.</p>';
			} else {
				var table = document.createElement('table');
				table.id = "historyTable";
				table.innerHTML = '<tr><th>Data</th><th>Hora</th><th>Adversário</th><th>Resultado</th></tr>';
				games.forEach(function(gameStr) {
					if (!gameStr) return;
					var info = gameStr.replace(/\[|\]/g, '');
					var [id, selfTime, opTime, result] = info.split(', ');
					var idParts = id.split(' | ');
					var date = idParts[1].split(': ')[1];
					var time = idParts[0].split(': ')[1];
					var p1 = idParts[2].split(': ')[1];
					var p2 = idParts[3].split(': ')[1];
					var opponent = p1 === playerName ? p2 : p1;
					table.innerHTML += '<tr><td>' + date + '</td><td>' + time + '</td><td>' + opponent + '</td><td>' + result + '</td></tr>'
				});
				historyContainer.appendChild(table);
			}
			break;
		case "OPONENTE_ENCONTRADO":
			myPiece = args[1];
			myTurn = myPiece === 'X';
			document.getElementById('game-status').innerText = 'Oponente: ' + args[0];
			drawBoard();
			log(myTurn ? "Começas tu!" : "Aguarda pelo teu oponente.");
			showScreen('game-screen');
			break;
		case "TEMPOS":
			// Usa 'partsWithHashes' que é o separador correto para esta mensagem
			document.getElementById('time-black').innerText = partsWithHashes[2];
			document.getElementById('time-white').innerText = partsWithHashes[3];
			document.getElementById('time-total').innerText = partsWithHashes[4];
			break;
		case "MOVE":
			// Usa 'partsWithSpaces' porque MOVE usa espaços
			drawPiece(parseInt(partsWithSpaces[2]) - 1, parseInt(partsWithSpaces[1]) - 1, partsWithSpaces[3]);
			myTurn = (partsWithSpaces[3] !== myPiece);
			log(myTurn ? 'É a tua vez.' : 'Aguarda...');
			break;
		case "JOGADA_INVALIDA":
			alert("Jogada inválida! Tenta outra posição.");
			myTurn = true;
			break;
		case "FIM_JOGO":
			alert('Fim do jogo! ' + args.join(' '));
			showScreen('login-screen');
			break;
		case "PHOTO_CHANGED_SUCCESS":
			alert("Foto de perfil atualizada com sucesso!");
			// Pede os dados do perfil novamente para atualizar a imagem no ecrã
			ws.send("ACCOUNT");
			break;
		case "LEADERBOARD_DATA":
			var container = document.getElementById('leaderboard-container');
			container.innerHTML = '<h2>Quadro de Honra</h2>';

			var leaderboardParts = message.split('_#');
			var allPlayers = leaderboardParts.slice(1);

			currentLeaderboardData = [];

			if (allPlayers.length === 0 || allPlayers[0].trim() === '') {
				container.innerHTML += '<p>Ainda não há dados para mostrar.</p>';
				showScreen('leaderboard-screen');
				break;
			}

			allPlayers.forEach(function(playerStr) {
				if (!playerStr) return;
				var stats = playerStr.split(';');
				currentLeaderboardData.push({
					rank: stats[0],
					nick: stats[1],
					wins: stats[2],
					avgTime: stats[3]
				});
			});

			var visualWrapper = document.createElement('div');
			visualWrapper.className = 'leaderboard-visual';

			// --- Lógica para o Pódio (Top 3) com FOTOS ---
			var podiumPlayers = allPlayers.slice(0, 3);
			var podiumContainer = document.createElement('div');
			podiumContainer.className = 'podium-container';

			podiumPlayers.forEach(function(playerStr) {
				if (!playerStr) return;
				var stats = playerStr.split(';');
				var rank = stats[0], nick = stats[1], wins = stats[2], avgTime = stats[3], flagPath = stats[4], photoBase64 = stats[5];

				var placeDiv = document.createElement('div');
				placeDiv.className = 'podium-place podium-place-' + rank;

				// Cria o HTML da bandeira
				var flagHtml = '';
				if (flagPath && flagPath.trim() !== '') {
					flagHtml = '<img class="flag-icon" src="' + flagPath + '" alt="Bandeira">';
				}

				// Cria o HTML da foto de perfil
				var photoHtml = '';
				if (photoBase64 && photoBase64.trim() !== '') {
					// Usa o Data URL para exibir a imagem Base64
					photoHtml = '<img class="profile-pic" src="data:image/png;base64,' + photoBase64 + '" alt="Foto de Perfil">';
				} else {
					// Fallback para uma imagem genérica ou um ícone
					photoHtml = '<img class="profile-pic" src="perfil-avatar.jpg" alt="Foto de Perfil">';
				}


				placeDiv.innerHTML =
					// A foto vai primeiro
					photoHtml +
					'<span class="rank">' + rank + '</span>' +
					'<span class="nick">' + flagHtml + nick + '</span>' +
					'<span class="wins">' + wins + ' Vitórias</span>';

				podiumContainer.appendChild(placeDiv);
			});
			visualWrapper.appendChild(podiumContainer);

			// --- Lógica para o resto da lista (4-10) - SEM FOTO ---
			var restPlayers = allPlayers.slice(3);
			if (restPlayers.length > 0) {
				var listContainer = document.createElement('div');
				listContainer.className = 'leaderboard-list';

				restPlayers.forEach(function(playerStr) {
					if (!playerStr) return;
					var stats = playerStr.split(';');
					var rank = stats[0], nick = stats[1], wins = stats[2], avgTime = stats[3], flagPath = stats[4];

					var flagHtml = '';
					if (flagPath && flagPath.trim() !== '') {
						flagHtml = '<img class="flag-icon" src="' + flagPath + '" alt="Bandeira">';
					}

					var itemDiv = document.createElement('div');
					itemDiv.className = 'list-item';

					itemDiv.innerHTML =
						'<span class="rank">' + rank + '.</span>' +
						flagHtml +
						'<span class="nick">' + nick + '</span>' +
						'<span class="wins">  (' + wins + ' vitórias)</span>';

					listContainer.appendChild(itemDiv);
				});
				visualWrapper.appendChild(listContainer);
			}

			container.appendChild(visualWrapper);
			showScreen('leaderboard-screen');
			break;

	}

}
function showScreen(screenId) {
	document.querySelectorAll('.screen').forEach(function(s) { s.classList.remove('active'); });
	document.getElementById(screenId).classList.add('active');
}
function showPasswordModal(hostName) {
	var modal = document.getElementById('password-modal');
	modal.style.display = 'flex';
	document.getElementById('modal-confirm-btn').onclick = function() {
		var password = document.getElementById('modal-password-input').value;
		ws.send('RECEIVED_PASSWORD ' + password);
		modal.style.display = 'none';
		document.getElementById('modal-password-input').value = '';
	};
	document.getElementById('modal-cancel-btn').onclick = function() {
		modal.style.display = 'none';
	};
}
function log(message) {
	var logDiv = document.getElementById('log');
	logDiv.innerHTML += '<div>' + message + '</div>';
	logDiv.scrollTop = logDiv.scrollHeight;
}
function login() {
	var pass = passwordInput.value;
	playerName = nicknameInput.value.trim();
	if (playerName && pass) ws.send('LOGIN ' + playerName + ' ' + pass);
}
function register() {
	var data = {
		nick: document.getElementById('reg-nickname').value.trim(),
		pass: document.getElementById('reg-password').value,
		country: document.getElementById('reg-country').value.trim(),
		age: document.getElementById('reg-age').value,
		gender: document.getElementById('reg-gender').value.trim(),
		photo: document.getElementById('reg-photo').value.trim()
	};
	if (data.nick && data.pass && data.country && data.age && data.gender) {
		ws.send('REGISTO_#' + data.nick + '_#' + data.country + '_#' + data.age + '_#' + data.pass + '_#' + data.photo + '_#' + data.gender);
	} else {
		alert("Preenche todos os campos obrigatórios.");
	}
}
function showLeaderboard() {
	ws.send("GET_LEADERBOARD");
}
function showProfile() { ws.send("ACCOUNT"); }
function createGame() { ws.send("CREATE_GAME"); }
function createPrivateGame() {
	var pass = prompt("Define uma password para o jogo:", "");
	if (pass) ws.send('CREATE_GAME ' + pass);
}
function listGames() { ws.send("LIST_GAMES"); }
function joinGame(hostName) {
	var nameToJoin;
	// Se o hostName for passado como argumento (clique num botão da lista)
	if (hostName) {
		nameToJoin = hostName;
	} else {
		// Se não, obter do campo de input (clique no botão geral)
		var input = document.getElementById('host-name-input');
		nameToJoin = input.value.trim();
		input.value = ''; // Limpa o input após o uso
	}

	if (nameToJoin) {
		ws.send('JOIN_GAME ' + nameToJoin);
	} else {
		alert('Insere o nome do anfitrião do jogo.');
	}

}
// --- Funções do Canvas ---
function drawBoard() {
	ctx.clearRect(0, 0, boardCanvas.width, boardCanvas.height);
	ctx.strokeStyle = '#000';

	for (var i = 0; i <= BOARD_SIZE; i++) {
		// Linhas Verticais
		ctx.beginPath();
		ctx.moveTo(i * TILE_SIZE + 0.5, 0.5);
		ctx.lineTo(i * TILE_SIZE + 0.5, boardCanvas.height - 0.5);
		ctx.stroke();

		// Linhas Horizontais
		ctx.beginPath();
		ctx.moveTo(0.5, i * TILE_SIZE + 0.5);
		ctx.lineTo(boardCanvas.width - 0.5, i * TILE_SIZE + 0.5);
		ctx.stroke();
	}

}
function drawPiece(col, row, piece) {
	var x = col * TILE_SIZE + TILE_SIZE / 2 + 0.5;
	var y = row * TILE_SIZE + TILE_SIZE / 2 + 0.5;
	ctx.beginPath();
	ctx.arc(x, y, TILE_SIZE / 2 - 4, 0, 2 * Math.PI);
	ctx.save();
	ctx.shadowColor = 'rgba(0,0,0,0.4)';
	ctx.shadowBlur = 4;
	ctx.shadowOffsetX = 2;
	ctx.shadowOffsetY = 2;
	ctx.fillStyle = piece === 'X' ? '#333' : '#fff';
	ctx.fill(); ctx.restore();
	ctx.strokeStyle = '#555';
	ctx.stroke();
}
boardCanvas.addEventListener('click', function(event) {
	if (!myTurn) return;
	var rect = boardCanvas.getBoundingClientRect();
	var col = Math.floor((event.clientX - rect.left) / TILE_SIZE);
	var row = Math.floor((event.clientY - rect.top) / TILE_SIZE);
	myTurn = false;
	ws.send('MOVE ' + (row + 1) + ' ' + (col + 1));
});
// Função para abrir o modal de configurações
function showSettingsModal() {
	document.getElementById('settings-modal').style.display = 'flex';
}
// Função para fechar o modal
function closeSettingsModal() {
	document.getElementById('settings-modal').style.display = 'none';
}
// Função para mudar o tema geral
function changeTheme() {
	var selectedTheme = themeSelect.value;
	// Remove classes de tema antigas
	bodyElement.classList.remove('theme-light', 'theme-dark', 'theme-sepia');

	// Adiciona a nova classe de tema
	bodyElement.classList.add(selectedTheme);

	// Salva a preferência no localStorage
	localStorage.setItem('goBangTheme', selectedTheme);

}
function promptChangePhoto() {
	// O prompt() mostra uma caixa de diálogo.
	// O segundo argumento é o valor padrão, que deixamos vazio.
	var newPhotoPath = prompt("Insere o novo caminho para a foto de perfil:", "");
	// Verifica se o utilizador inseriu algo e não clicou em "Cancelar"
	// (prompt retorna null se for cancelado)
	if (newPhotoPath !== null && newPhotoPath.trim() !== '') {
		// Envia o novo comando para o servidor com o caminho
		ws.send('CHANGE_PHOTO_#' + newPhotoPath.trim());
	}

}
function promptChangePassword() {
	// Pede a password antiga
	var oldPassword = prompt("Para tua segurança, por favor insere a tua password antiga:", "");
	// Se o utilizador inseriu algo, envia para verificação
	if (oldPassword) { // 'prompt' retorna null se cancelado, ou "" se OK sem texto
		ws.send("VERIFY_OLD_PASSWORD " + oldPassword);
	}

}
// --- LÓGICA DO AUTOCOMPLETE (VERSÃO FINAL) ---
function initializeAutocomplete() {
	var inp = document.getElementById("host-name-input");
	var currentFocus;
	// Remove qualquer listener antigo para evitar duplicação
	inp.oninput = null;

	inp.addEventListener("input", function(e) {
		var a, b, i, val = this.value;
		closeAllLists();
		if (!val) { return false; }
		currentFocus = -1;

		a = document.createElement("DIV");
		a.setAttribute("id", this.id + "autocomplete-list");
		a.setAttribute("class", "autocomplete-items");
		this.parentNode.appendChild(a);

		// Usa a lista global 'availableHosts' que é sempre a mais recente
		for (i = 0; i < availableHosts.length; i++) {
			if (availableHosts[i].substr(0, val.length).toUpperCase() == val.toUpperCase()) {
				b = document.createElement("DIV");
				b.innerHTML = "<strong>" + availableHosts[i].substr(0, val.length) + "</strong>";
				b.innerHTML += availableHosts[i].substr(val.length);
				b.innerHTML += "<input type='hidden' value='" + availableHosts[i] + "'>";
				b.addEventListener("click", function(e) {
					inp.value = this.getElementsByTagName("input")[0].value;
					closeAllLists();
				});
				a.appendChild(b);
			}
		}
	});

	function closeAllLists(elmnt) {
		var x = document.getElementsByClassName("autocomplete-items");
		for (var i = 0; i < x.length; i++) {
			if (elmnt != x[i] && elmnt != inp) {
				x[i].parentNode.removeChild(x[i]);
			}
		}
	}

	// Adiciona o listener para fechar a lista uma única vez
	if (!document.autocompleteListenerAdded) {
		document.addEventListener("click", function(e) {
			closeAllLists(e.target);
		});
		document.autocompleteListenerAdded = true;
	}

}
function downloadLeaderboard() {
	if (currentLeaderboardData.length === 0) {
		alert("Não há dados no leaderboard para fazer o download.");
		return;
	}
	// --- Parte A: Gerar o conteúdo HTML do pódio e da lista ---
	let podiumHTML = '';
	let listHTML = '';

	const podiumPlayers = currentLeaderboardData.slice(0, 3);
	const restPlayers = currentLeaderboardData.slice(3);

	podiumPlayers.forEach(stats => {
		podiumHTML += `
        <div class="podium-place podium-place-${stats.rank}">
            <span class="rank">${stats.rank}</span>
            <span class="wins">${stats.wins} Vitórias</span>
        </div>`;
	});

	if (restPlayers.length > 0) {
		restPlayers.forEach(stats => {
			listHTML += `
            <div class="list-item">
                <span class="rank">${stats.rank}.</span>
                <span class="nick">${stats.nick}</span>
                <span class="wins">(${stats.wins} vitórias)</span>
            </div>`;
		});
	}

	// --- Parte B: Montar o documento HTML completo com CSS embutido ---
	const htmlContent = `
<!DOCTYPE html>
<html lang="pt">
<head>
    <meta charset="UTF-8">
    <title>Quadro de Honra - GoBang</title>
    <style>
        body { font-family: 'Roboto', sans-serif; background-color: #f0f2f5; color: #333; text-align: center; }
        h1 { color: #000; text-shadow: 1px 1px 2px #ccc; }
        .leaderboard-visual { width: 100%; max-width: 800px; margin: 0 auto; padding: 20px 0; }
        .podium-container { display: flex; justify-content: center; align-items: flex-end; gap: 15px; min-height: 200px; border-bottom: 2px solid #ccc; padding-bottom: 20px; margin-bottom: 20px; }
        .podium-place { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; padding: 15px; padding-top: 50px; border-radius: 8px; background-color: #f0f2f5; width: 120px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
        .podium-place .rank { font-size: 2.8em; font-weight: bold; color: #a0aec0; line-height: 1; margin-bottom: 10px; }
        .podium-place .nick { font-size: 1.1em; font-weight: bold; margin-top: 5px; }
        .podium-place .wins { font-size: 0.9em; color: #718096; }
        .podium-place-1 { order: 2; height: 160px; background-color: #ffd700; }
        .podium-place-1 .rank { color: #b8860b; }
        .podium-place-2 { order: 1; height: 130px; background-color: #c0c0c0; }
        .podium-place-2 .rank { color: #6e6e6e; }
        .podium-place-3 { order: 3; height: 100px; background-color: #cd7f32; }
        .podium-place-3 .rank { color: #8c5e4a; }
        .leaderboard-list { display: grid; grid-template-columns: 1fr; gap: 10px; text-align: left; padding: 0 20px; }
        .list-item { background: #fafafa; padding: 10px; border-radius: 5px; border-left: 4px solid #822c2c; }
        .list-item .rank { font-weight: bold; margin-right: 8px; }
    </style>
</head>
<body>
    <h1>Quadro de Honra - GoBang</h1>
    <div class="leaderboard-visual">
        <div class="podium-container">
            ${podiumHTML}
        </div>
        <div class="leaderboard-list">
            ${listHTML}
        </div>
    </div>
</body>
</html>
    `;
	// --- Parte C: Criar o ficheiro e despoletar o download ---
	// Cria um objeto Blob (Binary Large Object) com o conteúdo HTML
	const blob = new Blob([htmlContent], { type: 'text/html' });

	// Cria um elemento <a> temporário em memória
	const link = document.createElement('a');

	// Define o URL do link para o nosso Blob
	link.href = URL.createObjectURL(blob);

	// Define o nome do ficheiro para download
	const date = new Date().toISOString().slice(0, 10); // Formato YYYY-MM-DD
	link.download = `leaderboard_gobang_${date}.html`; // A extensão é .html

	// Simula um clique no link para iniciar o download
	link.click();

	// Limpa o URL do objeto para libertar memória
	URL.revokeObjectURL(link.href);

}
// Inicia a funcionalidade
initializeAutocomplete();