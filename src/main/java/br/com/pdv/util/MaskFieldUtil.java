package br.com.pdv.util;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.control.TextField;

/**
 * Classe utilitária para aplicar máscaras em campos de texto
 */
public class MaskFieldUtil {

    /**
     * Aplica máscara de CPF (###.###.###-##)
     * 
     * @param textField O campo que receberá a máscara
     */
    public static void cpfMask(TextField textField) {
        // Aplica a máscara padrão para CPF
        maskField(textField, "###.###.###-##");

        // Limitando a entrada a 14 caracteres (com pontuação)
        maxField(textField, 14);
    }
    
    /**
     * Aplica máscara de telefone celular ((##) #####-####)
     * 
     * @param textField O campo que receberá a máscara
     */
    public static void foneCell(TextField textField) {
        // Aplica a máscara padrão para telefone celular
        maskField(textField, "(##) #####-####");

        // Limitando a entrada a 15 caracteres (com pontuação)
        maxField(textField, 15);
    }
    
    /**
     * Aplica máscara de CEP (#####-###)
     * 
     * @param textField O campo que receberá a máscara
     */
    public static void cepMask(TextField textField) {
        // Aplica a máscara padrão para CEP
        maskField(textField, "#####-###");

        // Limitando a entrada a 9 caracteres (com pontuação)
        maxField(textField, 9);
    }
    
    /**
     * Define um número máximo de caracteres para o campo
     * 
     * @param textField O campo que terá limite
     * @param length O número máximo de caracteres
     */
    private static void maxField(TextField textField, Integer length) {
        final AtomicBoolean updating = new AtomicBoolean(false);
        
        textField.textProperty().addListener((observableValue, oldValue, newValue) -> {
            // Se já está atualizando ou o valor é nulo, retornar
            if (updating.get() || newValue == null) {
                return;
            }
            
            // Se excedeu o tamanho máximo
            if (newValue.length() > length) {
                updating.set(true);
                try {
                    textField.setText(oldValue);
                } finally {
                    updating.set(false);
                }
            }
        });
    }
    
    /**
     * Posiciona o cursor após o último caractere do campo
     * 
     * @param textField O campo a ter o cursor posicionado
     */
    private static void positionCaret(TextField textField) {
        Platform.runLater(() -> {
            if (textField.getText().length() != 0) {
                textField.positionCaret(textField.getText().length());
            }
        });
    }
    
    /**
     * Aplica uma máscara a um TextField
     * 
     * @param textField O campo que receberá a máscara
     * @param mask A máscara a ser aplicada, usando # como placeholder para dígitos
     */
    public static void maskField(TextField textField, String mask) {
        // Flag para evitar recursão
        final AtomicBoolean updating = new AtomicBoolean(false);
        
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            // Verificar se já está em atualização para evitar recursão
            if (updating.get()) {
                return;
            }
            
            // Se não houver mudança ou o valor for nulo, não fazer nada
            if (newValue == null || newValue.equals(oldValue)) {
                return;
            }
            
            // Marcar como em atualização
            updating.set(true);
            
            try {
                // Remover caracteres não numéricos para processamento
                String valorApenasDígitos = newValue.replaceAll("[^0-9]", "");
                
                // Aplicar a máscara
                StringBuilder valorMascarado = new StringBuilder();
                int posicaoValor = 0;
                
                for (char caractereMascara : mask.toCharArray()) {
                    if (posicaoValor >= valorApenasDígitos.length()) {
                        break; // Se já processou todos os dígitos, sair do loop
                    }
                    
                    if (caractereMascara == '#') {
                        // Adicionar o próximo dígito
                        valorMascarado.append(valorApenasDígitos.charAt(posicaoValor));
                        posicaoValor++;
                    } else {
                        // Adicionar o caractere da máscara
                        valorMascarado.append(caractereMascara);
                    }
                }
                
                // Se o texto calculado for diferente do atual
                final String textoFinal = valorMascarado.toString();
                if (!textoFinal.equals(textField.getText())) {
                    // Atualizar a UI na thread de aplicação
                    Platform.runLater(() -> {
                        try {
                            // Calculando onde colocar o cursor após a mudança
                            int novaPosicaoCursor = textField.getCaretPosition();
                            
                            // Verificar se a entrada está aumentando ou diminuindo
                            if (textoFinal.length() > textField.getText().length()) {
                                // Se está aumentando, adicionar máscaras ao cálculo
                                int mascarasAdicionadas = 0;
                                for (int i = Math.min(novaPosicaoCursor, textoFinal.length()) - 1; i >= 0; i--) {
                                    if (textoFinal.charAt(i) != '#' && !Character.isDigit(textoFinal.charAt(i))) {
                                        mascarasAdicionadas++;
                                    }
                                }
                                novaPosicaoCursor += mascarasAdicionadas;
                            }
                            
                            // Aplicar o texto
                            textField.setText(textoFinal);
                            
                            // Posicionar o cursor
                            if (novaPosicaoCursor <= textoFinal.length()) {
                                textField.positionCaret(novaPosicaoCursor);
                            } else {
                                textField.positionCaret(textoFinal.length());
                            }
                        } finally {
                            // Garantir que a flag seja restaurada mesmo em caso de erro
                            updating.set(false);
                        }
                    });
                } else {
                    // Se não houve mudança, restaurar a flag
                    updating.set(false);
                }
            } catch (Exception e) {
                // Log do erro e restauração da flag
                System.err.println("Erro ao aplicar máscara: " + e.getMessage());
                updating.set(false);
            }
        });
        
        // Tratar o caso inicial quando o campo já tem um valor
        if (textField.getText() != null && !textField.getText().isEmpty()) {
            // Forçar a aplicação da máscara ao valor inicial
            String valorInicial = textField.getText();
            textField.clear(); // Limpar para evitar confusão
            textField.setText(valorInicial); // Isso acionará o listener acima
        }
    }
    
    /**
     * Método alternativo com implementação mais simples para máscaras genéricas
     * 
     * @param textField O campo que receberá a máscara
     * @param mask A máscara a ser aplicada
     */
    public static void simpleMask(TextField textField, String mask) {
        final AtomicBoolean updating = new AtomicBoolean(false);
        
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updating.get() || newValue == null) {
                return;
            }
            
            updating.set(true);
            
            try {
                // Remover caracteres não numéricos
                String digits = newValue.replaceAll("[^0-9]", "");
                
                // Construir o texto mascarado
                StringBuilder result = new StringBuilder();
                int digitIndex = 0;
                
                for (int i = 0; i < mask.length() && digitIndex < digits.length(); i++) {
                    char maskChar = mask.charAt(i);
                    
                    if (maskChar == '#') {
                        result.append(digits.charAt(digitIndex++));
                    } else {
                        result.append(maskChar);
                    }
                }
                
                // Atualizar o campo
                String finalText = result.toString();
                Platform.runLater(() -> {
                    textField.setText(finalText);
                    textField.positionCaret(finalText.length());
                });
            } finally {
                updating.set(false);
            }
        });
    }
}