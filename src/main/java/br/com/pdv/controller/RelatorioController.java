package br.com.pdv.controller;

import br.com.pdv.relatorio.*;
import br.com.pdv.relatorio.RelatorioManager.TipoRelatorio;
import br.com.pdv.relatorio.RelatorioManager.RelatorioPDVException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
//import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controlador para a tela de geração e gerenciamento de relatórios.
 * Permite ao usuário selecionar, gerar, visualizar, salvar e imprimir relatórios.
 */
public class RelatorioController implements Initializable {

    @FXML
    private Button backButton;
    
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

    // Gerenciador de relatórios
    private RelatorioManager relatorioManager;
    
    // Relatório atual
    private RelatorioBase currentReport;
    
    // Conteúdo do relatório em bytes
    private byte[] reportContent;
    
    // Progresso de geração
    private ProgressIndicator progressIndicator;
    private Stage stage;
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
        
        // Adicionar indicador de progresso
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(50, 50);
        
        // Adicionar o indicador de progresso ao layout
        Platform.runLater(() -> {
            if (generateButton.getParent() instanceof Pane) {
                Pane parent = (Pane) generateButton.getParent();
                parent.getChildren().add(progressIndicator);
                progressIndicator.setLayoutX(generateButton.getLayoutX() + generateButton.getWidth() / 2 - 25);
                progressIndicator.setLayoutY(generateButton.getLayoutY() + generateButton.getHeight() + 10);
            }
        });
    }
    
    /**
     * Configura as ações dos botões da interface
     */
    /*
    private void configureButtonActions() {
        // Botão Voltar
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
        });
        
        // Botão Gerar Relatório
        generateButton.setOnAction(event -> generateReport());
        
        // Botão Salvar Relatório
        saveButton.setOnAction(event -> saveReport());
        
        // Botão Imprimir Relatório
        printButton.setOnAction(event -> printReport());
    }
    */
    private void configureButtonActions() {
        // Botão Voltar - configure apenas se não foi configurado pelo setStage
        if (stage == null || mainScene == null) {
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
            });
        }
        
        // Botão Gerar Relatório
        generateButton.setOnAction(event -> generateReport());
        
        // Botão Salvar Relatório
        saveButton.setOnAction(event -> saveReport());
        
        // Botão Imprimir Relatório
        printButton.setOnAction(event -> printReport());
    }
    /**
     * Verifica se um tipo de relatório necessita de período (data início e fim)
     */
    private boolean needsPeriod(TipoRelatorio tipoRelatorio) {
        // Verifica quais tipos de relatório não precisam de período
        return tipoRelatorio != TipoRelatorio.NIVEL_ESTOQUE && 
               tipoRelatorio != TipoRelatorio.VALOR_ESTOQUE;
    }
    
    /**
     * Gera o relatório selecionado
     */
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
            
            // Desabilitar elementos durante a geração do relatório
            setControlsDisabled(true);
            progressIndicator.setVisible(true);
            reportPreviewArea.setText("Gerando relatório, por favor aguarde...");
            
            // Criar o relatório em uma thread separada para não bloquear a interface
            final LocalDateTime finalDataInicio = dataInicio;
            final LocalDateTime finalDataFim = dataFim;
            
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        // Criar o relatório
                        currentReport = relatorioManager.criarRelatorio(tipoRelatorio, finalDataInicio, finalDataFim);
                        
                        // Obter o conteúdo de acordo com o formato selecionado
                        boolean isPdf = pdfFormatRadio.isSelected();
                        
                        if (isPdf) {
                            try {
                                reportContent = currentReport.gerarRelatorioPDF();
                                
                                Platform.runLater(() -> {
                                    reportPreviewArea.setText("Relatório PDF gerado com sucesso.\n\n" +
                                        "Pré-visualização não disponível para formato PDF.\n" +
                                        "Clique em 'Salvar Relatório' para salvar o PDF.");
                                });
                            } catch (Exception e) {
                                // Se falhar ao gerar PDF, tenta gerar como texto
                                final String relatorioTexto = currentReport.gerarRelatorio();
                                reportContent = relatorioTexto.getBytes();
                                
                                Platform.runLater(() -> {
                                    reportPreviewArea.setText(relatorioTexto);
                                    pdfFormatRadio.setSelected(false);
                                    textFormatRadio.setSelected(true);
                                    showAlert("Não foi possível gerar o relatório em PDF. Gerado em formato texto.");
                                });
                            }
                        } else {
                            String reportText = currentReport.gerarRelatorio();
                            reportContent = reportText.getBytes();
                            
                            Platform.runLater(() -> {
                                reportPreviewArea.setText(reportText);
                            });
                        }
                        
                        Platform.runLater(() -> {
                            // Habilitar botões de salvar e imprimir
                            saveButton.setDisable(false);
                            printButton.setDisable(false);
                            
                            // Mostrar mensagem de sucesso
                            showInfo("Relatório gerado com sucesso!");
                        });
                        
                    } catch (RelatorioPDVException e) {
                        Platform.runLater(() -> {
                            showAlert("Erro ao criar relatório: " + e.getMessage());
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert("Erro ao gerar relatório: " + e.getMessage());
                        });
                    } finally {
                        Platform.runLater(() -> {
                            // Reabilitar elementos após a geração do relatório
                            setControlsDisabled(false);
                            progressIndicator.setVisible(false);
                        });
                    }
                    
                    return null;
                }
            };
            
            // Iniciar a tarefa em uma nova thread
            new Thread(task).start();
            
        } catch (Exception e) {
            showAlert("Erro ao preparar relatório: " + e.getMessage());
            setControlsDisabled(false);
            progressIndicator.setVisible(false);
        }
    }
    
    /**
     * Configura o Stage e a cena principal para navegação
     * 
     * @param stage O Stage atual da aplicação
     * @param mainScene A cena principal para retornar quando necessário
     */
    public void setStage(Stage stage, Scene mainScene) {
        this.stage = stage;
        this.mainScene = mainScene;
        
        // Configurar o botão voltar para usar o mainScene
        backButton.setOnAction(event -> {
            if (stage != null && mainScene != null) {
                try {
                    // Restaurar a cena principal
                    stage.setScene(mainScene);
                    stage.setTitle("PDV - Sistema de Ponto de Venda");
                    
                    // Solicitar foco para o campo de produto na tela principal
                    Platform.runLater(() -> {
                        Node node = mainScene.lookup("#productField");
                        if (node instanceof TextField) {
                            node.requestFocus();
                        }
                    });
                } catch (Exception e) {
                    showAlert("Erro ao voltar para a tela principal: " + e.getMessage());
                }
            } else {
                showAlert("Não foi possível retornar à tela principal. Stage ou Scene não configurados.");
            }
        });
    }
    
    
    
    
    /**
     * Habilita/desabilita os controles de interface durante processamento
     */
    private void setControlsDisabled(boolean disabled) {
        reportTypeComboBox.setDisable(disabled);
        startDatePicker.setDisable(disabled || !needsPeriod(reportTypeComboBox.getSelectionModel().getSelectedItem()));
        endDatePicker.setDisable(disabled || !needsPeriod(reportTypeComboBox.getSelectionModel().getSelectedItem()));
        pdfFormatRadio.setDisable(disabled);
        textFormatRadio.setDisable(disabled);
        generateButton.setDisable(disabled);
    }
    
    /**
     * Salva o relatório em um arquivo
     */
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
    
    /**
     * Imprime o relatório (abre no visualizador padrão para impressão)
     */
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
            tempFile.deleteOnExit(); // Deletar o arquivo ao sair
            
            // Escrever o conteúdo no arquivo temporário
            Files.write(tempFile.toPath(), reportContent);
            
            // Tentar imprimir usando o visualizador padrão do sistema
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                
                if (desktop.isSupported(java.awt.Desktop.Action.PRINT)) {
                    desktop.print(tempFile);
                    showInfo("Relatório enviado para impressão");
                } else if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(tempFile);
                    showInfo("Não foi possível imprimir diretamente. O arquivo foi aberto no aplicativo padrão.");
                } else {
                    showAlert("Seu sistema não suporta a abertura ou impressão de arquivos.");
                }
            } else {
                showAlert("Impressão não suportada na plataforma atual");
            }
        } catch (IOException e) {
            showAlert("Erro ao imprimir relatório: " + e.getMessage());
        }
    }
    
    /**
     * Exibe um alerta de erro
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Exibe um alerta informativo
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}