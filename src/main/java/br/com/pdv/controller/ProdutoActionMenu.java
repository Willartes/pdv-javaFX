package br.com.pdv.controller;

import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.model.Produto;
import br.com.pdv.util.AlertUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Classe utilitária para gerenciar o menu de ações dos produtos na listagem
 */
public class ProdutoActionMenu {

    /**
     * Cria e exibe o menu de contexto para ações sobre um produto
     * 
     * @param tableView A tabela que contém os produtos
     * @param stage O palco principal da aplicação
     */
    public static void showContextMenu(TableView<Produto> tableView, Stage stage) {
        // Criar o menu de contexto
        ContextMenu contextMenu = new ContextMenu();
        
        // Opção para editar produto
        MenuItem editarItem = new MenuItem("Editar Produto");
        editarItem.setOnAction(event -> {
            Produto produtoSelecionado = tableView.getSelectionModel().getSelectedItem();
            if (produtoSelecionado != null) {
                abrirTelaProduto(produtoSelecionado, stage);
            } else {
                AlertUtil.showWarning("Aviso", "Selecione um produto para editar.");
            }
        });
        
        // Opção para cancelar/inativar produto
        MenuItem cancelarItem = new MenuItem("Inativar Produto");
        cancelarItem.setOnAction(event -> {
            Produto produtoSelecionado = tableView.getSelectionModel().getSelectedItem();
            if (produtoSelecionado != null) {
                inativarProduto(produtoSelecionado, tableView);
            } else {
                AlertUtil.showWarning("Aviso", "Selecione um produto para inativar.");
            }
        });
        
        // Adicionar itens ao menu
        contextMenu.getItems().addAll(editarItem, cancelarItem);
        
        // Configurar o comportamento para exibir o menu
        tableView.setContextMenu(contextMenu);
        
        // Também permitir duplo clique para editar
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Produto produtoSelecionado = tableView.getSelectionModel().getSelectedItem();
                if (produtoSelecionado != null) {
                    abrirTelaProduto(produtoSelecionado, stage);
                }
            }
        });
    }
    
    /**
     * Abre a tela de cadastro de produto para edição
     * 
     * @param produto O produto a ser editado
     * @param ownerStage O palco proprietário
     */
    private static void abrirTelaProduto(Produto produto, Stage ownerStage) {
        try {
            // Carregar o FXML da tela de cadastro de produto
            FXMLLoader loader = new FXMLLoader(ProdutoActionMenu.class.getResource("/br/com/pdv/gui/views/NovoProdutoView.fxml"));
            Parent root = loader.load();
            
            // Obter o controlador e configurar para edição
            NovoProdutoController controller = loader.getController();
            controller.editarProduto(produto);
            
            // Configurar e exibir a janela
            Stage stage = new Stage();
            stage.setTitle("Editar Produto");
            stage.setScene(new Scene(root));
            stage.initOwner(ownerStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
        } catch (IOException e) {
            AlertUtil.showError("Erro", "Não foi possível abrir a tela de edição: " + e.getMessage());
        }
    }
    
    /**
     * Inativa um produto e atualiza a tabela
     * 
     * @param produto O produto a ser inativado
     * @param tableView A tabela a ser atualizada
     */
    private static void inativarProduto(Produto produto, TableView<Produto> tableView) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Inativar Produto");
        confirmDialog.setHeaderText("Deseja realmente inativar este produto?");
        confirmDialog.setContentText("O produto \"" + produto.getNome() + "\" será marcado como inativo.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Marcar o produto como inativo
                    produto.setAtivo(false);
                    
                    // Atualizar no banco de dados
                    ProdutoDAO.getInstance().update(produto);
                    
                    // Remover o item da tabela
                    tableView.getItems().remove(produto);
                    
                    AlertUtil.showInfo("Sucesso", "Produto inativado com sucesso!");
                    
                } catch (SQLException e) {
                    AlertUtil.showError("Erro", "Não foi possível inativar o produto: " + e.getMessage());
                }
            }
        });
    }
}