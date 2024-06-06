package org.ielena.chat;

import Objects.Message;
import Server.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import static org.ielena.chat.Mediator.*;

public class ChatApplication extends Application {
    Stage stage = new Stage();
    Scene scene;

    //Variables de registrarse
    @FXML
    TextField username, mail, telephone;
    @FXML
    PasswordField password;
    //Iniciar sesión
    @FXML
    TextField emailOrPhoneNumberOrUsername, loginPassword;
    //Añadir usuario o grupo
    @FXML
    Button buttonAddUserGroup;
    @FXML
    Label addUserOrGroup, writeUserOrGroup;
    @FXML
    TextField textFieldUserOrGroup, searchContact, addMember;
    //Vista de chats
    @FXML
    public VBox chats;
    @FXML
    VBox chatSide;
    @FXML
    VBox addUserGroupVbox;
    @FXML
    HBox addUserGroupHbox;
    //Usuario iniciado
    @FXML
    Label nameLoggedUser;
    //Casilla de recuerdame
    @FXML
    CheckBox rememberMe;
    //Archivo de recuerdame

    //Fragment Contacto
    @FXML
    Label contactName, lastMessage, groupName, groupLastMessage;
    @FXML
    ImageView profileImage, userImage;
    //Vista del chat
    @FXML
    public ListView<Message> chatView;
    //Campo para enviar chats
    @FXML
    TextField writeMessageField;
    //Botón para enviar chats
    @FXML
    Button sendMessage;
    static ArrayList<Integer> allContacts = new ArrayList<>();
    public static ChatApplication chat;

