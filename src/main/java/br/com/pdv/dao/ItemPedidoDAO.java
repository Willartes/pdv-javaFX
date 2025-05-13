package br.com.pdv.dao;

import br.com.pdv.controller.DashboardController;
import br.com.pdv.model.ItemPedido;
import br.com.pdv.model.Pedido;
import br.com.pdv.model.Produto;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.DatabaseUtil;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemPedidoDAO {
    private static ItemPedidoDAO instance;
    private final DatabaseConnection databaseConnection;
    
    private static final String SQL_INSERT = 
        "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, " +
        "preco_unitario, desconto) VALUES (?, ?, ?, ?, ?)";
        
    private static final String SQL_UPDATE = 
        "UPDATE itens_pedido SET quantidade = ?, preco_unitario = ?, " +
        "desconto = ? WHERE id = ?";
        
    private static final String SQL_DELETE = "DELETE FROM itens_pedido WHERE id = ?";
    private static final String SQL_DELETE_BY_PEDIDO = "DELETE FROM itens_pedido WHERE pedido_id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM itens_pedido WHERE id = ?";
    private static final String SQL_SELECT_BY_PEDIDO = "SELECT * FROM itens_pedido WHERE pedido_id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM itens_pedido ORDER BY id";
    
    private ItemPedidoDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized ItemPedidoDAO getInstance() {
        if (instance == null) {
            instance = new ItemPedidoDAO();
        }
        return instance;
    }

    public ItemPedido create(ItemPedido itemPedido) throws SQLException {
        LogUtil.info(ItemPedidoDAO.class, 
            String.format("Criando item pedido - Pedido ID: %d, Produto ID: %d", 
                itemPedido.getPedido().getId(), itemPedido.getProduto().getId()));
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, itemPedido.getPedido().getId());
            stmt.setInt(2, itemPedido.getProduto().getId());
            stmt.setInt(3, itemPedido.getQuantidade());
            stmt.setBigDecimal(4, itemPedido.getValorUnitario());
            stmt.setBigDecimal(5, itemPedido.getDesconto());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                SQLException e = new SQLException("Falha ao criar item pedido, nenhuma linha afetada.");
                LogUtil.error(ItemPedidoDAO.class, "Falha ao criar item pedido", e);
                throw e;
            }
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    itemPedido.setId(rs.getInt(1));
                    LogUtil.info(ItemPedidoDAO.class, "Item pedido criado com ID: " + itemPedido.getId());
                    return itemPedido;
                } else {
                    SQLException e = new SQLException("Falha ao criar item pedido, nenhum ID obtido.");
                    LogUtil.error(ItemPedidoDAO.class, "Falha ao criar item pedido: ID não gerado", e);
                    throw e;
                }
            }
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao criar item pedido: " + e.getMessage(), e);
            throw e;
        }
    }

    public ItemPedido findById(Integer id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_ID)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ItemPedido item = createItemPedidoFromResultSet(rs, conn);
                    LogUtil.info(ItemPedidoDAO.class, "Item pedido encontrado - ID: " + id);
                    return item;
                }
                LogUtil.info(ItemPedidoDAO.class, "Item pedido não encontrado - ID: " + id);
                return null;
            }
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao buscar item pedido por ID: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Método para obter os produtos mais vendidos
     * 
     * @param limit Número máximo de produtos a serem retornados
     * @return Lista com os produtos mais vendidos
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<DashboardController.ProdutoVendas> obterProdutosMaisVendidos(int limit) throws SQLException {
        List<DashboardController.ProdutoVendas> produtosMaisVendidos = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT p.id, p.nome, " +
                         "SUM(ip.quantidade) as quantidade_total, " +
                         "SUM(ip.valor_total) as valor_total " +
                         "FROM itens_pedido ip " +
                         "JOIN produtos p ON ip.produto_id = p.id " +
                         "JOIN pedidos pe ON ip.pedido_id = pe.id " +
                         "JOIN vendas v ON pe.id = v.pedido_id " +
                         "WHERE v.status = 'FINALIZADA' " +
                         "GROUP BY p.id, p.nome " +
                         "ORDER BY quantidade_total DESC " +
                         "LIMIT ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                int idProduto = rs.getInt("id");
                String nomeProduto = rs.getString("nome");
                int quantidadeVendida = rs.getInt("quantidade_total");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                
                DashboardController.ProdutoVendas produtoVendas = 
                    new DashboardController.ProdutoVendas(idProduto, nomeProduto, quantidadeVendida, valorTotal);
                
                produtosMaisVendidos.add(produtoVendas);
            }
            
            return produtosMaisVendidos;
            
        } finally {
            DatabaseUtil.closeResources(conn, stmt, rs);
        }
    }

    public List<ItemPedido> findByPedido(Pedido pedido) throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        List<Integer> itemIds = new ArrayList<>();
        List<Integer> produtoIds = new ArrayList<>();
        List<Integer> quantidades = new ArrayList<>();
        List<BigDecimal> precosUnitarios = new ArrayList<>();
        List<BigDecimal> descontos = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_BY_PEDIDO)) {
            
            stmt.setInt(1, pedido.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Coletar todos os dados necessários do ResultSet
                    itemIds.add(rs.getInt("id"));
                    produtoIds.add(rs.getInt("produto_id"));
                    quantidades.add(rs.getInt("quantidade"));
                    precosUnitarios.add(rs.getBigDecimal("preco_unitario"));
                    descontos.add(rs.getBigDecimal("desconto"));
                }
            }
            
            // Uma vez que os dados foram coletados e o ResultSet está fechado,
            // podemos construir os objetos ItemPedido
            ProdutoDAO produtoDAO = ProdutoDAO.getInstance();
            
            for (int i = 0; i < itemIds.size(); i++) {
                // Buscar o produto
                Produto produto = produtoDAO.findById(produtoIds.get(i));
                
                // Criar o ItemPedido
                ItemPedido itemPedido = new ItemPedido(produto, quantidades.get(i));
                itemPedido.setId(itemIds.get(i));
                itemPedido.setPedido(pedido);
                itemPedido.setValorUnitario(precosUnitarios.get(i));
                itemPedido.setDesconto(descontos.get(i));
                
                itens.add(itemPedido);
            }
            
            LogUtil.info(ItemPedidoDAO.class, 
                String.format("Encontrados %d itens para o pedido ID: %d", 
                    itens.size(), pedido.getId()));
            return itens;
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao buscar itens do pedido: " + e.getMessage(), e);
            throw e;
        }
    }

    public List<ItemPedido> findAll() throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                itens.add(createItemPedidoFromResultSet(rs, conn));
            }
            LogUtil.info(ItemPedidoDAO.class, "Total de itens encontrados: " + itens.size());
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao listar todos os itens: " + e.getMessage(), e);
            throw e;
        }
        return itens;
    }
    
    /**
     * Cria um novo item de pedido usando uma conexão fornecida
     * 
     * @param item O item de pedido a ser criado
     * @param conn A conexão com o banco de dados
     * @return O item de pedido criado com o ID gerado
     * @throws SQLException Se ocorrer um erro de SQL
     */
    
    public ItemPedido create(ItemPedido item, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
        	
        	String sql = "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario) " + 
        	             "VALUES (?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setLong(1, item.getPedido().getId());
            stmt.setLong(2, item.getProduto().getId());
            stmt.setInt(3, item.getQuantidade());
            stmt.setBigDecimal(4, item.getValorUnitario()); // Este valor vai para preco_unitario
            
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar item de pedido, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            
            if (generatedKeys.next()) {
                item.setId(generatedKeys.getInt(1)); // Usando getLong
                
                LogUtil.info(ItemPedidoDAO.class, "Criando item pedido - Pedido ID: " + 
                            item.getPedido().getId() + ", Produto ID: " + item.getProduto().getId());
                
                return item;
            } else {
                throw new SQLException("Falha ao criar item de pedido, nenhum ID obtido.");
            }
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao criar item pedido: " + e.getMessage(), e);
            throw e;
        } finally {
            // Fechamos apenas os recursos, mas não a conexão
            if (generatedKeys != null) generatedKeys.close();
            if (stmt != null) stmt.close();
        }
    }
    
    
    
    
    public boolean update(ItemPedido itemPedido) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            
            stmt.setInt(1, itemPedido.getQuantidade());
            stmt.setBigDecimal(2, itemPedido.getValorUnitario());
            stmt.setBigDecimal(3, itemPedido.getDesconto());
            stmt.setInt(4, itemPedido.getId());
            
            boolean updated = stmt.executeUpdate() > 0;
            if (updated) {
                LogUtil.info(ItemPedidoDAO.class, "Item pedido atualizado com sucesso - ID: " + itemPedido.getId());
            } else {
                LogUtil.warn(ItemPedidoDAO.class, "Nenhum item pedido atualizado - ID: " + itemPedido.getId());
            }
            return updated;
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao atualizar item pedido: " + e.getMessage(), e);
            throw e;
        }
    }

    public boolean delete(Integer id) throws SQLException {
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE)) {
            
            stmt.setInt(1, id);
            
            boolean deleted = stmt.executeUpdate() > 0;
            if (deleted) {
                LogUtil.info(ItemPedidoDAO.class, "Item pedido excluído com sucesso - ID: " + id);
            } else {
            	LogUtil.warn(ItemPedidoDAO.class, "Nenhum item excluído do pedido - ID: " + id);
            }
            return deleted;
        } catch (SQLException e) {
            LogUtil.error(ItemPedidoDAO.class, "Erro ao excluir itens do pedido: " + e.getMessage(), e);
            throw e;
        }
    }

    private ItemPedido createItemPedidoFromResultSet(ResultSet rs, Connection conn) throws SQLException {
        // Primeiro obtém o produto para poder usar o construtor adequado
        ProdutoDAO produtoDAO = ProdutoDAO.getInstance();
        Produto produto = produtoDAO.findById(rs.getInt("produto_id"));
        
        // Cria o ItemPedido usando o construtor com produto e quantidade
        ItemPedido itemPedido = new ItemPedido(
            produto,
            rs.getInt("quantidade")
        );
        
        // Define os demais atributos
        itemPedido.setId(rs.getInt("id"));
        itemPedido.setValorUnitario(rs.getBigDecimal("preco_unitario"));
        itemPedido.setDesconto(rs.getBigDecimal("desconto"));

        // valorTotal é calculado automaticamente pelos setters
        
        return itemPedido;
    }
}