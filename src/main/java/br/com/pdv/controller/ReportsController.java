package br.com.pdv.controller;

import br.com.pdv.relatorio.*;
import br.com.pdv.relatorio.RelatorioManager.TipoRelatorio;
import br.com.pdv.relatorio.RelatorioManager.RelatorioPDVException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

   // @FXML
    //private Button backButton;
    @FXML
    private ComboBox<TipoRelatorio> reportTypeComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private RadioButton pdfFormatRadio;
    @FXML
    private RadioButton textFormatRadio;
    @FXML
    private ToggleGroup formatToggleGroup;
    @FXML
    private Button generateButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button printButton;
    @FXML
    private TextArea reportPreviewArea;
    
    @FXML
    private Button menuButton;

    private RelatorioManager relatorioManager;
    private RelatorioBase currentReport;
    private byte[] reportContent;
    private Scene mainScene;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar o RelatorioManager
        relatorioManager = RelatorioManager.getInstance();
        
        // Preencher o ComboBox com os tipos de relatório
        reportTypeComboBox.setItems(FXCollections.observableArrayList(TipoRelatorio.values()));
        reportTypeComboBox.getSelectionModel().selectFirst();
        
        // Definir datas iniciais (último mês)
        LocalDate today = LocalDate.now();
        LocalDate oneMonthAgo = today.minusMonths(1);
        startDatePicker.setValue(oneMonthAgo);
        endDatePicker.setValue(today);
        
        // Configurar ações dos botões
        configureButtonActions();
        
        // Desabilitar botões de salvar e imprimir até que um relatório seja gerado
        saveButton.setDisable(true);
        printButton.setDisable(true);
        
        // Evento para quando o tipo de relatório muda
        reportTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsPeriod = needsPeriod(newVal);
            startDatePicker.setDisable(!needsPeriod);
            endDatePicker.setDisable(!needsPeriod);
        });
        
        // Verificar se o relatório selecionado inicialmente precisa de período
        boolean needsPeriod = needsPeriod(reportTypeComboBox.getSelectionModel().getSelectedItem());
        startDatePicker.setDisable(!needsPeriod);
        endDatePicker.setDisable(!needsPeriod);
    }
    
    
    /**
     * Volta para a tela principal (PDV)
     **/
    @FXML
    private void voltarParaPDV() {
        try {
            Stage stage = (Stage) menuButton.getScene().getWindow();
            if (mainScene != null) {
                stage.setScene(mainScene);
                stage.setTitle("PDV - Sistema de Ponto de Venda");
            } else {
                // Caso a cena principal não tenha sido definida, carregá-la novamente
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/com/pdv/gui/views/MainView.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                stage.setTitle("PDV - Sistema de Ponto de Venda");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro ao Voltar");
            alert.setHeaderText("Não foi possível voltar à tela principal");
            alert.setContentText("Ocorreu um erro: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    /**
     * Define a cena principal para retornar a ela
     **/
    public void setMainScene(Scene scene) {
        this.mainScene = scene;
    }
    
    
    private void configureButtonActions() {
        // Botão Voltar
    	/**
        backButton.setOnAction(event -> {
            try {
                // Voltar para a tela principal
                Parent root = FXMLLoader.load(getClass().getResource("/br/com/pdv/gui/views/MainView.fxml"));
                Scene scene = new Scene(root);
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("PDV - Sistema de Ponto de Venda");
            } catch (IOException e) {
                showAlert("Erro ao retornar à tela principal: " + e.getMessage());
            }
        });**/
        
        // Botão Gerar Relatório
        generateButton.setOnAction(event -> generateReport());
        
        // Botão Salvar Relatório
        saveButton.setOnAction(event -> saveReport());
        
        // Botão Imprimir Relatório
        printButton.setOnAction(event -> printReport());
    }
    
    private boolean needsPeriod(TipoRelatorio tipoRelatorio) {
        // Verifique quais tipos de relatório não precisam de período
        return tipoRelatorio != TipoRelatorio.NIVEL_ESTOQUE && 
               tipoRelatorio != TipoRelatorio.VALOR_ESTOQUE;
    }
    
    private void generateReport() {
        try {
            TipoRelatorio tipoRelatorio = reportTypeComboBox.getSelectionModel().getSelectedItem();
            if (tipoRelatorio == null) {
                showAlert("Selecione um tipo de relatório");
                return;
            }
            
            LocalDateTime dataInicio = null;
            LocalDateTime dataFim = null;
            
            if (needsPeriod(tipoRelatorio)) {
                // Verificar se as datas foram selecionadas
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                    showAlert("Selecione as datas de início e fim");
                    return;
                }
                
                // Converter LocalDate para LocalDateTime (início às 00:00 e fim às 23:59)
                dataInicio = startDatePicker.getValue().atStartOfDay();
                dataFim = endDatePicker.getValue().atTime(LocalTime.MAX);
                
                // Verificar se a data de início é anterior à data de fim
                if (dataInicio.isAfter(dataFim)) {
                    showAlert("A data de início deve ser anterior à data de fim");
                    return;
                }
            }
            
            // Criar o relatório
            currentReport = relatorioManager.criarRelatorio(tipoRelatorio, dataInicio, dataFim);
            
            // Obter o conteúdo de acordo com o formato selecionado
            boolean isPdf = pdfFormatRadio.isSelected();
            if (isPdf) {
                try {
                    reportContent = currentReport.gerarRelatorioPDF();
                    reportPreviewArea.setText("Relatório PDF gerado com sucesso.\n\n" +
                        "Pré-visualização não disponível para formato PDF.\n" +
                        "Clique em 'Salvar Relatório' para salvar o PDF.");
                } catch (Exception e) {
                    // Se falhar ao gerar PDF, tenta gerar como texto
                    reportContent = currentReport.gerarRelatorio().getBytes();
                    reportPreviewArea.setText(new String(reportContent));
                    showAlert("Não foi possível gerar o relatório em PDF. Gerado em formato texto.");
                    pdfFormatRadio.setSelected(false);
                    textFormatRadio.setSelected(true);
                }
            } else {
                String reportText = currentReport.gerarRelatorio();
                reportContent = reportText.getBytes();
                reportPreviewArea.setText(reportText);
            }
            
            // Habilitar botões de salvar e imprimir
            saveButton.setDisable(false);
            printButton.setDisable(false);
            
            showInfo("Relatório gerado com sucesso!");
            
        } catch (RelatorioPDVException e) {
            showAlert("Erro ao criar relatório: " + e.getMessage());
        } catch (Exception e) {
            showAlert("Erro ao gerar relatório: " + e.getMessage());
        }
    }
    
    private void saveReport() {
        if (reportContent == null) {
            showAlert("Gere um relatório primeiro");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        TipoRelatorio tipoRelatorio = reportTypeComboBox.getSelectionModel().getSelectedItem();
        String fileName = tipoRelatorio.name().toLowerCase() + "_relatorio";
        
        boolean isPdf = pdfFormatRadio.isSelected();
        if (isPdf) {
            fileChooser.setTitle("Salvar Relatório PDF");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf")
            );
            fileChooser.setInitialFileName(fileName + ".pdf");
        } else {
            fileChooser.setTitle("Salvar Relatório de Texto");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos de Texto", "*.txt")
            );
            fileChooser.setInitialFileName(fileName + ".txt");
        }
        
        File file = fileChooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(reportContent);
                showInfo("Relatório salvo com sucesso em: " + file.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Erro ao salvar relatório: " + e.getMessage());
            }
        }
    }
    
    private void printReport() {
        if (reportContent == null) {
            showAlert("Gere um relatório primeiro");
            return;
        }
        
        try {
            // Criar um arquivo temporário para o relatório
            String prefix = reportTypeComboBox.getSelectionModel().getSelectedItem().name().toLowerCase();
            String suffix = pdfFormatRadio.isSelected() ? ".pdf" : ".txt";
            File tempFile = File.createTempFile(prefix + "_", suffix);
            
            // Escrever o conteúdo no arquivo temporário
            Files.write(tempFile.toPath(), reportContent);
            
            // Tentar imprimir usando o visualizador padrão do sistema
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.PRINT)) {
                    desktop.print(tempFile);
                    showInfo("Relatório enviado para impressão");
                } else {
                    desktop.open(tempFile);
                    showInfo("Não foi possível imprimir diretamente. O arquivo foi aberto no aplicativo padrão.");
                }
            } else {
                showAlert("Impressão não suportada na plataforma atual");
            }
        } catch (IOException e) {
            showAlert("Erro ao imprimir relatório: " + e.getMessage());
        }
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}