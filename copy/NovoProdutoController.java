package br.com.pdv.controller;

import br.com.pdv.dao.CategoriaDAO;
import br.com.pdv.dao.MarcaDAO;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.dao.SubcategoriaDAO;
import br.com.pdv.model.Categoria;
import br.com.pdv.model.Marca;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Subcategoria;
import br.com.pdv.util.AlertUtil;
import br.com.pdv.util.FormatUtil;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;
import java.time.format.DateTimeFormatter;

public class NovoProdutoController implements Initializable {

    // Campos do formulário
    @FXML private TextField nomeField;
    @FXML private TextArea descricaoArea;
    @FXML private ComboBox<String> tipoComboBox;
    @FXML private ComboBox<Marca> marcaComboBox;
    @FXML private ComboBox<Subcategoria> subcategoriaComboBox;
    @FXML private TextField unidadeField;
    @FXML private ComboBox<Categoria> categoriaComboBox;
    @FXML private TextField codigoBarrasField;
    @FXML private TextField codigoProdutoField;
    @FXML private ComboBox<String> corComboBox;
    @FXML private ComboBox<String> tamanhoComboBox;
    @FXML private TextField custoField;
    @FXML private TextField markupField;
    @FXML private TextField valorField;
    @FXML private TextField cfopField;
    @FXML private TextField icmsField;
    @FXML private TextField icmsSubField;
    @FXML private TextField estoqueField;
    @FXML private TextField estoqueMinField;
    
    // Botões
    @FXML private Button novaMarcaButton;
    @FXML private Button novaSubcategoriaButton;
    @FXML private Button novaCategoriaButton;
    @FXML private Button novaCorButton;
    @FXML private Button novoTamanhoButton;
    @FXML private Button cancelarButton;
    @FXML private Button salvarButton;
    @FXML
    private DatePicker vencimentoPicker;
    // DAOs
    private ProdutoDAO produtoDAO;
    private CategoriaDAO categoriaDAO;
    private MarcaDAO marcaDAO;
    private SubcategoriaDAO subcategoriaDAO;
    
    // Produto sendo editado
    private Produto produtoAtual;
    private boolean modoEdicao = false;
    
    // Listas observáveis para os comboboxes
    private ObservableList<Categoria> categorias;
    private ObservableList<Marca> marcas;
    private ObservableList<Subcategoria> subcategorias;
    private ObservableList<String> cores;
    private ObservableList<String> tamanhos;
    private ObservableList<String> tipos;
    
    // Ouvintes para cálculo de valores
    private ChangeListener<String> custoChangeListener;
    private ChangeListener<String> markupChangeListener;
    private ChangeListener<String> valorChangeListener;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Inicializar DAOs
        produtoDAO = ProdutoDAO.getInstance();
        categoriaDAO = CategoriaDAO.getInstance();
        marcaDAO = MarcaDAO.getInstance();
        subcategoriaDAO = SubcategoriaDAO.getInstance();

        // Inicializar listas
        categorias = FXCollections.observableArrayList();
        marcas = FXCollections.observableArrayList();
        subcategorias = FXCollections.observableArrayList();
        cores = FXCollections.observableArrayList();
        tamanhos = FXCollections.observableArrayList();
        tipos = FXCollections.observableArrayList();

        // Carregar dados para os comboboxes
        carregarCategorias();
        carregarMarcas();
        carregarSubcategorias();
        carregarCores();
        carregarTamanhos();
        carregarTipos();

        // Configurar formatação de campos numéricos
        configureDecimalFields();

        // Configurar eventos para cálculo de valores
        setupCalculoPreco();
        
        // Configurar eventos dos botões
        configurarBotoes();

        // Definir valor padrão para unidade
        unidadeField.setText("UN");
        
        // Configurar o DatePicker de vencimento
        configureVencimentoPicker();

