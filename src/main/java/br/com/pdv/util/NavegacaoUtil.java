package br.com.pdv.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavegacaoUtil {
	public static void navegarPara(String fxmlPath, Node nodeOrigem, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavegacaoUtil.class.getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) nodeOrigem.getScene().getWindow();
            
            // Preservar dimensões e estado
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean maximized = stage.isMaximized();
            
            Scene novaCena = new Scene(root, width, height);
            stage.setScene(novaCena);
            stage.setTitle(titulo);
            stage.setMaximized(maximized);
            
        } catch (IOException e) {
            e.printStackTrace();
            // Exibir alerta de erro
        }
    }
}
