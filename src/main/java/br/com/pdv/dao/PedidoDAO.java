package br.com.pdv.dao;

import br.com.pdv.model.Cliente;
import br.com.pdv.model.ItemPedido;
import br.com.pdv.model.Pedido;
import br.com.pdv.model.Usuario;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe DAO para manipulação de dados de pedidos no banco de dados.
 */
public class PedidoDAO {

    private static final Logger logger = Logger.getLogger(PedidoDAO.class.getName());
    private static PedidoDAO instance;
    private final DatabaseConnection databaseConnection;
    
    // Lazy loading para dependências
    private ItemPedidoDAO itemPedidoDAO;
    private ProdutoDAO produtoDAO;
    private UsuarioDAO usuarioDAO;
    
    private static final String SQL_INSERT = "INSERT INTO pedidos (cliente_id, usuario_id, vendedor_id, data_pedido, valor_total, status) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM pedidos WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM pedidos";
    private static final String SQL_UPDATE = "UPDATE pedidos SET cliente_id = ?, usuario_id = ?, vendedor_id = ?, data_pedido = ?, valor_total = ?, status = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM pedidos WHERE id = ?";
    private static final String SQL_SELECT_BY_STATUS = "SELECT * FROM pedidos WHERE status = ?";
    
    // Constantes para status
    public static final String STATUS_ABERTO = "ABERTO";
    public static final String STATUS_FINALIZADO = "FINALIZADO";
    public static final String STATUS_CANCELADO = "CANCELADO";

    // Valor padrão para vendedor_id
    private static final int DEFAULT_VENDEDOR_ID = 1;

