module org.ielena.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.google.api.client;
    requires com.google.auth;
    requires com.google.api.client.json.jackson2;
    requires google.api.services.drive.v3.rev136;
    requires com.google.auth.oauth2;
    requires java.sql;
    requires net.coobird.thumbnailator;

    opens org.ielena.chat to javafx.fxml;
    exports org.ielena.chat;
}