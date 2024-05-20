package com.example.JavaTeacherBot.service;

import org.springframework.beans.factory.annotation.Value;

class Algorithm {
    @Value("${name}")
    String name;
    @Value("${description}")
    String description;
    @Value("${imageUrl}")
    String imageUrl;
    @Value("${descriptionCode}")
    String descriptionCode;


    public Algorithm(String name, String description, String imageUrl, String descriptionCode) { //конструктор
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.descriptionCode = descriptionCode;
    }

    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getDescriptionCode() { return descriptionCode; }
}