        // Inicializar produto
        novoProduto();
    }

    /**
     * Configura o DatePicker para data de vencimento
     */
    private void configureVencimentoPicker() {
        // Configurar o DatePicker para formato de data brasileiro
        vencimentoPicker.setConverter(new StringConverter<LocalDate>() {
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
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        });
        
        // Definir valor padrão para 1 ano no futuro
        vencimentoPicker.setValue(LocalDate.now().plusYears(1));
        
        // Adicionar prompt text
        vencimentoPicker.setPromptText("Data de Vencimento");
    }
    
    
    /**
     * Configura os eventos para cálculo automático de preço
     */
    private void setupCalculoPreco() {
        // Remover listeners antigos se existirem
        if (custoChangeListener != null) {
            custoField.textProperty().removeListener(custoChangeListener);
        }
        if (markupChangeListener != null) {
            markupField.textProperty().removeListener(markupChangeListener);
        }
        if (valorChangeListener != null) {
            valorField.textProperty().removeListener(valorChangeListener);
        }
        
        // Criar novos listeners
        custoChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue) && !newValue.isEmpty()) {
                // Se o markup tem valor, recalcular o valor final
                if (!markupField.getText().isEmpty()) {
                    calcularValorFinal();
                }
            }
        };
        
        markupChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue) && !newValue.isEmpty()) {
                // Se o custo tem valor, recalcular o valor final
                if (!custoField.getText().isEmpty()) {
                    calcularValorFinal();
                }
            }
        };
        
        valorChangeListener = (observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue) && !newValue.isEmpty() && !custoField.getText().isEmpty()) {
                // Se valor e custo têm valores, recalcular o markup
                calcularMarkup();
            }
        };
        
        // Adicionar os listeners
        custoField.textProperty().addListener(custoChangeListener);
        markupField.textProperty().addListener(markupChangeListener);
        valorField.textProperty().addListener(valorChangeListener);
    }
    
    
    /**
     * Carrega as categorias do banco de dados
     */
    private void carregarCategorias() {
        try {
            List<Categoria> lista = categoriaDAO.findAll();
            categorias.clear();
            categorias.addAll(lista);
            categoriaComboBox.setItems(categorias);
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar categorias", e.getMessage());
        }
    }
    
    /**
     * Carrega as marcas do banco de dados
     */
    private void carregarMarcas() {
        try {
            List<Marca> lista = marcaDAO.findAll();
            marcas.clear();
            marcas.addAll(lista);
            marcaComboBox.setItems(marcas);
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar marcas", e.getMessage());
        }
    }
    
    /**
     * Carrega as subcategorias do banco de dados
     */
    private void carregarSubcategorias() {
        try {
            List<Subcategoria> lista = subcategoriaDAO.findAll();
            subcategorias.clear();
            subcategorias.addAll(lista);
            subcategoriaComboBox.setItems(subcategorias);
        } catch (SQLException e) {
            AlertUtil.showError("Erro ao carregar subcategorias", e.getMessage());
        }
    }
    
    /**
     * Carrega os dados de cores do banco de dados ou define valores padrão
     */
    private void carregarCores() {
        // Em um sistema real, buscaria do banco de dados
        cores.addAll("Vermelho", "Azul", "Verde", "Amarelo", "Branco", "Preto", "Rosa", "Roxo", "Laranja", "Marrom");
        corComboBox.setItems(cores);
    }
    
    /**
     * Carrega os dados de tamanhos do banco de dados ou define valores padrão
     */
    private void carregarTamanhos() {
        // Em um sistema real, buscaria do banco de dados
        tamanhos.addAll("P", "M", "G", "GG", "XG", "Único");
        tamanhoComboBox.setItems(tamanhos);
    }
    
    /**
     * Carrega os tipos de produtos
     */
    private void carregarTipos() {
        // Em um sistema real, buscaria do banco de dados
        tipos.addAll("Produto", "Serviço", "Kit");
        tipoComboBox.setItems(tipos);
    }
    
    /**
     * Configura formatação para campos decimais
     */
    private void configureDecimalFields() {
        // Aplicar formatação para campos de valores
        FormatUtil.formatTextFieldAsCurrency(custoField);
        FormatUtil.formatTextFieldAsCurrency(valorField);
        
        // Aplicar uma formatação mais simples para o markup
        markupField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                try {
                    // Permitir apenas números e o caractere de vírgula
                    String filtered = newValue.replaceAll("[^0-9,]", "");
                    
                    // Garantir apenas uma vírgula
                    if (filtered.chars().filter(ch -> ch == ',').count() > 1) {
                        int index = filtered.indexOf(',');
                        filtered = filtered.substring(0, index + 1) + 
                                   filtered.substring(index + 1).replace(",", "");
                    }
                    
                    // Atualizar o campo apenas se o valor foi alterado
                    if (!filtered.equals(newValue)) {
                        markupField.setText(filtered);
                    }
                } catch (Exception e) {
                    // Ignorar erros
                }
            }
        });
        
        FormatUtil.formatTextFieldAsPercentage(icmsField);
        FormatUtil.formatTextFieldAsPercentage(icmsSubField);
        
        // Aplicar formatação para campos inteiros
        FormatUtil.formatTextFieldAsInteger(estoqueField);
        FormatUtil.formatTextFieldAsInteger(estoqueMinField);
    }
    
    /**
     * Calcula o valor final baseado no custo e markup
     */
    private void calcularValorFinal() {
        try {
            String custoStr = custoField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            String markupStr = markupField.getText().replace("%", "").replace(",", ".").trim();
            
            if (custoStr.isEmpty() || markupStr.isEmpty()) {
                return;
            }
            
            BigDecimal custo = new BigDecimal(custoStr);
            BigDecimal markup = new BigDecimal(markupStr).divide(new BigDecimal("100"), 4, RoundingMode.HALF_EVEN);
            
            // Fórmula: valor = custo * (1 + markup)
            BigDecimal valorFinal = custo.multiply(BigDecimal.ONE.add(markup)).setScale(2, RoundingMode.HALF_EVEN);
            
            // Log para debug
            System.out.println("Calculando valor final: Custo=" + custo + ", Markup=" + markupStr + "%, Valor=" + valorFinal);
            
            // Verificar se o campo de valor já está sendo editado pelo usuário
            if (!valorField.isFocused()) {
                valorField.setText(FormatUtil.formatarValor(valorFinal));
            }
        } catch (NumberFormatException e) {
            // Ignorar erros durante a digitação
        }
    }
    
    /**
     * Calcula o markup baseado no custo e valor final
     */
    private void calcularMarkup() {
        try {
            String custoStr = custoField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            String valorStr = valorField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
            
            if (custoStr.isEmpty() || valorStr.isEmpty() || custoStr.equals("0") || custoStr.equals("0.0")) {
                return;
            }
            
            BigDecimal custo = new BigDecimal(custoStr);
            BigDecimal valor = new BigDecimal(valorStr);
            
            // Fórmula: markup = (valor / custo) - 1
            BigDecimal markup = valor.divide(custo, 4, RoundingMode.HALF_EVEN).subtract(BigDecimal.ONE);
            markup = markup.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_EVEN);
            
            // Log para debug
            System.out.println("Calculando markup: Custo=" + custo + ", Valor=" + valor + ", Markup=" + markup + "%");
            
            // Usar formatação mais simples
            String formattedMarkup = markup.toString().replace(".", ",");
            
            // Verificar se o campo já está sendo editado pelo usuário
            if (!markupField.isFocused()) {
                markupField.setText(formattedMarkup);
            }
        } catch (NumberFormatException | ArithmeticException e) {
            // Ignorar erros durante a digitação
        }
    }
    
    /**
     * Configura os eventos dos botões
     */
    private void configurarBotoes() {
        // Botões para adicionar novos itens
        novaMarcaButton.setOnAction(event -> abrirCadastroMarca());
        novaSubcategoriaButton.setOnAction(event -> abrirCadastroSubcategoria());
        novaCategoriaButton.setOnAction(event -> abrirCadastroCategoria());
        novaCorButton.setOnAction(event -> abrirCadastroCor());
        novoTamanhoButton.setOnAction(event -> abrirCadastroTamanho());
        
        // Botões de controle de formulário
        cancelarButton.setOnAction(event -> cancelarCadastro());
        salvarButton.setOnAction(event -> salvarProduto());
    }
    
    /**
     * Inicia um novo produto
     */
    public void novoProduto() {
        produtoAtual = new Produto();
        modoEdicao = false;
       
        // Limpar campos
        nomeField.clear();
        descricaoArea.clear();
        tipoComboBox.getSelectionModel().clearSelection();
        marcaComboBox.getSelectionModel().clearSelection();
        subcategoriaComboBox.getSelectionModel().clearSelection();
        unidadeField.setText("UN");
        categoriaComboBox.getSelectionModel().clearSelection();
        codigoBarrasField.clear();
        codigoProdutoField.clear();
        corComboBox.getSelectionModel().clearSelection();
        tamanhoComboBox.getSelectionModel().clearSelection();
        custoField.setText("0,00");
        markupField.setText("0,00");
        valorField.setText("0,00");
        cfopField.setText("5.102");
        icmsField.setText("0,00");
        icmsSubField.setText("0,00");
        estoqueField.setText("0");
        estoqueMinField.setText("0");
        vencimentoPicker.setValue(LocalDate.now().plusWeeks(2));
        // Focar no campo de nome
        Platform.runLater(() -> nomeField.requestFocus());
    }
    
    /**
     * Configura o produto para edição
     * 
     * @param produto O produto a ser editado
     */
    public void editarProduto(Produto produto) {
        this.produtoAtual = produto;
        modoEdicao = true;
        
        // Preencher campos com dados do produto
        nomeField.setText(produto.getNome());
        descricaoArea.setText(produto.getDescricao());
        
        // Selecionar valores nos combos
        for (int i = 0; i < tipos.size(); i++) {
            if (tipos.get(i).equals(produto.getTipo())) {
                tipoComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Selecionar marca
        for (int i = 0; i < marcas.size(); i++) {
            if (marcas.get(i).getId().equals(produto.getMarca().getId())) {
                marcaComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Selecionar subcategoria
        for (int i = 0; i < subcategorias.size(); i++) {
            if (produto.getSubcategoria() != null && 
                subcategorias.get(i).getId().equals(produto.getSubcategoria().getId())) {
                subcategoriaComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Selecionar categoria
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).getId().equals(produto.getCategoria().getId())) {
                categoriaComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Outros campos
        unidadeField.setText(produto.getUnidade());
        codigoBarrasField.setText(produto.getCodigoBarra());
        codigoProdutoField.setText(produto.getCodigo());
        
        // Selecionar cor
        for (int i = 0; i < cores.size(); i++) {
            if (cores.get(i).equals(produto.getCor())) {
                corComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Selecionar tamanho
        for (int i = 0; i < tamanhos.size(); i++) {
            if (tamanhos.get(i).equals(produto.getTamanho())) {
            	tamanhoComboBox.getSelectionModel().select(i);
                break;
            }
        }
        
        // Valores numéricos
        custoField.setText(FormatUtil.formatarValor(produto.getCusto()));
        
        // Calcular markup baseado no custo e preço
        BigDecimal markup = BigDecimal.ZERO;
        if (produto.getCusto().compareTo(BigDecimal.ZERO) > 0) {
            markup = produto.getPreco()
                    .divide(produto.getCusto(), 4, RoundingMode.HALF_EVEN)
                    .subtract(BigDecimal.ONE)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        markupField.setText(FormatUtil.formatarPercentual(markup));
        valorField.setText(FormatUtil.formatarValor(produto.getPreco()));
        
        // Outros campos
        cfopField.setText(produto.getCfop());
        icmsField.setText(FormatUtil.formatarPercentual(produto.getIcms()));
        icmsSubField.setText(FormatUtil.formatarPercentual(produto.getIcmsSub()));
        estoqueField.setText(String.valueOf(produto.getEstoqueAtual()));
        estoqueMinField.setText(String.valueOf(produto.getEstoqueMinimo()));
        
        // Focar no campo de nome
        Platform.runLater(() -> nomeField.requestFocus());
    }
    
    /**
     * Salva o produto no banco de dados
     */
    private void salvarProduto() {
        try {
            // Validar campos obrigatórios
            if (nomeField.getText().trim().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "O nome do produto é obrigatório.");
                nomeField.requestFocus();
                return;
            }
            
            if (categoriaComboBox.getSelectionModel().isEmpty()) {
                AlertUtil.showWarning("Campo obrigatório", "Selecione uma categoria para o produto.");
                categoriaComboBox.requestFocus();
                return;
            }
            
            // Preencher o objeto produto com os dados do formulário
            produtoAtual.setNome(nomeField.getText().trim());
            produtoAtual.setDescricao(descricaoArea.getText().trim());
            
            if (!tipoComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setTipo(tipoComboBox.getSelectionModel().getSelectedItem());
            }
            
            if (!marcaComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setMarca(marcaComboBox.getSelectionModel().getSelectedItem());
            }
            
            if (!subcategoriaComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setSubcategoria(subcategoriaComboBox.getSelectionModel().getSelectedItem());
            }
            produtoAtual.setDataVencimento(vencimentoPicker.getValue());
            produtoAtual.setUnidade(unidadeField.getText().trim());
            
            if (!categoriaComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setCategoria(categoriaComboBox.getSelectionModel().getSelectedItem());
            }
            
            produtoAtual.setCodigoBarra(codigoBarrasField.getText().trim());
            produtoAtual.setCodigo(codigoProdutoField.getText().trim());
            
            if (!corComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setCor(corComboBox.getSelectionModel().getSelectedItem());
            }
            
            if (!tamanhoComboBox.getSelectionModel().isEmpty()) {
                produtoAtual.setTamanho(tamanhoComboBox.getSelectionModel().getSelectedItem());
            }
            
            // Valores numéricos
            try {
                String custoStr = custoField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
                produtoAtual.setCusto(new BigDecimal(custoStr));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Custo inválido. Use o formato 0,00.");
                custoField.requestFocus();
                return;
            }
            
            try {
                String valorStr = valorField.getText().replace("R$", "").replace(".", "").replace(",", ".").trim();
                produtoAtual.setPreco(new BigDecimal(valorStr));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Preço inválido. Use o formato 0,00.");
                valorField.requestFocus();
                return;
            }
            
            produtoAtual.setCfop(cfopField.getText().trim());
            
            try {
                String icmsStr = icmsField.getText().replace("%", "").replace(",", ".").trim();
                produtoAtual.setIcms(new BigDecimal(icmsStr));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "ICMS inválido. Use o formato 0,00.");
                icmsField.requestFocus();
                return;
            }
            
            try {
                String icmsSubStr = icmsSubField.getText().replace("%", "").replace(",", ".").trim();
                produtoAtual.setIcmsSub(new BigDecimal(icmsSubStr));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "ICMS Substituto inválido. Use o formato 0,00.");
                icmsSubField.requestFocus();
                return;
            }
            
            try {
                produtoAtual.setEstoqueAtual(Integer.parseInt(estoqueField.getText().trim()));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque inválido. Digite um número inteiro.");
                estoqueField.requestFocus();
                return;
            }
            
            try {
                produtoAtual.setEstoqueMinimo(Integer.parseInt(estoqueMinField.getText().trim()));
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Valor inválido", "Estoque mínimo inválido. Digite um número inteiro.");
                estoqueMinField.requestFocus();
                return;
            }
            
            // Campos adicionais
            produtoAtual.setAtivo(true);
            produtoAtual.setDataCadastro(modoEdicao ? produtoAtual.getDataCadastro() : LocalDateTime.now());
            produtoAtual.setDataAtualizacao(LocalDateTime.now());
            
            // Salvar o produto
            if (modoEdicao) {
                produtoDAO.update(produtoAtual);
                AlertUtil.showInfo("Sucesso", "Produto atualizado com sucesso!");
                System.out.println("Custo salvo: " + produtoAtual.getCusto());
            } else {
                produtoDAO.create(produtoAtual);
                AlertUtil.showInfo("Sucesso", "Produto cadastrado com sucesso!");
            }
            
            // Limpar o formulário e iniciar um novo produto
            novoProduto();
            
        } catch (Exception e) {
            AlertUtil.showError("Erro ao salvar produto", e.getMessage());
        }
    }
    
    /**
     * Cancela o cadastro/edição e fecha a tela
     */
    private void cancelarCadastro() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancelar cadastro");
        confirmDialog.setHeaderText("Deseja realmente cancelar?");
        confirmDialog.setContentText("Todas as alterações serão perdidas.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Obter a janela atual e fechá-la
                Stage stage = (Stage) cancelarButton.getScene().getWindow();
                stage.close();
            }
        });
    }
    
    /**
     * Abre o cadastro de marca
     */
    private void abrirCadastroMarca() {
        // Esta é uma implementação simplificada
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Marca");
        dialog.setHeaderText("Cadastrar nova marca");
        dialog.setContentText("Nome da marca:");
        
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                try {
                    Marca marca = new Marca();
                    marca.setNome(nome.trim());
                    marca.setAtivo(true);
                    marcaDAO.create(marca);
                    
                    // Recarregar a lista de marcas
                    carregarMarcas();
                    
                    // Selecionar a nova marca
                    for (int i = 0; i < marcas.size(); i++) {
                        if (marcas.get(i).getNome().equals(nome.trim())) {
                            marcaComboBox.getSelectionModel().select(i);
                            break;
                        }
                    }
                    
                    AlertUtil.showInfo("Sucesso", "Marca cadastrada com sucesso!");
                } catch (SQLException e) {
                    AlertUtil.showError("Erro ao cadastrar marca", e.getMessage());
                }
            }
        });
    }
    
    /**
     * Abre o cadastro de subcategoria
     */
    private void abrirCadastroSubcategoria() {
        // Esta é uma implementação simplificada
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Subcategoria");
        dialog.setHeaderText("Cadastrar nova subcategoria");
        dialog.setContentText("Nome da subcategoria:");
        
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                try {
                    Subcategoria subcategoria = new Subcategoria();
                    subcategoria.setNome(nome.trim());
                    subcategoria.setAtivo(true);
                    subcategoriaDAO.create(subcategoria);
                    
                    // Recarregar a lista de subcategorias
                    carregarSubcategorias();
                    
                    // Selecionar a nova subcategoria
                    for (int i = 0; i < subcategorias.size(); i++) {
                        if (subcategorias.get(i).getNome().equals(nome.trim())) {
                            subcategoriaComboBox.getSelectionModel().select(i);
                            break;
                        }
                    }
                    
                    AlertUtil.showInfo("Sucesso", "Subcategoria cadastrada com sucesso!");
                } catch (SQLException e) {
                    AlertUtil.showError("Erro ao cadastrar subcategoria", e.getMessage());
                }
            }
        });
    }
    
    /**
     * Abre o cadastro de categoria
     */
    private void abrirCadastroCategoria() {
        // Esta é uma implementação simplificada
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Categoria");
        dialog.setHeaderText("Cadastrar nova categoria");
        dialog.setContentText("Nome da categoria:");
        
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty()) {
                try {
                    Categoria categoria = new Categoria();
                    categoria.setNome(nome.trim());
                    categoria.setAtivo(true);
                    categoriaDAO.create(categoria);
                    
                    // Recarregar a lista de categorias
                    carregarCategorias();
                    
                    // Selecionar a nova categoria
                    for (int i = 0; i < categorias.size(); i++) {
                        if (categorias.get(i).getNome().equals(nome.trim())) {
                            categoriaComboBox.getSelectionModel().select(i);
                            break;
                        }
                    }
                    
                    AlertUtil.showInfo("Sucesso", "Categoria cadastrada com sucesso!");
                } catch (SQLException e) {
                    AlertUtil.showError("Erro ao cadastrar categoria", e.getMessage());
                }
            }
        });
    }
    
    /**
     * Abre o cadastro de cor
     */
    private void abrirCadastroCor() {
        // Esta é uma implementação simplificada
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nova Cor");
        dialog.setHeaderText("Cadastrar nova cor");
        dialog.setContentText("Nome da cor:");
        
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty() && !cores.contains(nome.trim())) {
                cores.add(nome.trim());
                corComboBox.setItems(cores);
                corComboBox.getSelectionModel().select(nome.trim());
                AlertUtil.showInfo("Sucesso", "Cor cadastrada com sucesso!");
            }
        });
    }
    
    /**
     * Abre o cadastro de tamanho
     */
    private void abrirCadastroTamanho() {
        // Esta é uma implementação simplificada
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Novo Tamanho");
        dialog.setHeaderText("Cadastrar novo tamanho");
        dialog.setContentText("Tamanho:");
        
        dialog.showAndWait().ifPresent(nome -> {
            if (!nome.trim().isEmpty() && !tamanhos.contains(nome.trim())) {
                tamanhos.add(nome.trim());
                tamanhoComboBox.setItems(tamanhos);
                tamanhoComboBox.getSelectionModel().select(nome.trim());
                AlertUtil.showInfo("Sucesso", "Tamanho cadastrado com sucesso!");
            }
        });
    }
}
                