    //COMIENZA LA APLICACIÓN ABRIENDO LA VENTANA LOGIN
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("login-view.fxml"));
        scene = new Scene(fxmlLoader.load());
        // Obtener el controlador después de cargar el archivo FXML
        ChatApplication chatApplication = fxmlLoader.getController();
        //Diseñar la ventana
        stage.setTitle("Inicio de sesión");
        stage.setMinWidth(800);
        stage.setMinHeight(800);
        stage.setScene(scene);
        stage.setResizable(true);

        //Primera descarga diferente para iniciar la aplicación
        //Start.changeChecker.firstDownload(new CountDownLatch(1));

        // Cargar el icono
        try {
            stage.getIcons().add(icon);

        } catch (NullPointerException e) {

        }
        //Si el usuario pidió que se le recordara
        ArrayList<TextField> fields = Mediator.remember(chatApplication.emailOrPhoneNumberOrUsername, chatApplication.loginPassword, chatApplication.rememberMe);
        //Si no es nulo, se autoescriben los campos
        if (fields != null) {
            emailOrPhoneNumberOrUsername = fields.get(0);
            loginPassword = fields.get(1);
        }
        //Mostrar el Login
        stage.show();
    }

    //BUSCAR CONTACTOS Y CARGARLOS EN LA VISTA PRINCIPAL
    public void searchUser() {
        String searchText = searchContact.getText();
        ArrayList<String> contactsName = new ArrayList<>();
        ArrayList<Integer> contactsSearched;

        for (int i = 0; i < allContacts.size(); i++) {
            try {
                contactsName.add(databaseManager.returnUsernameById(allContacts.get(i)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        // Llama al método searchUser() de Mediator
        try {
            contactsSearched = Mediator.searchUser(searchText, contactsName, allContacts);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //Limpiamos los chats si hubiese
        chats.getChildren().clear();

        //Cargamos en la vista los chats
        Mediator.loadUserFragments(contactsSearched, chats);

    }

    //MÉTODO QUE COMPRUEBA EL LOGIN ANTES DE ABRIR LA SESIÓN
    public void checkLogin(ActionEvent event) throws Exception {

        boolean login;
        login = checkSignIn(emailOrPhoneNumberOrUsername.getText(),loginPassword.getText(),rememberMe);
        //Si devuelve true, es que se inició con exito
        if (login) launchChat(event);

    }

    //MÉTODO QUE ABRE LA VENTANA DE CHAT CUANDO SE PULSA EN INCIAR SESION
    @FXML
    public void launchChat(ActionEvent event) throws Exception {

        // Cerrar la ventana actual (LOGIN)
        Window window = ((Node) event.getSource()).getScene().getWindow();
        Stage stage = (Stage) window;
        stage.close();

        // Abrir la nueva ventana (CHAT)
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("chat-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Obtener el controlador después de cargar el archivo FXML
        ChatApplication chatApplication = fxmlLoader.getController();
        // Guardamos el controlador también en la clase
        chat = chatApplication;

        stage.setTitle("Chat Galaxy");
        stage.setResizable(true);
        stage.setScene(scene);

        //Preparamos al usuario

        Mediator.prepareUser(emailOrPhoneNumberOrUsername, loginPassword, chat);

        stage.setWidth(1000);
        stage.setHeight(800);

        // Capturar el evento de cierre de la ventana para detener el hilo
        stage.setOnCloseRequest(windowEvent -> {
            //Terminamos de recibir mensajes
            user.close = true;
            user.disconnectFromServer();
            threadReceiveMessages.interrupt();
        });

        // Cargar el icono
        try {
            stage.getIcons().add(icon);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (userError){
            //El usuario no ha podido crearse por lo que se no se muestra su ventana de chats
        }
        else {
            stage.show();
        }

    }


    //CARGAR CHATS EN LA VISTA
    public void loadChats(ChatApplication chatApplication, String emailOrPhoneNumberOrUsername) {
        try {
            //Sacar la id del usuario cuando abra el chat
            id_user = databaseManager.returnId(name);
            //crear todos sus chats la primera vez
            chatApplication.chats = Mediator.createAllChat(chatApplication.chats, emailOrPhoneNumberOrUsername);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    //RECARGAR LAS VISTAS CUANDO SE AGREGA O SE ELIMINA UN CONTACTO
    public void reloadChats(VBox chats, String emailOrPhoneNumberOrUsername) {

        Mediator.createAllChat(chats, emailOrPhoneNumberOrUsername);
    }

    public void loadChatUserOrGroup() {
        // Recorremos cada nodo hijo del VBox "chats"
        for (Node vboxNode : chat.chats.getChildren()) {
            // Verificamos si el nodo es un VBox
            if (vboxNode instanceof VBox) {
                // Si es un VBox, lo convertimos y asignamos a vBox
                VBox vBox = (VBox) vboxNode;
                // Asociamos un evento de clic al VBox
                vBox.setOnMouseClicked(event -> {
                    //Sacamos el nombre del usuario
                    ArrayList<String> nameAndType = Mediator.getChatName(vBox);
                    try {
                        //Si vale contactName es un usuario
                        if (nameAndType.get(0).equals("contactName")) {
                            //Asignamos el tipo
                            messageTo = nameAndType.get(0);

                            contactOrGroupID = databaseManager.returnId(nameAndType.get(1));
                            //Cargamos su chat
                            Mediator.getMessageUserChat(id_user, contactOrGroupID);
                        }
                        //Si vale groupName es un grupo
                        else if (nameAndType.get(0).equals("groupName")) {
                            //Asignamos el tipo
                            messageTo = nameAndType.get(0);

                            contactOrGroupID = databaseManager.getIdGroup(nameAndType.get(1));
                            Mediator.getMessagesGroupChat(id_user, contactOrGroupID);
                        }

                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                });
            }
        }

    }

    //LLAMADA AL MEDIADOR PARA ESCRIBIR UN MENSAJE
    public void writeMessage() {

        Mediator.writeMessage(id_user, contactOrGroupID, chat.writeMessageField.getText(), messageTo);
        chat.writeMessageField.clear();

    }

    //MÉTODO QUE ABRE LA VENTANA DE REGISTRO CUANDO SE PULSA EN REGISTRARSE
    @FXML
    public void launchRegister(ActionEvent event) throws Exception {
        // Cerrar la ventana actual (LOGIN)
        Window window = ((Node) event.getSource()).getScene().getWindow();
        stage = (Stage) window;
        stage.close();

        // Abrir la nueva ventana (REGISTER)
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("register-view.fxml"));
        scene = new Scene(fxmlLoader.load());
        stage.setTitle("¡Regístrate!");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(800);
        stage.setResizable(true);

        // Cargar el icono
        try {
            Image icon = new Image(getClass().getResourceAsStream("/Images/chat-icon.png"));
            // Establecer el icono de la ventana
            stage.getIcons().add(icon);
        } catch (NullPointerException e) {

        }

        stage.show();
    }

    //MÉTODO PARA CERRAR SESIÓN EN LA VENTANA DE CHAT PRINCIPAL
    public void signOff(ActionEvent event) throws Exception {
        //Terminamos de recibir mensajes
        user.close = true;
        //Cerrar hilo usuario
        user.disconnectFromServer();
        threadReceiveMessages.interrupt();

        Mediator.returnToLogin(event, new ChatApplication());

    }

    //METODO PARA REGISTRAR UN USUARIO
    public void register(ActionEvent event) throws Exception {

        Mediator.register(event, username.getText(), mail.getText(), telephone.getText(), password.getText(), new ChatApplication());

    }

    //METODO PARA VOLVER AL LOGIN DESDE LA VISTA DE REGISTRO
    public void returnToLogin(ActionEvent event) {
        try {
            Mediator.returnToLogin(event, new ChatApplication());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //VENTANA DE AÑADIR USUARIO
    public void addUserView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("add-user-group-view.fxml"));
        stage = new Stage();
        Scene scene = new Scene(fxmlLoader.load());
        //Cargar el controlador
        ChatApplication chatApplication = fxmlLoader.getController();
        stage.setTitle("Añadir un usuario");
        stage.setScene(scene);
        stage.setResizable(false);
        //Cambiar el texto
        Mediator.textUser(chatApplication.addUserGroupVbox, chatApplication.addUserOrGroup, chatApplication.writeUserOrGroup,
                chatApplication.buttonAddUserGroup, chatApplication.addMember);
        //Acción del botón
        chatApplication.buttonAddUserGroup.setOnAction(event -> {
            Mediator.addUser(chatApplication.textFieldUserOrGroup, id_user);
            try {
                //Cerrar la ventana de añadir usuario
                Mediator.returnToChat(event);
                //Cargar de nuevo los chats
                reloadChats(chats, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        // pulsar enter y ejecutar el metodo
        chatApplication.textFieldUserOrGroup.setOnAction(event -> {
            Mediator.addUser(chatApplication.textFieldUserOrGroup, id_user);
            try {
                //Cerrar la ventana de añadir usuario
                Mediator.returnToChat(event);
                //Cargar de nuevo los chats
                reloadChats(chat.chats, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        //Cuando haya 2 ventanas, solo se podra utilizar la que haya delante
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);


        // Cargar el icono
        try {
            this.stage.getIcons().add(icon);
        } catch (NullPointerException e) {

        }
        stage.show();
    }

    //VENTANA PARA CREAR UN GRUPO

    public void addGroupView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("add-user-group-view.fxml"));
        stage = new Stage();
        scene = new Scene(fxmlLoader.load());
        //Cargar controlador de la vista
        ChatApplication chatApplication = fxmlLoader.getController();
        stage.setTitle("Crear un grupo");
        stage.setScene(scene);
        stage.setResizable(false);

        //Cambiar el texto
        Mediator.textGroup(chatApplication.addUserGroupHbox, chatApplication.addUserOrGroup, chatApplication.writeUserOrGroup,
                chatApplication.buttonAddUserGroup, chatApplication.textFieldUserOrGroup, chatApplication.addMember);

        //Cuando haya 2 ventanas, solo se podra utilizar la que haya delante
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);

        //Ejecutamos el método añadir grupo
        chatApplication.buttonAddUserGroup.setOnAction(event -> {

            boolean created = Mediator.addGroup(chatApplication.textFieldUserOrGroup.getText(), members);
            if (created) {
                try {
                    //Cerrar la ventana de añadir usuario
                    Mediator.returnToChat(event);
                    //Cargar de nuevo los chats
                    reloadChats(chat.chats, name);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //Si se cierra la ventana, se reinicia el contador de miembros
        stage.setOnCloseRequest(windowEvent -> {
                    members = new ArrayList<>();
                }
        );

        // Cargar el icono
        try {
            this.stage.getIcons().add(icon);
        } catch (NullPointerException e) {

        }
        stage.show();
    }

    public void deleteContactOrGroupView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("add-user-group-view.fxml"));
        stage = new Stage();
        scene = new Scene(fxmlLoader.load());
        //Cargar el controlador
        ChatApplication chatApplication = fxmlLoader.getController();
        stage.setTitle("Borrar un contacto o grupo");
        stage.setScene(scene);
        stage.setResizable(false);

        //Editar el texto de la ventana de borrar
        Mediator.textDeleteContactOrGroup(chatApplication.addUserOrGroup, chatApplication.buttonAddUserGroup,
                chatApplication.addUserGroupVbox,chatApplication.writeUserOrGroup, chatApplication.addMember);

        //Borrar contacto
        chatApplication.buttonAddUserGroup.setOnAction(event -> {
            //Pasamos el contacto que queremos borrar
            Mediator.deleteContact(id_user, chatApplication.textFieldUserOrGroup.getText());
            try {
                //Cerrar la ventana de añadir usuario
                Mediator.returnToChat(event);
                //Cargar de nuevo los chats
                reloadChats(chat.chats, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // pulsar enter y ejecutar el metodo
        chatApplication.textFieldUserOrGroup.setOnAction(event -> {
            //Pasamos el contacto que queremos borrar
            Mediator.deleteContact(id_user, chatApplication.textFieldUserOrGroup.getText());
            try {
                //Cerrar la ventana de añadir usuario
                Mediator.returnToChat(event);
                //Cargar de nuevo los chats
                reloadChats(chat.chats, name);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        //Cuando haya 2 ventanas, solo se podra utilizar la que haya delante
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);

        // Cargar el icono
        try {
            this.stage.getIcons().add(icon);
        } catch (NullPointerException e) {

        }
        stage.show();
    }

    public void deleteAccountView(ActionEvent windowEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("add-user-group-view.fxml"));
        stage = new Stage();
        scene = new Scene(fxmlLoader.load());
        //Cargar el controlador
        ChatApplication chatApplication = fxmlLoader.getController();
        stage.setTitle("Eliminar cuenta");
        stage.setScene(scene);
        stage.setResizable(false);

        //Editar el texto de la ventana de eliminar cuenta
        Mediator.textDeleteAccount(chatApplication.addUserOrGroup, chatApplication.writeUserOrGroup,
                chatApplication.buttonAddUserGroup, chatApplication.addUserGroupVbox, chatApplication.textFieldUserOrGroup, chatApplication.addMember);

        //Eliminar cuenta
        chatApplication.buttonAddUserGroup.setOnAction(event -> {
            //Pasamos el contacto que queremos borrar
            Mediator.deleteAccount();
            try {
                //Cerrar la ventana de eliminar cuenta
                Mediator.returnToChat(event);
                // Cerrar la ventana actual (CHAT)
                Window window = ((Node) windowEvent.getSource()).getScene().getWindow();
                Stage stage = (Stage) window;
                stage.close();
                //Abrimos de nuevo la ventana de Login
                start(stage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        //Cuando haya 2 ventanas, solo se podra utilizar la que haya delante
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.DECORATED);

        // Cargar el icono
        try {
            this.stage.getIcons().add(icon);
        } catch (NullPointerException e) {

        }
        stage.show();
    }

    //MÉTODO PARA CAMBIAR LA IMAGEN DE PERFIL
    public void selectImageProfile() {
        Mediator.selectImageFile(stage);
    }


    public static void main(String[] args) {
        launch();

    }
}