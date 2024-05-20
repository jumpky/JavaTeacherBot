package com.example.JavaTeacherBot.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class DatabaseManager {

    @Autowired
    private Environment env;

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public DatabaseManager(Environment env) {
        this.env = env;
        this.dbUrl = env.getProperty("database.dbUrl");
        this.dbUser = env.getProperty("database.dbUser");
        this.dbPassword = env.getProperty("database.dbPassword");
    }

    public Algorithm getAlgorithm(String algorithmName, String language) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, description, image_url, " + language + "_code AS code FROM algorithms WHERE name = ?"
             )) {
            statement.setString(1, algorithmName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Algorithm(
                            resultSet.getString("name"),
                            resultSet.getString("description"),
                            resultSet.getString("image_url"),
                            resultSet.getString("code")
                    );
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // возвращаем 0 если алгоритм не найден
    }

    public List<String> getAlgorithmNames() {
        List<String> algorithmNames = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM algorithms")) {
            while (resultSet.next()) {
                algorithmNames.add(resultSet.getString("name"));
            }
        }
        catch (SQLException e) {
            // Handle SQL exceptions and logging
            e.printStackTrace();
        }
        return algorithmNames;
    }


    public void updateUserLanguage(long chatId, String language) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO Settings (chatId, language) VALUES (?, ?) ON DUPLICATE KEY UPDATE language = ?"
             )) {
            statement.setLong(1, chatId);
            statement.setString(2, language);
            statement.setString(3, language);
            statement.executeUpdate();
        }
        catch (SQLException e) {
            // Handle SQL exceptions and logging
            e.printStackTrace();
        }
    }


    public String getUserLanguage(long chatId) {
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT language FROM Settings WHERE chatId = ?"
             )) {
            statement.setLong(1, chatId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("language");
                }
            }
        }
        catch (SQLException e) {
            // Handle SQL exceptions and logging
            e.printStackTrace();
        }
        return "java"; // Default language if not found in settings
    }
}
