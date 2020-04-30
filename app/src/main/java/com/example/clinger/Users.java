package com.example.clinger;

public class Users {

    public String name;
    public String image;
    public String status;
    public String thumb_image;

    public Users(){

    }




    public Users(String name, String image, String status, String thumb) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.thumb_image = thumb;
    }



    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getStatus() {
        return status;
    }


    public String getThumb_image() {
        return thumb_image;
    }

    public void setThumb_image(String thumb_image) {
        this.thumb_image = thumb_image;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}
