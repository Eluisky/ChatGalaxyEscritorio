package Server;


import Objects.Contact;
import javafx.scene.image.Image;
import Objects.Message;
import org.ielena.chat.Mediator;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager {
    private  final String DATABASE_URL = "jdbc:mysql://34.175.206.223:3306/chat_database";
    private  final String USERNAME = "remote_user";
    private  final String PASSWORD = "password";

    // MÉTODO PARA REGISTRAR UN USUARIO EN LA BASE DE DATOS
    public boolean registerUser(File profileImage, String password, String mail, String name, String telephoneNumber) {
        boolean connect;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD)) {
            String sql = "INSERT INTO user (profile_image, password, mail, name, telephone_number) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                try (InputStream inputStream = new FileInputStream(profileImage)) {
                    statement.setBlob(1, inputStream);
                    statement.setString(2, password);
                    statement.setString(3, mail);
                    statement.setString(4, name);
                    statement.setString(5, telephoneNumber);

                    statement.executeUpdate();
                    connect = true;
                }
            }
        } catch (SQLException | IOException e) {
            connect = false;
            e.printStackTrace();
        }
        return connect;
    }

    public boolean checkUsername(String username) {
        String query = "SELECT COUNT(*) FROM user WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                return count < 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkMail(String mail) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE mail = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, mail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count < 1;
                }
            }
        }
        return false;
    }

    public  boolean checkTelephone(String telephone) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE telephone_number = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, telephone);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count < 1;
                }
            }
        }
        return false;
    }

    public boolean checkCredentials(String emailOrPhoneNumberOrUsername, String password) throws SQLException {
        String query = "SELECT COUNT(*) FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?) AND password = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, emailOrPhoneNumberOrUsername);
            statement.setString(2, emailOrPhoneNumberOrUsername);
            statement.setString(3, emailOrPhoneNumberOrUsername);
            statement.setString(4, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    public String returnUsername(String emailOrPhoneNumberOrUsername, String password) throws SQLException {
        String query = "SELECT name FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?) AND password = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, emailOrPhoneNumberOrUsername);
            statement.setString(2, emailOrPhoneNumberOrUsername);
            statement.setString(3, emailOrPhoneNumberOrUsername);
            statement.setString(4, password);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }

    public int checkUserExists(String user) throws SQLException {
        int userId = -1;
        String query = "SELECT id FROM user WHERE name=? OR telephone_number=? OR mail=?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, user);
            statement.setString(2, user);
            statement.setString(3, user);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    userId = resultSet.getInt("id");
                }
            }
        }
        return userId;
    }

    //CREAR GRUPO Y AÑADIR AL GRUPO USUARIOS
    public boolean createGroup(String nameGroup, int idGroup, int idUser) throws SQLException {
        String query = "INSERT INTO groups ( id_group, id_user, name) VALUES (?,?,?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, idGroup);
            preparedStatement.setInt(2, idUser);
            preparedStatement.setString(3, nameGroup);
            int rowsInserted = preparedStatement.executeUpdate();
            return rowsInserted > 0;
        }
    }

    public String getNameGroup(int idGroup) throws SQLException {
        String query = "SELECT name FROM groups WHERE id_group = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, idGroup);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
            return "";
        }
    }
    public int getIdGroup(String name) throws SQLException {
        String query = "SELECT id_group FROM groups WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
            return -1;
        }
    }
    public boolean checkNameGroup(String name){
        String query = "SELECT name FROM user WHERE name = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int checkGroups() throws SQLException {
        int groups = 0;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM groups ORDER BY id DESC");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                groups = resultSet.getInt(1);
            }
        }
        return groups;
    }

    public ArrayList<Integer> checkUserGroups(int idUser) throws SQLException {
        ArrayList<Integer> groups = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id_group FROM groups WHERE id_user = ?")) {
            statement.setInt(1, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    groups.add(resultSet.getInt(1));
                }
            }
        }
        return groups;
    }
    //COMPROBAR LOS USUARIOS DE UN GRUPO
    public Integer checkUsersOnGroups(String name) throws SQLException {
        int numberOfUsers = 0;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id_user FROM groups WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    numberOfUsers = 1;
                    return numberOfUsers;
                }
            }
        }
        return numberOfUsers;
    }

    public boolean deleteFromGroup(int idUser, int idGroup) {
        String sql = "DELETE FROM groups WHERE (id_user = ? AND id_group = ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUser);
            pstmt.setInt(2, idGroup);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    //BORRAR LOS CHATS DE UN GRUPO
    public void deleteChatsofGroup(int groupId) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM chat_group WHERE id_group = ?")) {

            statement.setInt(1, groupId);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean checkContactAdded(int userId, int contactId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM contact WHERE id_user_add_contact=? AND id_contact_added=?")) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public  boolean checkContactAddedInverse(int userId, int contactId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM contact WHERE id_contact_added=? AND id_user_add_contact=?")) {
            statement.setInt(1, userId);
            statement.setInt(2, contactId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    public  ArrayList<Integer> checkContacts(int userId) throws SQLException {
        ArrayList<Integer> contacts = new ArrayList<>();
        String query = "SELECT id_contact_added FROM contact WHERE id_user_add_contact = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contacts.add(resultSet.getInt(1));
                }
            }
        }
        return contacts;
    }

    public  ArrayList<Integer> checkContactsInverse(int userId) throws SQLException {
        ArrayList<Integer> contacts = new ArrayList<>();
        String query = "SELECT id_user_add_contact FROM contact WHERE id_contact_added = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    contacts.add(resultSet.getInt(1));
                }
            }
        }
        return contacts;
    }

