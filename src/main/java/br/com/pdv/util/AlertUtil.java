package br.com.pdv.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Classe utilitária para exibir alertas e diálogos de confirmação.
 */
public class AlertUtil {

    /**
     * Exibe um alerta de informação.
     *
     * @param title Título do alerta
     * @param message Mensagem a ser exibida
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Exibe um alerta de aviso.
     *
     * @param title Título do alerta
     * @param message Mensagem a ser exibida
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Exibe um alerta de erro.
     *
     * @param title Título do alerta
     * @param message Mensagem a ser exibida
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Exibe um diálogo de confirmação com botões Sim e Não.
     *
     * @param title Título do diálogo
     * @param message Mensagem a ser exibida
     * @return true se o usuário confirmar, false caso contrário
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Personalizar botões
        ButtonType buttonYes = new ButtonType("Sim");
        ButtonType buttonNo = new ButtonType("Não");
        
        alert.getButtonTypes().setAll(buttonYes, buttonNo);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == buttonYes;
    }
    
    /**
     * Exibe um diálogo personalizado para exclusão com botões Confirmar e Cancelar.
     *
     * @param title Título do diálogo
     * @param message Mensagem a ser exibida
     * @return true se o usuário confirmar, false caso contrário
     */
    public static boolean showDeleteConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("Confirmação de Exclusão");
        alert.setContentText(message);
        
        // Personalizar botões
        ButtonType buttonConfirm = new ButtonType("Confirmar");
        ButtonType buttonCancel = new ButtonType("Cancelar");
        
        alert.getButtonTypes().setAll(buttonConfirm, buttonCancel);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == buttonConfirm;
    }
    
    /**
     * Exibe um diálogo de entrada de texto.
     *
     * @param title Título do diálogo
     * @param message Mensagem a ser exibida
     * @param defaultValue Valor padrão para o campo de texto
     * @return O texto inserido pelo usuário, ou null se cancelado
     */
    public static String showTextInputDialog(String title, String message, String defaultValue) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}