    /**
     * Construtor privado para garantir o padrão Singleton.
     */
    private PedidoDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
        // Dependências serão inicializadas sob demanda
    }

    /**
     * Implementação do padrão Singleton para garantir uma única instância de PedidoDAO.
     *
     * @return A instância única de PedidoDAO.
     */
    public static synchronized PedidoDAO getInstance() {
        if (instance == null) {
            instance = new PedidoDAO();
        }
        return instance;
    }
    
    /**
     * Método auxiliar para obter a instância de ItemPedidoDAO (lazy loading)
     */
    private ItemPedidoDAO getItemPedidoDAO() {
        if (itemPedidoDAO == null) {
            itemPedidoDAO = ItemPedidoDAO.getInstance();
        }
        return itemPedidoDAO;
    }
    
    /**
     * Método auxiliar para obter a instância de ProdutoDAO (lazy loading)
     */
    private ProdutoDAO getProdutoDAO() {
        if (produtoDAO == null) {
            produtoDAO = ProdutoDAO.getInstance();
        }
        return produtoDAO;
    }
    
    /**
     * Método auxiliar para obter a instância de UsuarioDAO (lazy loading)
     */
    private UsuarioDAO getUsuarioDAO() {
        if (usuarioDAO == null) {
            usuarioDAO = UsuarioDAO.getInstance();
        }
        return usuarioDAO;
    }

    /**
     * Cria um novo pedido usando uma conexão fornecida
     * 
     * @param pedido O pedido a ser criado
     * @param conn A conexão com o banco de dados
     * @return O pedido criado com o ID gerado
     * @throws SQLException Se ocorrer um erro de SQL
     */
    
    public Pedido create(Pedido pedido, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
        	String sql = "INSERT INTO pedidos (cliente_id, vendedor_id, usuario_id, data_pedido, valor_total, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        	
        	
        	stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            if (stmt == null) {
                throw new SQLException("Falha ao criar PreparedStatement, objeto é nulo");
            }
            
	       // Verificar se o cliente foi informado
	       if (pedido.getCliente() != null && pedido.getCliente().getId() != null) {
	           stmt.setLong(1, pedido.getCliente().getId());
	       } else {
	           stmt.setNull(1, java.sql.Types.INTEGER);
	       }
	
	       // Verificar se o vendedor foi informado
	       if (pedido.getVendedorId() == null) {
	            throw new SQLException("O ID do vendedor é obrigatório para criar um pedido");
	        }
	        
	        // Definir o vendedor_id no PreparedStatement
	        stmt.setLong(2, pedido.getVendedorId());
	        /**
	       if (pedido.getUsuario() != null && pedido.getUsuario().getId() != null) {
	           stmt.setInt(2, pedido.getUsuario().getId());
	       } else {
	           throw new SQLException("Vendedor é obrigatório e não pode ser nulo");
	       }*/
	
	       // Usuário que criou o pedido (pode ser o mesmo que o vendedor)
	       if (pedido.getUsuario() != null && pedido.getUsuario().getId() != null) {
	           stmt.setInt(3, pedido.getUsuario().getId());
	       } else {
	           throw new SQLException("Usuário é obrigatório e não pode ser nulo");
	       }
	
	       stmt.setTimestamp(4, Timestamp.valueOf(pedido.getDataPedido()));
	       stmt.setBigDecimal(5, pedido.getValorTotal());
	       stmt.setString(6, pedido.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar pedido, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            
            if (generatedKeys.next()) {
                pedido.setId(generatedKeys.getInt(1)); // Usando getLong em vez de getInt
                LogUtil.info(PedidoDAO.class, "Pedido criado com sucesso. ID: " + pedido.getId());
                return pedido;
            } else {
                throw new SQLException("Falha ao criar pedido, nenhum ID obtido.");
            }
        } catch (SQLException e) {
            LogUtil.error(PedidoDAO.class, "Erro ao criar pedido: " + e.getMessage(), e);
            throw e;
        } finally {
            // Fechamos apenas os recursos, mas não a conexão
            if (generatedKeys != null) generatedKeys.close();
            if (stmt != null) stmt.close();
        }
    }
    
    
    /**
     * Cria um novo pedido
     * 
     * @param pedido O pedido a ser criado
     * @return O pedido criado com o ID gerado
     * @throws SQLException Se ocorrer um erro de SQL
     */
    
    public Pedido create(Pedido pedido) throws SQLException {
    	Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            String sql = "INSERT INTO pedidos (cliente_id, usuario_id, vendedor_id, data_pedido, valor_total, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            // Verificar se o cliente foi informado
            if (pedido.getCliente() != null && pedido.getCliente().getId() != null) {
                stmt.setLong(1, pedido.getCliente().getId());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            
            // Verificar se o usuário foi informado
            if (pedido.getUsuario() != null && pedido.getUsuario().getId() != null) {
                stmt.setInt(2, pedido.getUsuario().getId());
            } else {
                throw new SQLException("Usuário é obrigatório e não pode ser nulo");
            }
            
            // Verificar se o vendedor foi informado (usando o método getVendedor)
            if (pedido.getVendedor() != null && pedido.getVendedor().getId() != null) {
                stmt.setInt(3, pedido.getVendedor().getId());
            } else {
                // Se não houver vendedor específico, usar o usuário como vendedor
                if (pedido.getUsuario() != null && pedido.getUsuario().getId() != null) {
                    stmt.setInt(3, pedido.getUsuario().getId());
                } else {
                    throw new SQLException("Vendedor é obrigatório e não pode ser nulo");
                }
            }
            
            stmt.setTimestamp(4, Timestamp.valueOf(pedido.getDataPedido()));
            stmt.setBigDecimal(5, pedido.getValorTotal());
            stmt.setString(6, pedido.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Falha ao criar pedido, nenhuma linha afetada.");
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            
            if (generatedKeys.next()) {
                pedido.setId(generatedKeys.getInt(1));
                LogUtil.info(getClass(), "Pedido criado com sucesso. ID: " + pedido.getId());
                return pedido;
            } else {
                throw new SQLException("Falha ao criar pedido, nenhum ID obtido.");
            }
        } catch (SQLException e) {
            LogUtil.error(getClass(), "Erro ao criar pedido: " + e.getMessage(), e);
            throw e;
        } finally {
            // Fechar recursos
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException e) { /* ignorar */ }
            if (stmt != null) try { stmt.close(); } catch (SQLException e) { /* ignorar */ }
            if (conn != null) databaseConnection.releaseConnection(conn);
        }
    }

    // Método para fechar recursos do banco de dados com segurança
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao fechar ResultSet", e);
            }
        }
        
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao fechar Statement", e);
            }
        }
        
        if (conn != null) {
            try {
                // Restaurar autoCommit antes de fechar
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Erro ao restaurar autoCommit", e);
                }
                
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao fechar Connection", e);
            }
        }
    }

    /**
     * Busca um pedido pelo ID.
     */
    public Pedido findById(Integer id) throws SQLException {
        Objects.requireNonNull(id, "ID não pode ser nulo");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setLong(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Extrair dados básicos do pedido
                Integer pedidoId = rs.getInt("id");
                Integer clienteId = rs.getInt("cliente_id");
                Integer usuarioId = rs.getInt("usuario_id");
                Integer vendedorId = rs.getInt("vendedor_id");
                LocalDateTime dataPedido = rs.getTimestamp("data_pedido").toLocalDateTime();
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                String status = rs.getString("status");
                
                // Fechar ResultSet antes de fazer outras consultas
                rs.close();
                rs = null;
                
                // Buscar entidades relacionadas
                ClienteDAO clienteDAO = ClienteDAO.getInstance();
                Cliente cliente = clienteDAO.findById((long) clienteId);
                
                UsuarioDAO usuarioDAO = getUsuarioDAO();
                Usuario usuario = usuarioDAO.findById(usuarioId);
                
                // Determinar o vendedor
                Usuario vendedor;
                if (vendedorId.equals(usuarioId)) {
                    vendedor = usuario;
                } else {
                    vendedor = usuarioDAO.findById(vendedorId);
                }
                
                // Criar e configurar o pedido
                Pedido pedido = new Pedido();
                pedido.setId(pedidoId);
                pedido.setCliente(cliente);
                pedido.setUsuario(usuario);
                pedido.setVendedor(vendedor);
                pedido.setDataPedido(dataPedido);
                pedido.setValorTotal(valorTotal);
                pedido.setStatus(status);
                
                // Buscar os itens do pedido
                ItemPedidoDAO itemPedidoDAO = getItemPedidoDAO();
                List<ItemPedido> itens = itemPedidoDAO.findByPedido(pedido);
                pedido.setItens(itens);
                
                logger.info("Pedido encontrado - ID: " + id);
                return pedido;
            } else {
                logger.info("Pedido não encontrado - ID: " + id);
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao buscar pedido por ID: " + e.getMessage(), e);
            throw new SQLException("Erro ao buscar pedido por ID: " + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Lista todos os pedidos do banco de dados.
     */
    public List<Pedido> findAll() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();
        Map<Integer, Integer> pedidoToClienteId = new HashMap<>();
        Map<Integer, Integer> pedidoToUsuarioId = new HashMap<>();
        Map<Integer, Integer> pedidoToVendedorId = new HashMap<>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_ALL);
            rs = stmt.executeQuery();

            // Primeira passagem: coletar IDs e dados básicos
            while (rs.next()) {
                Integer pedidoId = rs.getInt("id");
                Integer clienteId = rs.getInt("cliente_id");
                Integer usuarioId = rs.getInt("usuario_id");
                Integer vendedorId = rs.getInt("vendedor_id");
                LocalDateTime dataPedido = rs.getTimestamp("data_pedido").toLocalDateTime();
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                String status = rs.getString("status");
                
                // Criar pedido básico
                Pedido pedido = new Pedido();
                pedido.setId(pedidoId);
                pedido.setDataPedido(dataPedido);
                pedido.setValorTotal(valorTotal);
                pedido.setStatus(status);
                
                // Armazenar mapeamentos de IDs
                pedidoToClienteId.put(pedidoId, clienteId);
                pedidoToUsuarioId.put(pedidoId, usuarioId);
                pedidoToVendedorId.put(pedidoId, vendedorId);
                
                // Adicionar à lista
                pedidos.add(pedido);
            }
            
            // Fechar recursos antes de próximas consultas
            closeResources(null, stmt, rs);
            stmt = null;
            rs = null;
            
            // Segunda passagem: buscar entidades relacionadas
            ClienteDAO clienteDAO = ClienteDAO.getInstance();
            UsuarioDAO usuarioDAO = getUsuarioDAO();
            ItemPedidoDAO itemPedidoDAO = getItemPedidoDAO();
            
            for (Pedido pedido : pedidos) {
                // Buscar cliente
                Integer clienteId = pedidoToClienteId.get(pedido.getId());
                if (clienteId != null) {
                    Cliente cliente = clienteDAO.findById((long) clienteId);
                    pedido.setCliente(cliente);
                }
                
                // Buscar usuário
                Integer usuarioId = pedidoToUsuarioId.get(pedido.getId());
                if (usuarioId != null) {
                    Usuario usuario = usuarioDAO.findById(usuarioId);
                    pedido.setUsuario(usuario);
                }
                
                // Buscar vendedor
                Integer vendedorId = pedidoToVendedorId.get(pedido.getId());
                if (vendedorId != null) {
                    if (vendedorId.equals(usuarioId)) {
                        pedido.setVendedor(pedido.getUsuario());
                    } else {
                        Usuario vendedor = usuarioDAO.findById(vendedorId);
                        pedido.setVendedor(vendedor);
                    }
                }
                
                // Buscar itens
                List<ItemPedido> itens = itemPedidoDAO.findByPedido(pedido);
                pedido.setItens(itens);
            }
            
            logger.info("Total de pedidos encontrados: " + pedidos.size());
            return pedidos;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar pedidos: " + e.getMessage(), e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Atualiza um pedido existente.
     */
    public boolean update(Pedido pedido) throws SQLException {
        Objects.requireNonNull(pedido, "Pedido não pode ser nulo");
        Objects.requireNonNull(pedido.getId(), "ID do pedido não pode ser nulo");
        Objects.requireNonNull(pedido.getCliente(), "Cliente do pedido não pode ser nulo");
        Objects.requireNonNull(pedido.getUsuario(), "Usuário do pedido não pode ser nulo");
        Objects.requireNonNull(pedido.getStatus(), "Status do pedido não pode ser nulo");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);

            int vendedorId = (pedido.getVendedor() != null && pedido.getVendedor().getId() != null)
                    ? pedido.getVendedor().getId()
                    : DEFAULT_VENDEDOR_ID;

            stmt.setLong(1, pedido.getCliente().getId());
            stmt.setLong(2, pedido.getUsuario().getId());
            stmt.setLong(3, vendedorId);
            stmt.setObject(4, pedido.getDataPedido());
            stmt.setBigDecimal(5, pedido.getValorTotal());
            stmt.setString(6, pedido.getStatus());
            stmt.setLong(7, pedido.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Pedido atualizado com sucesso. ID: " + pedido.getId());
                return true;
            } else {
                logger.warning("Nenhum pedido atualizado - ID: " + pedido.getId());
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao atualizar pedido: " + e.getMessage(), e);
            throw new SQLException("Erro ao atualizar pedido: " + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Exclui um pedido do banco de dados.
     */
    public boolean delete(Integer id) throws SQLException {
        Objects.requireNonNull(id, "ID não pode ser nulo");

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = databaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Primeiro exclui os itens do pedido
            try (PreparedStatement stmtItens = conn.prepareStatement("DELETE FROM itens_pedido WHERE pedido_id = ?")) {
                stmtItens.setLong(1, id);
                stmtItens.executeUpdate();
                logger.info("Itens do pedido excluídos - Pedido ID: " + id);
            }

            // Depois exclui o pedido
            stmt = conn.prepareStatement(SQL_DELETE);
            stmt.setLong(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                conn.commit();
                logger.info("Pedido excluído com sucesso - ID: " + id);
                return true;
            } else {
                conn.rollback();
                logger.warning("Nenhum pedido excluído - ID: " + id);
                return false;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Erro ao fazer rollback: " + ex.getMessage(), ex);
                }
            }
            logger.log(Level.SEVERE, "Erro ao excluir pedido: " + e.getMessage(), e);
            throw new SQLException("Erro ao excluir pedido: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE, "Erro ao restaurar autoCommit: " + e.getMessage(), e);
                }
            }
            closeResources(conn, stmt, null);
        }
    }

    /**
     * Busca pedidos por status.
     */
    public List<Pedido> findByStatus(String status) throws SQLException {
        Objects.requireNonNull(status, "Status não pode ser nulo");

        List<Pedido> pedidos = new ArrayList<>();
        Map<Integer, Integer> pedidoToClienteId = new HashMap<>();
        Map<Integer, Integer> pedidoToUsuarioId = new HashMap<>();
        Map<Integer, Integer> pedidoToVendedorId = new HashMap<>();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_STATUS);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            while (rs.next()) {
                // Extrair dados do ResultSet para cada pedido
                Integer pedidoId = rs.getInt("id");
                Integer clienteId = rs.getInt("cliente_id");
                Integer usuarioId = rs.getInt("usuario_id");
                Integer vendedorId = rs.getInt("vendedor_id");
                LocalDateTime dataPedido = rs.getTimestamp("data_pedido").toLocalDateTime();
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                String pedidoStatus = rs.getString("status");
                
                // Criar e configurar o pedido básico
                Pedido pedido = new Pedido();
                pedido.setId(pedidoId);
                pedido.setDataPedido(dataPedido);
                pedido.setValorTotal(valorTotal);
                pedido.setStatus(pedidoStatus);
                
                // Salvar os IDs para carregar depois
                pedidoToClienteId.put(pedidoId, clienteId);
                pedidoToUsuarioId.put(pedidoId, usuarioId);
                pedidoToVendedorId.put(pedidoId, vendedorId);
                
                // Adicionar à lista
                pedidos.add(pedido);
            }
            
            // Fechar ResultSet e Statement antes de fazer outras consultas
            if (rs != null) {
                rs.close();
                rs = null;
            }
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }
            
            // Carregar entidades relacionadas
            ClienteDAO clienteDAO = ClienteDAO.getInstance();
            UsuarioDAO usuarioDAO = getUsuarioDAO();
            ItemPedidoDAO itemPedidoDAO = getItemPedidoDAO();
            
            for (Pedido pedido : pedidos) {
                // Buscar cliente
                Integer clienteId = pedidoToClienteId.get(pedido.getId());
                if (clienteId != null) {
                    Cliente cliente = clienteDAO.findById((long) clienteId);
                    pedido.setCliente(cliente);
                }
                
                // Buscar usuário
                Integer usuarioId = pedidoToUsuarioId.get(pedido.getId());
                if (usuarioId != null) {
                    Usuario usuario = usuarioDAO.findById(usuarioId);
                    pedido.setUsuario(usuario);
                }
                
                // Buscar vendedor
                Integer vendedorId = pedidoToVendedorId.get(pedido.getId());
                if (vendedorId != null) {
                    if (vendedorId.equals(usuarioId)) {
                        // Se o vendedor é o mesmo usuário
                        pedido.setVendedor(pedido.getUsuario());
                    } else {
                        Usuario vendedor = usuarioDAO.findById(vendedorId);
                        pedido.setVendedor(vendedor);
                    }
                }
                
                // Buscar itens
                List<ItemPedido> itens = itemPedidoDAO.findByPedido(pedido);
                pedido.setItens(itens);
            }
            
            logger.info("Total de pedidos encontrados com status " + status + ": " + pedidos.size());
            return pedidos;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar pedidos por status: " + e.getMessage(), e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
}