public  boolean addContactToUser(int userId, int contactId) throws SQLException {
    String query = "INSERT INTO contact (id_user_add_contact, id_contact_added) VALUES (?, ?)";
    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setInt(1, userId);
        statement.setInt(2, contactId);
        int rowsInserted = statement.executeUpdate();
        return rowsInserted > 0;
    }
}

public  int returnId(String name) throws SQLException {
    String query = "SELECT id FROM user WHERE (mail = ? OR telephone_number = ? OR name = ?)";
    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setString(1, name);
        statement.setString(2, name);
        statement.setString(3, name);
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
    }
    return 0;
}

public  String returnUsernameById(int id) throws SQLException {
    String query = "SELECT name FROM user WHERE id = ?";
    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setInt(1, id);
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
    }
    return "";
}

public  Contact returnUserById(int id) throws SQLException {
    String query = "SELECT id, name, profile_image FROM user WHERE id = ?";
    Contact contact = null;
    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setInt(1, id);
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                int userId = resultSet.getInt("id");
                String name = resultSet.getString("name");
                byte[] imageBytes = resultSet.getBytes("profile_image");
                InputStream inputStream = new ByteArrayInputStream(imageBytes);
                Image profileImage = new Image(inputStream);
                contact = new Contact(userId, name, profileImage);
                return contact;
            }
        }
    }
    return contact;
}

    //SACAR EL ULTIMO MENSAJE ENTRE 2 USUARIOS
    public String getLastMessage(int idUser, int id_contact) throws SQLException {
        String query = "SELECT text_message FROM chat WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?) ORDER BY id DESC";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idUser);
            statement.setInt(2, id_contact);
            statement.setInt(3, id_contact);
            statement.setInt(4, idUser);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return "";
    }

public String getLastMessageGroup(int id_group) throws SQLException {
    String query = "SELECT text_message FROM chat_group WHERE id_group = ? ORDER BY id DESC";

    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {

        statement.setInt(1, id_group);

        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
    }
    return "";
}

