package br.com.pdv.controller;

import br.com.pdv.dao.PedidoDAO;
import br.com.pdv.dao.ItemPedidoDAO;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.model.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador para operações relacionadas a pedidos.
 */
public class PedidoController {

    private static final Logger logger = Logger.getLogger(PedidoController.class.getName());
    private final PedidoDAO pedidoDAO;
    private final ItemPedidoDAO itemPedidoDAO;
    private final ProdutoDAO produtoDAO;

    // Construtor com instâncias Singleton
    public PedidoController() {
        this.pedidoDAO = PedidoDAO.getInstance();
        this.itemPedidoDAO = ItemPedidoDAO.getInstance();
        this.produtoDAO = ProdutoDAO.getInstance();
    }

    // Construtor com injeção de dependência (útil para testes)
    public PedidoController(PedidoDAO pedidoDAO, ItemPedidoDAO itemPedidoDAO, ProdutoDAO produtoDAO) {
        this.pedidoDAO = pedidoDAO;
        this.itemPedidoDAO = itemPedidoDAO;
        this.produtoDAO = produtoDAO;
    }

    /**
     * Inicia um novo pedido para um cliente com um vendedor específico.
     *
     * @param cliente O cliente que está realizando o pedido
     * @param vendedor O usuário vendedor responsável pelo pedido
     * @return O pedido criado
     */
    public Pedido iniciarPedido(Cliente cliente, Usuario vendedor) {
        try {
            validarClienteAtivo(cliente);
            
            // Criar novo pedido
            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setUsuario(vendedor);
            pedido.setDataPedido(LocalDateTime.now());
            pedido.setValorTotal(BigDecimal.ZERO);
            pedido.setItens(new ArrayList<>());
            
            // Persistir no banco
            return pedidoDAO.create(pedido);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao iniciar pedido", e);
            throw new RuntimeException("Erro ao iniciar pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Adiciona um item ao pedido com base no código do produto e quantidade.
     *
     * @param pedido O pedido ao qual o item será adicionado
     * @param codigoProduto O código do produto a ser adicionado
     * @param quantidade A quantidade do produto a ser adicionada
     */
    public void adicionarItem(Pedido pedido, String codigoProduto, int quantidade) {
        try {
            Produto produto = buscarEValidarProduto(codigoProduto, quantidade);

            // Criar o item
            ItemPedido item = new ItemPedido(produto, quantidade);
            item.setPedido(pedido);
            
            // Se o pedido já tem itens, adicionar à lista existente
            if (pedido.getItens() == null) {
                pedido.setItens(new ArrayList<>());
            }
            pedido.getItens().add(item);
            
            // Recalcular o valor total do pedido
            atualizarValorTotalPedido(pedido);
            
            // Persistir o item e o pedido atualizado
            itemPedidoDAO.create(item);
            pedidoDAO.update(pedido);
            
            logger.info("Item adicionado ao pedido " + pedido.getId() + ": " + 
                      "Produto: " + produto.getNome() + ", Quantidade: " + quantidade);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao adicionar item ao pedido", e);
            throw new RuntimeException("Erro ao adicionar item ao pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Remove um item do pedido.
     *
     * @param pedido O pedido do qual o item será removido
     * @param item O item a ser removido
     */
    public void removerItem(Pedido pedido, ItemPedido item) {
        try {
            // Remover o item da lista do pedido
            if (pedido.getItens() != null) {
                pedido.getItens().remove(item);
            }
            
            // Recalcular o valor total do pedido
            atualizarValorTotalPedido(pedido);
            
            // Persistir a remoção e o pedido atualizado
            itemPedidoDAO.delete(item.getId());
            pedidoDAO.update(pedido);
            
            logger.info("Item removido do pedido " + pedido.getId() + ": " + 
                      "ID do item: " + item.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao remover item do pedido", e);
            throw new RuntimeException("Erro ao remover item do pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Recalcula o valor total do pedido com base nos itens.
     *
     * @param pedido O pedido a ter seu valor recalculado
     */
    private void atualizarValorTotalPedido(Pedido pedido) {
        BigDecimal valorTotal = BigDecimal.ZERO;
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                valorTotal = valorTotal.add(item.getValorTotal());
            }
        }
        pedido.setValorTotal(valorTotal);
    }

    /**
     * Finaliza um pedido, atualizando o estoque dos produtos.
     *
     * @param pedido O pedido a ser finalizado
     */
    public void finalizarPedido(Pedido pedido) {
        try {
            validarItensPedido(pedido);
            
            // Atualizar estoque dos produtos
            atualizarEstoque(pedido);
            
            // Persistir o pedido finalizado
            pedidoDAO.update(pedido);
            
            logger.info("Pedido finalizado: " + pedido.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao finalizar pedido", e);
            throw new RuntimeException("Erro ao finalizar pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Cancela um pedido, removendo-o do banco de dados.
     *
     * @param pedido O pedido a ser cancelado
     */
    public void cancelarPedido(Pedido pedido) {
        try {
            // Persistir o pedido cancelado
            pedidoDAO.delete(pedido.getId());
            
            logger.info("Pedido cancelado: " + pedido.getId());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao cancelar pedido", e);
            throw new RuntimeException("Erro ao cancelar pedido: " + e.getMessage(), e);
        }
    }

    /**
     * Busca um pedido pelo ID.
     *
     * @param id O ID do pedido a ser buscado
     * @return O pedido encontrado
     */
    public Pedido buscarPedido(Integer id) {
        try {
            return pedidoDAO.findById(id);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar pedido", e);
            throw new RuntimeException("Erro ao buscar pedido: " + e.getMessage(), e);
        }
    }
    
    /**
     * Busca todos os pedidos cadastrados.
     *
     * @return Lista de todos os pedidos
     */
    public List<Pedido> buscarTodosPedidos() {
        try {
            return pedidoDAO.findAll();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar todos os pedidos", e);
            throw new RuntimeException("Erro ao buscar todos os pedidos: " + e.getMessage(), e);
        }
    }

    /**
     * Valida se o cliente está ativo.
     *
     * @param cliente O cliente a ser validado
     */
    private void validarClienteAtivo(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente não pode ser nulo");
        }
        if (!cliente.isAtivo()) {
            throw new IllegalArgumentException("Cliente inativo não pode realizar pedidos");
        }
    }

    /**
     * Busca um produto pelo código e valida se pode ser vendido.
     *
     * @param codigoProduto O código do produto a ser buscado
     * @param quantidade A quantidade desejada para venda
     * @return O produto encontrado
     */
    private Produto buscarEValidarProduto(String codigoProduto, int quantidade) {
        try {
            Produto produto = produtoDAO.findByCodigo(codigoProduto);
            if (produto == null) {
                throw new IllegalArgumentException("Produto não encontrado: " + codigoProduto);
            }
            if (!produto.isAtivo()) {
                throw new IllegalArgumentException("Produto inativo não pode ser vendido");
            }
            if (quantidade <= 0) {
                throw new IllegalArgumentException("Quantidade deve ser maior que zero");
            }
            if (produto.getEstoqueAtual() < quantidade) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto: " + produto.getNome());
            }
            return produto;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar produto", e);
            throw new RuntimeException("Erro ao buscar produto: " + e.getMessage(), e);
        }
    }

    /**
     * Valida se um pedido possui itens.
     *
     * @param pedido O pedido a ser validado
     */
    private void validarItensPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido não pode ser nulo");
        }
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Pedido não possui itens");
        }
    }

    /**
     * Atualiza o estoque dos produtos do pedido.
     *
     * @param pedido O pedido cujos produtos terão o estoque atualizado
     * @throws SQLException Se ocorrer um erro ao atualizar o estoque
     */
    private void atualizarEstoque(Pedido pedido) throws SQLException {
        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                Produto produto = item.getProduto();
                // Reduzir o estoque do produto
                int novoEstoque = produto.getEstoqueAtual() - item.getQuantidade();
                if (novoEstoque < 0) {
                    throw new IllegalStateException("Estoque insuficiente para o produto: " + produto.getNome());
                }
                produto.setEstoqueAtual(novoEstoque);
                
                // Persistir a atualização do produto
                produtoDAO.update(produto);
                
                logger.info("Estoque atualizado para produto " + produto.getId() + 
                          ": Novo estoque = " + novoEstoque);
            }
        }
    }
}