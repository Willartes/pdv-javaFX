package br.com.pdv.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PDVApplication extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/br/com/pdv/gui/views/MainView.fxml"));
        
        
        // Definir tamanho personalizado
        Scene scene = new Scene(root, 1300, 700);
        
        
        primaryStage.setTitle("PDV - Sistema de Ponto de Venda");
        primaryStage.setScene(scene);
        
        // Opcionalmente definir tamanho mínimo
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        
        primaryStage.setMaximized(false);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}