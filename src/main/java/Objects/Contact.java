package Objects;

import javafx.scene.image.Image;


public class Contact {
    private int id;
    private String name;
    private Image profile_image;
    private boolean approve;

    public Contact(int id, String name, Image profile_image, boolean approve) {
        this.id = id;
        this.name = name;
        this.profile_image = profile_image;
        this.approve = approve;
    }

    public Contact(int id, String name, Image profile_image) {
        this.id = id;
        this.name = name;
        this.profile_image = profile_image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Image getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(Image profile_image) {
        this.profile_image = profile_image;
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }
}
