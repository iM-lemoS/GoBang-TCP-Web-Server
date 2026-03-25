# GoBang (Five in a Row) - Multiplayer Distributed System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white)
![HTML/CSS/JS](https://img.shields.io/badge/Frontend-E34F26?style=for-the-badge&logo=html5&logoColor=white)

A client-server distributed system for the strategy game **GoBang** (also known as Five in a Row or Gomoku). 

This project was developed for the **Distributed Computational Infrastructures (IECD)** course and stands out for its flexible architecture capable of simultaneously supporting console clients (via **TCP Sockets**) and web browser clients (via **WebSockets**).

## 🎮 Features

* **Multi-Protocol Architecture:** The central server manages traditional TCP connections and modern WebSocket connections, allowing cross-platform matches between the command line and the web interface.
* **Authentication and Profile Management:** Complete registration/login system. Players can change passwords, enter their nationality (with validation), and add profile pictures.
* **Real-time Game Engine:** Move validation, automatic win/draw detection, and continuous game time tracking for each player.
* **Lobby and Matchmaking:** Ability to create public games or private games (password-protected), along with a list of available games.
* **Leaderboard (Honor Roll):** Global scoring system based on the number of wins, with tie-breaking using the average move time. It is possible to download the leaderboard as a dynamic HTML file.
* **XML Data Persistence:** All game history, statistics, and profiles are saved through XML DOM manipulation, including an automatic backup system to prevent data corruption.

## 🏗️ System Architecture

To support multiple types of clients without duplicating game logic, a strong **Abstraction Layer** was implemented on the server:

1. **`IClientHandler` & `AbstractClientManager`:** An interface and abstract class that define the generic behaviors of any client.
2. **`MessageProcessor`:** A central class that processes commands (LOGIN, MOVE, etc.) agnostically, responding regardless of the original communication channel.
3. **`GameSession`:** Encapsulates the game logic (the `GoBang` class) and manages the turn flow between two `IClientHandler` instances.

*(You can check the complete PDF report in the `/docs` folder to see the architecture diagrams).*

## 🚀 How to Run the Project

### Prerequisites
* Java Development Kit (JDK) 11 or higher.
* Tomcat (embedded in the application dependencies or configured in your IDE).

### Starting the Server
1. Compile the project.
2. Run the main class `Server.java` (located in `src/main/java/TCP/Server.java`).
3. The server will open port `5025` for TCP connections and start the embedded Tomcat on port `8080` for WebSockets.

### Connecting a WEB Client
1. With the server running, open a web browser.
2. Go to `http://localhost:8080/`.
3. Interact with the visual interface to create an account, log in, and play.

### Connecting a Console Client (TCP)
1. In another terminal window, run the `ClientTCP.java` class (located in `src/main/java/TCP/ClientTCP.java`).
2. Follow the terminal instructions to register/log in and interact with the game via the console.

## 📸 Screenshots

*(Add some images here: Take screenshots of the lobby, the web game board, and the terminal running, save them in a `/docs/images/` folder, and replace the links below)*

| Web Interface (Lobby & Profile) | Game Board (Web) | Console Client (TCP) |
|:---:|:---:|:---:|
| <img src="docs/placeholder.png" width="250"/> | <img src="docs/placeholder.png" width="250"/> | <img src="docs/placeholder.png" width="250"/> |


Academic project developed by:
* **Rafael Lemos**  Institution: Instituto Superior de Engenharia de Lisboa