public boolean deleteContact(int idUser, int contactId) {
    String sql = "DELETE FROM contact WHERE (id_user_add_contact = ? AND id_contact_added = ?) OR (id_user_add_contact = ? AND id_contact_added = ?)";
    try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, idUser);
        pstmt.setInt(2, contactId);
        pstmt.setInt(3, contactId);
        pstmt.setInt(4, idUser);
        int rowsDeleted = pstmt.executeUpdate();
        return rowsDeleted > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public boolean deleteUser(int idUser) {
    String sql = "DELETE FROM user WHERE id = ?";
    try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, idUser);
        int rowsDeleted = pstmt.executeUpdate();
        return rowsDeleted > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public boolean deleteUserContact(int idUser) {
    String sql = "DELETE FROM contact WHERE id_user_add_contact = ? OR id_contact_added = ?";
    try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, idUser);
        pstmt.setInt(2, idUser);
        int rowsDeleted = pstmt.executeUpdate();
        return rowsDeleted > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}

public ArrayList<Message> getChatMessages(int user_id, int contact_id) throws SQLException {
    ArrayList<Message> messages = new ArrayList<>();
    String query = "SELECT user_id, contact_id, text_message FROM chat WHERE (user_id = ? AND contact_id = ?) OR (user_id = ? AND contact_id = ?)";

    try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setInt(1, user_id);
        stmt.setInt(2, contact_id);
        stmt.setInt(3, contact_id);
        stmt.setInt(4, user_id);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            int userId = rs.getInt("user_id");
            int contactId = rs.getInt("contact_id");
            String textMessage = rs.getString("text_message");
            messages.add(new Message(userId, contactId, textMessage));
        }
    }
    return messages;
}

public ArrayList<Message> getChatMessagesGroup( int group_id) throws SQLException {
    ArrayList<Message> messages = new ArrayList<>();
    String query = "SELECT id_user, id_group, text_message FROM chat_group WHERE id_group = ?";

    try (Connection conn = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setInt(1, group_id);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            int userId = rs.getInt("id_user");
            int contactId = rs.getInt("id_group");
            String textMessage = rs.getString("text_message");
            messages.add(new Message(userId, contactId, textMessage));
        }
    }
    return messages;
}

public synchronized void sendMessage(int userId, int contactId, String textMessage) throws SQLException {
    String query = "INSERT INTO chat (user_id, contact_id, text_message) VALUES (?, ?, ?)";

    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement statement = connection.prepareStatement(query)) {
        statement.setInt(1, userId);
        statement.setInt(2, contactId);
        statement.setString(3, textMessage);

        statement.executeUpdate();
    }
}
    //Método para enviar un mensaje a un grupo
    public synchronized void sendMessageGroup(int userId, int groupId, String textMessage) throws SQLException {
        String query = "INSERT INTO chat_group (id_user, id_group, text_message) VALUES (?, ?, ?)";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, userId);
            statement.setInt(2, groupId);
            statement.setString(3, textMessage);
            statement.executeUpdate();
        }
    }

public int countMessages(int userId) {
    int messageCount = 0;
    String query1 = "SELECT COUNT(*) FROM chat WHERE user_id = ? OR contact_id = ?";
    String query2 = "SELECT COUNT(*) FROM chat_group WHERE id_group IN (SELECT id_group FROM chat_group WHERE id_user = ?)";
    try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
         PreparedStatement stmt1 = connection.prepareStatement(query1);
         PreparedStatement stmt2 = connection.prepareStatement(query2)) {
        stmt1.setInt(1, userId);
        stmt1.setInt(2, userId);
        ResultSet rs1 = stmt1.executeQuery();
        if (rs1.next()) {
            messageCount += rs1.getInt(1);
        }
        stmt2.setInt(1, userId);
        ResultSet rs2 = stmt2.executeQuery();
        if (rs2.next()) {
            messageCount += rs2.getInt(1);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return messageCount;
}

    //CAMBIAR IMAGEN PERFIL
    public boolean changeImage(File image, int idUser) {
        String query = "UPDATE user SET profile_image = ? WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(query)){

            // Convertir la imagen a un array de bytes y redimensionarla
            byte[] imageBytes = Mediator.resizeImageToBytes(image, 100, 100);
            if (imageBytes == null) return false;

            // Establecer los bytes de la imagen en la consulta preparada
            stmt.setBytes(1, imageBytes);
            stmt.setInt(2, idUser);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}