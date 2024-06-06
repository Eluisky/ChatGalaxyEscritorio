package org.ielena.chat;

import Objects.Contact;
import Objects.Message;
import Objects.User;
import Server.DatabaseManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ielena.chat.ChatApplication.chat;

public class Mediator {
    //nombre usuario
    public static String name;
    //ruta imagen defecto
    static final File defaultImage = new File("src/main/resources/Images/empty-user.png");
    static final Image icon = new Image(ChatApplication.class.getResourceAsStream("/Images/chat-icon.png"));
    public static int contactOrGroupID = 0;
    public static int id_user = 0;
    public static User user;
    public static boolean userError = false;
    public static String messageTo = "imageChanged";

    static final File rememberFile = new File("./src/main/resources/assets/remember.txt");
    public static ArrayList<Integer> members = new ArrayList();
    public static Thread threadReceiveMessages;
    public static final DatabaseManager databaseManager = new DatabaseManager();
    private static ArrayList<Integer> numberOfGroups;

    //LEER EL ARCHIVO SI EXISTE PARA RECORDAR AL USUARIO
    public static ArrayList<TextField> remember(TextField emailOrPhoneNumberOrUsername, TextField loginPassword, CheckBox rememberMe) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(rememberFile));
            //lee la primera linea
            emailOrPhoneNumberOrUsername.setText(reader.readLine());
            //lee la segunda linea
            loginPassword.setText(reader.readLine());
            reader.close();
            //Guardamos en ArrayList
            ArrayList<TextField> fields = new ArrayList<>();
            fields.add(emailOrPhoneNumberOrUsername);
            fields.add(loginPassword);
            //Activamos de nuevo el checkbox
            rememberMe.setSelected(true);
            return fields;
        } catch (IOException e) {
            //System.err.println("El usuario no ha marcado la casilla");
        }
        return null;
    }

    //COMPROBAR QUE TODOS LOS CAMPOS ESTÉN RELLENADOS
    public static boolean checkFields(String username, String mail, String telephone, String password) {
        if (!username.equals("") && !mail.equals("") && !telephone.equals("") && !password.equals("")) {

            return true;
        }
        return false;
    }

    //COMPROBAR EL FORMATO DE CORREO
    public static boolean checkMail(String mail) {
        // Patrón para validar correo electrónico
        String patronCorreo = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(patronCorreo);
        Matcher matcher = pattern.matcher(mail);
        return matcher.matches();
    }

    //COMPROBAR EL FORMATO DEL TELEFONO
    public static boolean checkTelephone(String telephone) {
        // Patrón para validar número de teléfono
        String telephonePattern = "^[0-9]{9}$";
        Pattern pattern = Pattern.compile(telephonePattern);
        Matcher matcher = pattern.matcher(telephone);
        return matcher.matches();
    }

    //COMPROBAR CONTRASEÑA
    public static boolean checkPassword(String password) {
        // Verificar si la contraseña tiene al menos ocho caracteres
        return password.length() >= 8;
    }

    //COMPROBAR QUE NO HAYA CAMPOS VACIOS EN EL LOGIN
    public static boolean checkLoginFields(String loginNameTelephoneMail, String password) {
        if (!loginNameTelephoneMail.equals("") && !password.equals("")) {
            return true;
        }
        return false;
    }

    public static boolean checkSignIn(String emailOrPhoneNumberOrUsername, String loginPassword, CheckBox rememberMe) throws SQLException {
        boolean login;
        // Comprobar que no estén vacíos
        login = Mediator.checkLoginFields(emailOrPhoneNumberOrUsername, loginPassword);

        if (!login) {
            showError("Campos vacíos", "Rellene todos los campos para iniciar sesión");
            return login;
        }

        // Comprobar que existan en la base de datos
        login = databaseManager.checkCredentials(emailOrPhoneNumberOrUsername, loginPassword);

        if (!login) {
            showError("Error al iniciar sesión", "No se ha encontrado una cuenta, compruebe las credenciales escritas");
            return login;
        }

        // Activar el Check si el usuario lo ha pedido
        Mediator.checkCheckBox(rememberMe, emailOrPhoneNumberOrUsername, loginPassword);
        return login;
    }

    //COMPROBAR SI LA CASILLA ESTÁ MARCADA
    public static void checkCheckBox(CheckBox rememberMe, String emailOrPhoneNumberOrUsername, String loginPassword) {
        //Si el usuario marca la casilla Recordar usuario
        if (rememberMe.isSelected()) {
            //Si existe el archivo se borra
            if (rememberFile.exists()) rememberFile.delete();
            try {
                rememberFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //Se escribe en el archivo
            try {
                BufferedWriter write = new BufferedWriter(new FileWriter(rememberFile));
                write.write(emailOrPhoneNumberOrUsername);
                write.write("\n");
                write.write(loginPassword);
                write.close();
            } catch (IOException e) {
            }
        }
        //Si el usuario no marca la casilla, si el archivo existe lo borra
        else {
            if (rememberFile.exists()) rememberFile.delete();
        }
    }

    //METODO PARA REGISTRAR UN USUARIO
    public static void register(ActionEvent event, String username, String mail, String telephone, String password, ChatApplication chatApplication) throws Exception {
        boolean fields;
        boolean checkMail;
        boolean checkTelephone;
        boolean checkPassword;
        boolean checkUsername;

        fields = Mediator.checkFields(username, mail, telephone, password);

        if (fields) {

            checkMail = Mediator.checkMail(mail);
            if (!checkMail) {
                showError("Error ortográfico", "El correo electrónico no está escrito correctamente");
            } else {
                checkMail = databaseManager.checkMail(mail);
                if (!checkMail)
                    showError("Correo existente", "El correo electrónico ya existe en la base de datos con otra cuenta");
            }

            checkTelephone = Mediator.checkTelephone(telephone);
            if (!checkTelephone) {
                showError("Error ortográfico", "El número de teléfono no está escrito correctamente");
            } else {
                checkTelephone = databaseManager.checkTelephone(telephone);
                if (!checkTelephone)
                    showError("Teléfono existente", "El teléfono ya existe en la base de datos con otra cuenta");
            }

            checkPassword = Mediator.checkPassword(password);
            if (!checkPassword) {
                showError("Contraseña mal formulada", "La contraseña debe tener al menos 8 carácteres");
            }

            checkUsername = databaseManager.checkUsername(username);
            if (!checkUsername) {
                showError("Usuario existente", "El nombre de usuario ya existe en la base de datos");
            }

            if (checkMail && checkTelephone && checkPassword && checkUsername) {

                databaseManager.registerUser(defaultImage, password, mail, username, telephone);
                //Avisamos al usuario
                showInformation("Registro correcto", "Gracias por registrarte");
                //Volver al Login
                returnToLogin(event, chatApplication);

            }

        } else {

            showError("Faltan campos", "Debes rellenar todos los campos para registrarte");

        }

    }

    //PREPARAR AL USUARIO QUE INICIA SESION
    public static void prepareUser(TextField emailOrPhoneNumberOrUsername, TextField loginPassword, ChatApplication chatApplication) throws SQLException, IOException {
        // La variable nombre la asignamos con el nombre del usuario que inicia sesión
        name = databaseManager.returnUsername(emailOrPhoneNumberOrUsername.getText(), loginPassword.getText());

        //Asignamos la imagen del usuario
        id_user = databaseManager.returnId(name);
        Contact contact = databaseManager.returnUserById(id_user);
        chat.userImage.setImage(contact.getProfile_image());

        // Asignamos el nombre a la vista de chats
        chatApplication.nameLoggedUser.setText(Mediator.setName(emailOrPhoneNumberOrUsername.getText(), loginPassword.getText()));
        //Crear el hilo
        userThread();
            // Cargar los chats
            chatApplication.loadChats(chatApplication, emailOrPhoneNumberOrUsername.getText());


    }
    public static void userThread(){
        //Creamos el usuario para enviar mensajes
        user = new User(id_user);
        if (userError) {
            try {
                chat.start(new Stage());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            threadReceiveMessages = new Thread(() -> {
                // Continuar recibiendo mensajes mientras receiveMessage devuelva true
                while (user.receiveMessage()) {
                    Platform.runLater(() -> {
                        //Recargar vistas
                        createAllChat(chat.chats, name);
                    });
                }
            });
            threadReceiveMessages.start();
            //Hilo para actualizaciones en la vista
            new Thread(() -> {
                while (user.waitForUpdate()) {
                    Platform.runLater(() -> {
                        //Recargar vistas
                        createAllChat(chat.chats, name);
                    });
                }
            }).start();
        }
    }

    //BUSCAR CONTACTOS QUE TENGA EL USUARIO EN LA VISTA DE CHATS
    public static ArrayList<Integer> searchUser(String searchText, ArrayList<String> contactsName, ArrayList<Integer> allContacts) throws SQLException {

        for (int i = 0; i < allContacts.size(); i++) {
            try {
                contactsName.add(databaseManager.returnUsernameById(allContacts.get(i)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        ArrayList<Integer> contactsSearched = new ArrayList<>();

        // Realizar la búsqueda en la lista actual de contactos
        if (!searchText.equals("")) {
            for (int i = 0; i < contactsName.size(); i++) {
                if (!contactsName.get(i).contains(searchText)) {
                    contactsName.remove(contactsName.get(i));
                    i--;
                }
            }
            for (int i = 0; i < contactsName.size(); i++) {
                contactsSearched.add(databaseManager.returnId(contactsName.get(i)));
            }
        } else {
            contactsSearched.addAll(allContacts);
        }

        return contactsSearched;
    }

    //ACTUALIZAR LA VISTA DE CONTACTOS SEGÚN LO QUE EL USUARIO HA BUSCADO
    public static void loadUserFragments(ArrayList<Integer> contactsSearched, VBox chats) {
        for (int i = 0; i < contactsSearched.size(); i++) {
            try {
                FXMLLoader loader = new FXMLLoader(ChatApplication.class.getResource("user-fragment.fxml"));
                Node chat = loader.load();
                // Obtener el controlador de la vista cargada
                ChatApplication controller = loader.getController();
                String name = databaseManager.returnUsernameById(contactsSearched.get(i));
                controller.contactName.setText(name);
                chats.getChildren().add(chat);
            } catch (IOException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //ESCRIBE EL NOMBRE DE USUARIO EN LA PANTALLA DE CHAT
    public static String setName(String emailOrPhoneNumberOrUsername, String loginPassword) {
        //guardar el nombre de usuario para mostrarlo en la ventana de chat
        try {
            String name = databaseManager.returnUsername(emailOrPhoneNumberOrUsername, loginPassword);
            String welcome = "¡Bienvenido/a " + name + "!";
            return welcome;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //CREAR CHATS PARA LA VISTA DE CHATS DEL USUARIO
    public static VBox createAllChat(VBox chats, String name) {
        ArrayList<Integer> allContacts;
        ArrayList<Integer> allGroups;
        try {
            if (!chats.getChildren().isEmpty()) {
                chats.getChildren().clear();
            }
            // Cargar la vista user-fragment.fxml
            int idUser = databaseManager.returnId(name);
            //Añade a los chats los usuarios que haya agregado y que le hayan agregado al usuario
            allContacts = databaseManager.checkContacts(idUser);
            allContacts.addAll(databaseManager.checkContactsInverse(idUser));
            //Guardamos en la vista principal el numero de chats cargados
            ChatApplication.allContacts = allContacts;

            //Creamos los usuarios
            for (int i = 0; i < allContacts.size(); i++) {
                FXMLLoader loader = new FXMLLoader(Mediator.class.getResource("user-fragment.fxml"));
                Node chat = loader.load();
                // Obtener el controlador de la vista cargada
                ChatApplication controller = loader.getController();
                //Obtenemos el usuario completo
                Contact contact = databaseManager.returnUserById(allContacts.get(i));
                //Lo asignamos a su fragment
                controller.contactName.setText(contact.getName());
                controller.profileImage.setImage(contact.getProfile_image());
                //Sacar el último mensaje
                controller.lastMessage.setText(databaseManager.getLastMessage(id_user, allContacts.get(i)));

                // Agregar las vistas al VBox
                chats.getChildren().addAll(chat);
            }

            //Creamos los grupos
            allGroups = databaseManager.checkUserGroups(idUser);
            if (numberOfGroups == null) numberOfGroups = allGroups;
            if (numberOfGroups.size() != allGroups.size()){
                //Igualar grupos
                numberOfGroups = allGroups;
                //Cerrar hilo
                user.close = true;
                user.disconnectFromServer();
                threadReceiveMessages.interrupt();
                //Abrir uno nuevo con los grupos nuevos
                userThread();
            }
            //Guardamos en la vista principal el numero de chats cargados
            ChatApplication.allContacts.addAll(allGroups);
            //Creamos los grupos
            for (int i = 0; i < allGroups.size(); i++) {
                FXMLLoader loader = new FXMLLoader(Mediator.class.getResource("group-fragment.fxml"));
                Node groups = loader.load();

                // Obtener el controlador de la vista cargada
                ChatApplication controller = loader.getController();
                name = databaseManager.getNameGroup(allGroups.get(i));
                controller.groupName.setText(name);
                //Sacar el último mensaje
                controller.groupLastMessage.setText(databaseManager.getLastMessageGroup(allGroups.get(i)));

                // Agregar las vistas al VBox
                chats.getChildren().addAll(groups);
            }

            return chats;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static ArrayList<String> getChatName(VBox vBox) {
        ArrayList<String> chatNameAndType = new ArrayList<>();
        // Recorremos cada nodo hijo del VBox
        for (Node hboxNode : vBox.getChildren()) {
            // Verificamos si el nodo es un HBox
            if (hboxNode instanceof HBox) {
                // Si es un HBox, lo convertimos y asignamos a hbox
                HBox hbox = (HBox) hboxNode;
                // Recorremos cada nodo hijo del HBox
                for (Node vboxNode2 : hbox.getChildren()) {
                    // Verificamos si el nodo es un VBox
                    if (vboxNode2 instanceof VBox) {
                        // Si es un VBox, lo convertimos y asignamos a vbox2
                        VBox vbox2 = (VBox) vboxNode2;
                        // Recorremos cada nodo hijo del VBox
                        for (Node labelNode : vbox2.getChildren()) {
                            // Verificamos si el nodo es un Label
                            if (labelNode instanceof Label) {
                                // Si es un Label, lo convertimos y asignamos a label
                                Label label = (Label) labelNode;
                                // Verificamos si el Label tiene el id "contactName"
                                if (label.getId().equals("contactName")) {
                                    // Si es el Label buscado, guardamos el nombre
                                    chatNameAndType.add(label.getId());
                                    chatNameAndType.add(label.getText());
                                }
                                //Si no se encuentra ninguno, es posible que sea un grupo
                                else if (label.getId().equals("groupName")) {
                                    // Si es el Label buscado, guardamos el nombre
                                    chatNameAndType.add(label.getId());
                                    chatNameAndType.add(label.getText());
                                }
                            }
                        }
                    }
                }
            }
        }
        return chatNameAndType;
    }

    //CARGAR EL CHAT DEL USUARIO ELEGIDO
    public static ArrayList<Message> getMessageUserChat(int idUser, int idContact) {

        try {
            ArrayList<Message> messages = databaseManager.getChatMessages(idUser, idContact);
            // Crear una lista observable para los mensajes
            ObservableList<Message> observableMessages = FXCollections.observableArrayList(messages);
            //Cargamos los mensajes en el chat
            loadChat(idUser, observableMessages);
            //Devolver la lista con los mensajes para el listView para escribirlos cuando un usuario ha recibido un mensaje
            return messages;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //CARGAR EL CHAT DEL GRUPO ELEGIDO
    public static ArrayList<Message> getMessagesGroupChat(int idUser, int idGroup) {

        try {
            ArrayList<Message> messages;
            synchronized (user) {
                messages = databaseManager.getChatMessagesGroup(idGroup);
            }

            // Crear una lista observable para los mensajes
            ObservableList<Message> observableMessages = FXCollections.observableArrayList(messages);
            //Cargamos los mensajes en el chat
            loadChat(idUser, observableMessages);
            //Devolver la lista con los mensajes para el listView para escribirlos cuando un usuario ha recibido un mensaje
            return messages;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadChat(int idUser, ObservableList<Message> observableMessages) {
        // Ejecutar la configuración del ListView en el hilo de la aplicación JavaFX
        Platform.runLater(() -> {
            // Configurar el ListView para usar celdas personalizadas
            chat.chatView.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>() {
                @Override
                public ListCell<Message> call(ListView<Message> param) {
                    return new ListCell<Message>() {
                        @Override
                        protected void updateItem(Message item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setGraphic(null);
                                setStyle("-fx-background-color: transparent;");
                            } else {
                                setText(item.getTextMessage());
                                //Aqui se diseña el texto del listView
                                // Alinear el texto a la izquierda si el mensaje fue enviado por el usuario actual
                                if (item.getUserId() == idUser) {
                                    setStyle("-fx-alignment: center-right;-fx-background-color: white; -fx-background-radius: 10px;" +
                                            "-fx-font-size: 18px;-fx-font-weight: bold;-fx-background-color: #8A2BE2;" +
                                            "-fx-padding: 10px;-fx-text-fill: white");
                                } else {
                                    // Alinear el texto a la derecha si el mensaje fue enviado por el otro usuario
                                    setStyle("-fx-alignment: center-left;-fx-background-color: white; -fx-background-radius: 10px;" +
                                            "-fx-font-size: 18px;-fx-font-weight: bold;-fx-background-color: #66CDAA;" +
                                            "-fx-padding: 10px;-fx-text-fill: white");
                                }
                            }
                        }
                    };
                }
            });

            // Establecer la lista observable en el ListView
            chat.chatView.setItems(observableMessages);
            // Desplazar el ListView hacia el final de la lista para mostrar los últimos mensajes
            if (!observableMessages.isEmpty()) {
                chat.chatView.scrollTo(observableMessages.size() - 1);
            }
        });
    }

    //MÉTODO PARA ESCRIBIR MENSAJES
    public static void writeMessage(int idUser, int contactIdOrGroup, String text, String type) {
        try {
            //Contactos
            if (!text.equals("") && text != null && type.equals("contactName")) {
                //Se guarda el mensaje
                synchronized (user) {
                    databaseManager.sendMessage(idUser, contactIdOrGroup, text);
                }
                //Grupos del contacto
                ArrayList<Integer> groupsContact = databaseManager.checkUserGroups(contactOrGroupID);
                //Recargar vistas
                createAllChat(chat.chats, name);
                getMessageUserChat(idUser, contactIdOrGroup);
                //El usuario envia el mensaje al contacto deseado
                user.sendMessage(contactOrGroupID, groupsContact, text);
            }
            //Grupos
            else if (!text.equals("") && text != null && type.equals("groupName")) {
                //Añadimos a mensaje el nombre del usuario para distinguir
                text = name + ": " + text;
                //Se guarda el mensaje
                synchronized (user) {
                    databaseManager.sendMessageGroup(idUser, contactIdOrGroup, text);
                }
                //Recargar vistas
                createAllChat(chat.chats, name);
                getMessagesGroupChat(idUser, contactIdOrGroup);
                //El usuario envia el mensaje al grupo deseado
                user.sendGroup(contactOrGroupID, text);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //VENTANA PARA AÑADIR UN USUARIO
    public static void textUser(VBox addUserGroupVbox, Label addUserOrGroup, Label writeUserOrGroup, Button buttonAddUserGroup, TextField addMember) {
        addUserOrGroup.setText("Añadir un usuario");
        writeUserOrGroup.setText("Escriba el usuario que quiera añadir\n(Nombre, teléfono o correo electrónico)");
        //textFieldUserOrGroup.setPromptText("Nombre, teléfono o correo electrónico");
        buttonAddUserGroup.setText("Añadir usuario");
        //Quitamos el textfield de grupo
        addUserGroupVbox.getChildren().remove(addMember);
    }

    //MÉTODO QUE AÑADE UN USUARIO
    public static boolean addUser(TextField newUser, int idUser) {
        int idContactUser = 0;
        //Comprobar que existe
        try {
            idContactUser = databaseManager.checkUserExists(newUser.getText());
            if (idContactUser == id_user) {
                showError("Error al agregar un usuario", "No puedes agregarte a ti mismo");
                return false;
            }
        } catch (SQLException e) {

        }
        //Si no existe, se avisa al usuario, y si sucedió un error en la consulta también
        if (idContactUser == 0) showError("Error de conexión", "Error de conexión al intentar agregar un contacto");
        if (idContactUser == -1) showError("No existe el usuario", "El usuario escrito no existe");
            //Si existe se comprueba si esta agregado
        else {

            boolean isAdded;
            try {
                //Comprobamos en ambas direcciones si tiene un contacto que agregó o un contacto del que fue agregado
                isAdded = databaseManager.checkContactAdded(idUser, idContactUser);
                if (!isAdded) isAdded = databaseManager.checkContactAddedInverse(idUser, idContactUser);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            //Si no esta agregado se añade al usuario
            if (!isAdded) {

                try {

                    databaseManager.addContactToUser(idUser, idContactUser);
                    //Avisamos al usuario
                    showInformation("Añadido correctamente", "Usuario Añadido correctamente");
                    //Avisar a notifyUser que se ha agregado el contacto correctamente
                    return true;

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            //si no se muestra una ventana de error de que ya esta agregado
            else showError("Usuario ya agregado", "Este usuario ya está en tus contactos");

        }
        return false;
    }

    //MÉTODO QUE CREA UN GRUPO Y LOS USUARIOS ESCOGIDOS
    public static boolean addGroup(String nameGroup, ArrayList<Integer> members) {
        //Comprobar que se haya agregado algún miembro
        if (members.size() > 0) {

            if (nameGroup != null && !nameGroup.isEmpty()) {
                //Comprobar que el grupo no se llame como un usuario
                boolean existsGroupName = databaseManager.checkNameGroup(nameGroup);
                boolean existsUserName = databaseManager.checkUsername(nameGroup);
                if (!existsUserName || existsGroupName) {
                    showError("Nombre de grupo no váido", "Este nombre no está disponible, seleccione otro");
                    return false;
                }
                try {
                    //Se cuentan los grupos y se añade uno, que va a ser la id nueva del nuevo grupo
                    int idGroup = databaseManager.checkGroups() + 1;
                    //Se crea el grupo con el usuario que lo crea
                    databaseManager.createGroup(nameGroup, idGroup, id_user);
                    //Después el resto de miembros
                    for (int member : members) {
                        databaseManager.createGroup(nameGroup, idGroup, member);
                    }
                    //Se cierra con un mensaje de aviso
                    showInformation("Grupo creado correctamente", "El grupo se ha creado correctamente");
                    //Volver a hacer un hilo de usuario con el nuevo grupo
                    user.close = true;
                    user.disconnectFromServer();
                    threadReceiveMessages.interrupt();
                    userThread();
                    return true;


                } catch (SQLException e) {
                    showError("Error de conexión", "Ocurrió un error al acceder a la base de datos");
                }

            } else showError("Nombre de grupo vacío", "El nombre del grupo no puede estar vacio");
        } else showError("No se ha agregado a nadie", "No se puede crear un grupo vacío, agregue primero un contacto");


        return false;
    }

    private static boolean addUserToGroup(String newMember) {
        int idMember = 0;
        try {
            //Comprobar que el usuario exista
            idMember = databaseManager.checkUserExists(newMember);
            if (newMember == null || newMember.isEmpty()) {
                showError("Usuario no existente",
                        "Este usuario no existe, compruebe el nombre y su lista de contactos, el campo tampoco puede estar vacío");
                return false;
            }
            //Se comprueba que el usuario este agregado o que le agregaron
            if (databaseManager.checkContactAdded(id_user, idMember) || databaseManager.checkContactAddedInverse(id_user, idMember)) {
                //Que no se haya añadido anteriormente
                for (int member : members) {
                    if (member == idMember) {
                        showError("Usuario ya preparado",
                                "Este usuario ya está preparado para ser agregado al grupo");
                        return false;
                    }

                }
                members.add(idMember);
                showInformation("Usuario agregado", "Usuario preparado para el grupo, puede crear el grupo o escribir más usuarios");
                return true;

            } else {
                showError("Usuario no existente",
                        "Este usuario no existe, compruebe el nombre y su lista de contactos, el campo tampoco puede estar vacío");
                return false;
            }

        } catch (SQLException e) {
            showError("Error",
                    "Ha ocurrido un error al acceder a la base de datos");
            return false;
        }
    }

    //CAMBIAR EL TEXTO PARA BORRAR UN CONTACTO
    public static void textDeleteContactOrGroup(Label addUserOrGroup, Button buttonAddUserGroup, VBox addUserGroupVbox,Label writeUserOrGroup, TextField addMember) {
        addUserOrGroup.setText("Escriba el nombre del contacto o grupo que desee borrar");
        addUserGroupVbox.getChildren().remove(addMember);
        addUserGroupVbox.getChildren().remove(writeUserOrGroup);
        buttonAddUserGroup.setText("Eliminar contacto o grupo");
    }

    //MÉTODO PARA ELIMINAR UN CONTACTO Y COMPROBAR SI ES UN GRUPO
    public static boolean deleteContact(int idUser, String contactToBeDeleted) {
        int idToDelete = -2;
        //Comprobar que existe
        try {
            idToDelete = databaseManager.checkUserExists(contactToBeDeleted);
            //Si no lo encuentra, busca si es un grupo
            if (idToDelete == -2 || idToDelete == -1) {

                idToDelete = databaseManager.getIdGroup(contactToBeDeleted);
                //Si es distinto de 0, es que se quiere borrar un grupo
                if (idToDelete != -2 && idToDelete != -1) {
                    deleteGroup(idToDelete, contactToBeDeleted);
                    return true;
                }
            }
        } catch (SQLException e) {

        }
        //Si no existe, se avisa al usuario, y si sucedió un error en la consulta también
        if (idToDelete == -2)
            showError("Error de conexión", "Error de conexión al intentar borrar un contacto o un grupo");
        if (idToDelete == -1)
            showError("No existe el usuario o el grupo", "No se ha encontrado el contacto o el grupo");
            //Si existe se comprueba si esta agregado
        else {

            boolean isAdded;

            try {
                //Comprobamos en ambas direcciones si tiene un contacto que agregó o un contacto del que fue agregado
                isAdded = databaseManager.checkContactAdded(idUser, idToDelete);
                if (!isAdded) isAdded = databaseManager.checkContactAddedInverse(idUser, idToDelete);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            //Si  está agregado se borra al usuario
            if (isAdded) {

                databaseManager.deleteContact(idUser, idToDelete);
                //Avisamos al usuario
                showInformation("Eliminado correctamente", "Contacto eliminado correctamente");
                //Avisamos a notifyDeleteContact que se ha borrado el contacto
                return true;

            }
            //si no se muestra una ventana de error de que ya esta agregado
            else showError("No existe el usuario", "No se ha encontrado el contacto");

        }
        return false;
    }

    //ELIMINAR GRUPO
    public static boolean deleteGroup(int groupToBeDeleted, String nameGroup) {
        try {
            databaseManager.deleteFromGroup(id_user, groupToBeDeleted);
            //Avisamos al usuario
            showInformation("Eliminado correctamente", "Grupo borrado correctamente");
            //Comprobar si ese grupo está vacío para eliminarlo
            int numberOfUsers = databaseManager.checkUsersOnGroups(nameGroup);
            if (numberOfUsers == 0){
                databaseManager.deleteChatsofGroup(groupToBeDeleted);
            }
            //Avisamos a notifyDeleteContact que se ha borrado el contacto
        } catch (Exception e) {
            showError("Error de conexion", "Error de conexion al intentar eliminar el grupo");
            return false;
        }


        return true;
    }

    //CAMBIAR EL TEXTO PARA ELIMINAR LA CUENTA
    public static void textDeleteAccount(Label addUserOrGroup, Label writeUserOrGroup, Button buttonAddUserGroup,
                                         VBox addUserGroupVbox, TextField textFieldUserOrGroup, TextField addMember) {
        addUserOrGroup.setText("¿Estás seguro que deseas borrar tu cuenta?");
        writeUserOrGroup.setText("Esta acción no se puede deshacer");
        addUserGroupVbox.getChildren().remove(addMember);
        addUserGroupVbox.getChildren().remove(textFieldUserOrGroup);
        buttonAddUserGroup.setText("Eliminar cuenta");
    }

    //ELIMINAR LA CUENTA TOTALMENTE DE LA BASE DE DATOS
    public static void deleteAccount() {
        databaseManager.deleteUser(id_user);
        databaseManager.deleteUserContact(id_user);
        //Mostrar información por pantalla
        showInformation("Cuenta eliminada", "¡Esperamos volver a verte pronto!");
    }

    //CAMBIAR EL TEXTO PARA AÑADIR GRUPOS
    public static void textGroup(HBox addUserGroupHbox, Label addUserOrGroup, Label writeUserOrGroup, Button buttonAddUserGroup,
                                 TextField textFieldUserOrGroup, TextField addMember) {
        //Crear boton nuevo
        Button add = new Button();
        addUserOrGroup.setText("Crear un grupo");
        writeUserOrGroup.setText("Escriba el nombre del grupo que quieras crear");
        textFieldUserOrGroup.setPromptText("Nombre del nuevo grupo");
        buttonAddUserGroup.setText("Crear grupo");
        //Añadir texto y estilo a los nuevos botones
        add.setText("Añadir usuario");
        add.getStylesheets().add("file:src/main/resources/css/buttonStyle.css");
        //Añadir texto y estilo
        //Añadir boton
        addUserGroupHbox.getChildren().add(add);
        add.setOnAction(event -> {
            addUserToGroup(addMember.getText());
            addMember.setText("");
        });
    }

    //MÉTODO QUE VUELVE AL LOGIN SI EN LA VENTANA DE REGISTRO QUIERE VOLVER HACIA ATRÁS
    public static void returnToLogin(ActionEvent event, ChatApplication chatApplication) throws Exception {
        // Cerrar la ventana actual (REGISTER)
        Window window = ((Node) event.getSource()).getScene().getWindow();
        Stage stage = (Stage) window;
        stage.close();
        //Volver al Login
        chatApplication.start(stage);
    }

    public static void returnToChat(ActionEvent event) {
        // Cerrar la ventana actual (Añadir Usuario o Grupo)
        Window window = ((Node) event.getSource()).getScene().getWindow();
        Stage stage = (Stage) window;
        stage.close();
    }

    //MÉTODO PARA CREAR VENTANAS DE ALERTA
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);

        alert.showAndWait();
    }

    //MÉTODO PARA CREAR VENTANAS DE INFORMACION AL USUARIO
    public static void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información para el usuario");
        alert.setHeaderText(title);
        alert.setContentText(message);

        alert.showAndWait();
    }

    //MÉTODO PARA CAMBIAR LA IMAGEN DE PERFIL
    public static void selectImageFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();

        // Configurar el filtro para que solo muestre imágenes
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.jpg", "*.jpeg", "*.png", "*.webp");

        fileChooser.getExtensionFilters().add(imageFilter);

        // Abrir el diálogo del selector de archivos
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {

            boolean changed = databaseManager.changeImage(selectedFile, id_user);

            if (changed) {

                messageTo = "imageChanged";
                showInformation("Actualizado correctamente", "Imagen de perfil actualizada correctamente");

                //Asignamos la imagen del usuario
                int id;
                try {
                    id = databaseManager.returnId(name);
                    Contact contact = databaseManager.returnUserById(id);
                    chat.userImage.setImage(contact.getProfile_image());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

            } else showError("Error al actualizar imagen", "No se ha podido actualizar la imagen de perfil");
        }
    }

    //REDIMENSIONAR IMAGEN AL SUBIRLA A LA BASE DE DATOS
    public static byte[] resizeImageToBytes(File originalImageFile, int targetWidth, int targetHeight) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            Thumbnails.of(originalImageFile)
                    .size(targetWidth, targetHeight)
                    .toOutputStream(baos);

        } catch (UnsupportedFormatException e) {
            showError("Imagen no soportada", "Utilice imagenes .jpg, jpeg y png para la imagen de perfil");
            return null;
        }

        return baos.toByteArray();
    }

}
