package br.com.pdv.controller;

import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.exception.CpfCnpjDuplicadoException;
import br.com.pdv.model.Cliente;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.MaskFieldUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class NovoClienteController implements Initializable {

    @FXML
    private TextField nomeField;
    
    @FXML
    private TextField cpfField;
    
    @FXML
    private TextField celularField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField enderecoField;
    
    @FXML
    private TextField bairroField;
    
    @FXML
    private TextField cidadeField;
    
    @FXML
    private TextField estadoField;
    
    @FXML
    private TextField cepField;
    
    @FXML
    private DatePicker dataNascimentoPicker;
    
    @FXML
    private TextArea observacaoArea;
    
    @FXML
    private Button salvarButton;
    
    @FXML
    private Button cancelarButton;
    
    
    
    private ClienteDAO clienteDAO;
    private Cliente clienteAtual;
    private boolean modoEdicao = false;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    	// Inicializar DAO
        clienteDAO = ClienteDAO.getInstance();
        
        // Inicializar um novo cliente
        clienteAtual = new Cliente();
        
        // Aplicar máscaras aos campos
        MaskFieldUtil.cpfMask(cpfField);
        MaskFieldUtil.foneCell(celularField);
        MaskFieldUtil.cepMask(cepField);
        
        // Configurar o DatePicker (ADICIONE ESTA LINHA)
        configurarDatePicker();
        
        // Configurar eventos dos botões
        configurarEventosBotoes();
    }
    
    /**
     * Configura os eventos dos botões
     */
    private void configurarEventosBotoes() {
        salvarButton.setOnAction(event -> salvarCliente());
        cancelarButton.setOnAction(event -> fecharJanela());
    }
    
    /**
     * Define o cliente para edição
     * 
     * @param cliente O cliente a ser editado
     */
    public void setClienteParaEdicao(Cliente cliente) {
        this.clienteAtual = cliente;
        this.modoEdicao = true;
        
        // Preencher os campos com os dados do cliente
        preencherCampos();
    }
    
    /**
     * Preenche os campos do formulário com os dados do cliente em edição
     */
    private void preencherCampos() {
        if (clienteAtual != null) {
            nomeField.setText(clienteAtual.getNome());
            cpfField.setText(clienteAtual.getCpfCnpj());
            celularField.setText(clienteAtual.getTelefone());
            emailField.setText(clienteAtual.getEmail());

            // Tratar o endereço completo
            String endereco = clienteAtual.getEndereco();
            if (endereco != null) {
                // Tentativa de extrair informações do endereço concatenado
                enderecoField.setText(extrairEndereco(endereco));
                bairroField.setText(extrairBairro(endereco));
                cidadeField.setText(extrairCidade(endereco));
                estadoField.setText(extrairEstado(endereco));
                cepField.setText(extrairCep(endereco));
            } else {
                enderecoField.setText("");
                bairroField.setText("");
                cidadeField.setText("");
                estadoField.setText("");
                cepField.setText("");
            }

            // Verificar e definir data de nascimento
            if (clienteAtual.getDataNascimento() != null) {
                // Converter de Date para LocalDate
                dataNascimentoPicker.setValue(((java.sql.Date) clienteAtual.getDataNascimento()).toLocalDate());
            } else {
                dataNascimentoPicker.setValue(null);
            }

            // Verificar e definir observação
            observacaoArea.setText(clienteAtual.getObservacao() != null ? clienteAtual.getObservacao() : "");
        }
    }

    /**
     * Método para configurar o DatePicker com formato correto
     * Deve ser chamado no método initialize() da classe
     */
    private void configurarDatePicker() {
        // Definir o formato de exibição da data
        dataNascimentoPicker.setPromptText("dd/MM/yyyy");

        // Configurar o conversor para formatar a data adequadamente
        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    try {
                        return LocalDate.parse(string, dateFormatter);
                    } catch (DateTimeParseException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        };
        dataNascimentoPicker.setConverter(converter);
        
        // Acessar o editor interno do DatePicker
        TextField editor = dataNascimentoPicker.getEditor();
        
        // Criar uma flag para evitar recursão
        final AtomicBoolean updating = new AtomicBoolean(false);
        
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updating.get() || newValue == null) {
                return;
            }
            
            // Remover todos os caracteres não numéricos
            String numeros = newValue.replaceAll("[^0-9]", "");
            
            // Se não tiver números, não fazer nada
            if (numeros.isEmpty()) {
                return;
            }
            
            updating.set(true);
            try {
                // Limitar a 8 dígitos (ddMMaaaa)
                if (numeros.length() > 8) {
                    numeros = numeros.substring(0, 8);
                }
                
                // Formatar com as barras
                StringBuilder formatado = new StringBuilder();
                
                for (int i = 0; i < numeros.length(); i++) {
                    // Adicionar barras antes do 3º e 5º dígitos
                    if (i == 2 || i == 4) {
                        formatado.append('/');
                    }
                    formatado.append(numeros.charAt(i));
                }
                
                Platform.runLater(() -> {
                    editor.setText(formatado.toString());
                    // Posicionar o cursor no final
                    editor.positionCaret(formatado.length());
                });
            } finally {
                updating.set(false);
            }
        });
    }
    
    
    // Métodos auxiliares para tentar extrair partes do endereço
    private String extrairEndereco(String enderecoCompleto) {
        // Extrai a parte do endereço antes da primeira vírgula
        int index = enderecoCompleto.indexOf(',');
        if (index > 0) {
            return enderecoCompleto.substring(0, index).trim();
        }
        return enderecoCompleto; // Se não houver vírgula, retorna todo o texto
    }

    private String extrairBairro(String enderecoCompleto) {
        // Tenta encontrar o padrão "Bairro: X"
        int inicio = enderecoCompleto.indexOf("Bairro:");
        if (inicio >= 0) {
            inicio += 7; // Comprimento de "Bairro:"
            int fim = enderecoCompleto.indexOf(',', inicio);
            if (fim > inicio) {
                return enderecoCompleto.substring(inicio, fim).trim();
            } else {
                return enderecoCompleto.substring(inicio).trim();
            }
        }
        return "";
    }

    private String extrairCidade(String enderecoCompleto) {
        // Tenta extrair a cidade que geralmente está entre uma vírgula e um hífen
        int inicioPossivel = enderecoCompleto.lastIndexOf(',');
        if (inicioPossivel >= 0) {
            int fimPossivel = enderecoCompleto.indexOf('-', inicioPossivel);
            if (fimPossivel > inicioPossivel) {
                return enderecoCompleto.substring(inicioPossivel + 1, fimPossivel).trim();
            }
        }
        return "";
    }

    private String extrairEstado(String enderecoCompleto) {
        // Estado geralmente está após um hífen
        int inicio = enderecoCompleto.lastIndexOf('-');
        if (inicio >= 0) {
            int fim = enderecoCompleto.indexOf(',', inicio);
            if (fim > inicio) {
                return enderecoCompleto.substring(inicio + 1, fim).trim();
            } else {
                return enderecoCompleto.substring(inicio + 1).trim();
            }
        }
        return "";
    }

    private String extrairCep(String enderecoCompleto) {
        // CEP geralmente está no formato "CEP: XXXXX-XXX"
        int inicio = enderecoCompleto.indexOf("CEP:");
        if (inicio >= 0) {
            inicio += 4; // Comprimento de "CEP:"
            return enderecoCompleto.substring(inicio).trim().replaceAll("[^0-9]", "");
        }
        return "";
    }
    
    
    /**
     * Coleta os dados do formulário e salva o cliente
     */
    @FXML
    private void salvarCliente() {
        try {
            // Validar campos obrigatórios
            if (nomeField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O nome do cliente é obrigatório.");
                nomeField.requestFocus();
                return;
            }
            
            // Atualizar os dados do cliente com os valores do formulário
            clienteAtual.setNome(nomeField.getText().trim());
            clienteAtual.setCpfCnpj(cpfField.getText().replaceAll("[^0-9]", ""));
            clienteAtual.setTelefone(celularField.getText());
            clienteAtual.setEmail(emailField.getText().trim());
            
            // Construir o endereço completo com as informações disponíveis
            StringBuilder endereco = new StringBuilder();
            String enderecoValue = enderecoField.getText().trim();
            String bairroValue = bairroField.getText().trim();
            String cidadeValue = cidadeField.getText().trim();
            String estadoValue = estadoField.getText().trim();
            String cepValue = cepField.getText().replaceAll("[^0-9]", "");
            
            if (!enderecoValue.isEmpty()) {
                endereco.append(enderecoValue);
            }
            
            if (!bairroValue.isEmpty()) {
                if (endereco.length() > 0) endereco.append(", ");
                endereco.append("Bairro: ").append(bairroValue);
            }
            
            if (!cidadeValue.isEmpty()) {
                if (endereco.length() > 0) endereco.append(", ");
                endereco.append(cidadeValue);
            }
            
            if (!estadoValue.isEmpty()) {
                if (endereco.length() > 0) endereco.append(" - ");
                endereco.append(estadoValue);
            }
            
            if (!cepValue.isEmpty()) {
                if (endereco.length() > 0) endereco.append(", ");
                endereco.append("CEP: ").append(cepValue);
            }
            
            clienteAtual.setEndereco(endereco.toString());
            
            // Salvar a data de nascimento
            LocalDate dataNascimento = dataNascimentoPicker.getValue();
            if (dataNascimento != null) {
                // Convertendo LocalDate para Date (se necessário para seu modelo)
                Date dataNascimentoDate = java.sql.Date.valueOf(dataNascimento);
                clienteAtual.setDataNascimento(dataNascimentoDate);
            } else {
                // Se não houver data selecionada, definir como null
                clienteAtual.setDataNascimento(null);
            }

            // Salvar a observação
            String observacao = observacaoArea.getText();
            if (observacao != null && !observacao.trim().isEmpty()) {
                clienteAtual.setObservacao(observacao.trim());
            } else {
                clienteAtual.setObservacao(null);
            }
            
            // Se for um novo cliente
            if (!modoEdicao) {
                clienteAtual.setAtivo(true);
                clienteAtual.setDataCadastro(new Date()); // Use java.util.Date
            }
            
            // Salvar no banco de dados
            try {
                if (modoEdicao) {
                    clienteDAO.update(clienteAtual);
                    AlertUtil.showInfo("Cliente atualizado", "O cliente foi atualizado com sucesso!");
                } else {
                    clienteDAO.create(clienteAtual);
                    AlertUtil.showInfo("Cliente cadastrado", "O cliente foi cadastrado com sucesso!");
                }
                
                // Fechar a janela
                fecharJanela();
                
            } catch (CpfCnpjDuplicadoException e) {
                AlertUtil.showWarning("CPF/CNPJ duplicado", 
                    "Já existe um cliente cadastrado com este CPF/CNPJ.");
                cpfField.requestFocus();
            } catch (SQLException e) {
                AlertUtil.showError("Erro ao salvar", 
                    "Ocorreu um erro ao salvar o cliente: " + e.getMessage());
            }
            
        } catch (Exception e) {
            AlertUtil.showError("Erro", "Ocorreu um erro inesperado: " + e.getMessage());
        }
    }
    
    /**
     * Fecha a janela atual
     */
    @FXML
    private void fecharJanela() {
        Stage stage = (Stage) cancelarButton.getScene().getWindow();
        stage.close();
    }
}