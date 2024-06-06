package Objects;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import org.ielena.chat.Mediator;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.ielena.chat.Mediator.*;

public class User extends Thread {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    public boolean close = false;
    private Thread receiverThread;

    final String SERVER_IP = "34.175.206.223"; // Dirección IP del servidor
    final int SERVER_PORT = 12345; // Puerto en el que el servidor está escuchando
    File soundFile = new File("src/main/resources/Audio/message1.wav"); // Ruta del archivo de sonido
    String soundMessage = soundFile.toURI().toString(); // Notificación de mensajes
    AudioClip audioClip = new AudioClip(soundMessage);

    public User(int clientId) {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ArrayList<Integer> groups = databaseManager.checkUserGroups(clientId);

            // Enviar el identificador del cliente al servidor
            writer.println(clientId + "" + groups);

            //Si se ha creado correctamente, se deja userError en false
            userError = false;

            // Iniciar hilo para recibir mensajes
            startReceiverThread();

        } catch (IOException e) {
            //Se reinicia el login si no se puede conectar al servidor
            showError("Error de Conexión", "Error al acceder a tu cuenta, inténtelo de nuevo");
            userError = true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void startReceiverThread() {
        receiverThread = new Thread(() -> {
            while (running) {
                try {
                    if (reader.ready()) {
                        String message = reader.readLine();
                        if (message != null) {
                            messageQueue.put(message); // Añadir mensaje a la cola
                        } else {
                            running = false; // Terminar bucle si la conexión se cierra
                        }
                    } else {
                        Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException e) {
                    running = false;
                }
            }
        });
        receiverThread.start();
    }

    public boolean waitForUpdate() {
        if (running){
            try {
                Thread.sleep(10000);
                return true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
       return false;
    }

    //MENSAJE A CONTACTOS
    public void sendMessage(int targetClientId, ArrayList<Integer> groups, String message) {
        if (writer != null) {
            writer.println(targetClientId + "" + groups + ":" + message);
        } else {
            System.out.println("No se ha establecido la conexión con el servidor.");
        }
    }

    //MENSAJES A GRUPOS
    public void sendGroup(int targetClientId, String message) {
        if (writer != null) {
            writer.println(targetClientId + ":" + message);
        } else {
            System.out.println("No se ha establecido la conexión con el servidor.");
        }
    }

    public boolean receiveMessage() {
        String message = "";
        //Si close está en true, quiere decir que el usuario ha cerrado la aplicación, su sesión ha terminado
        if (close) {
            return false;
        }

        //Esperar a recibir el mensaje
        try {
            message = messageQueue.take();
        } catch (InterruptedException e) {

        }
        Platform.runLater(() -> {
            if (messageTo.equals("contactName")) {
                Mediator.getMessageUserChat(id_user, contactOrGroupID);
            } else if (messageTo.equals("groupName")) {
                Mediator.getMessagesGroupChat(id_user, contactOrGroupID);
            }

        });
        //Si el mensaje no está vacio se reproduce el sonido
        if (!message.equals("")) {
            // Reproducir sonido
            audioClip.play();
        }

        return true;

    }

    public void disconnectFromServer() {
        running = false;
        receiverThread.interrupt(); // Interrumpir el hilo
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}