package br.com.pdv.dao;

import br.com.pdv.model.Venda;
import br.com.pdv.model.Cliente;
import br.com.pdv.model.ItemPedido;
import br.com.pdv.model.Parcela;
import br.com.pdv.model.Pedido;
import br.com.pdv.model.Usuario;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * Classe responsável por realizar operações de acesso a dados para a entidade Venda.
 */
public class VendaDAO {
    
    private static final Logger logger = Logger.getLogger(VendaDAO.class);
    private static VendaDAO instance;
    private final DatabaseConnection databaseConnection;
    
    // SQL statements
    private static final String SQL_INSERT = 
            "INSERT INTO vendas (pedido_id, cliente_id, usuario_id, data_venda, " +
            "valor_total, valor_desconto, valor_pago, troco, forma_pagamento, " +
            "status, numero_nf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = 
            "UPDATE vendas SET pedido_id = ?, cliente_id = ?, usuario_id = ?, " +
            "data_venda = ?, valor_total = ?, valor_desconto = ?, valor_pago = ?, " +
            "troco = ?, forma_pagamento = ?, status = ?, numero_nf = ?, " +
            "data_cancelamento = ?, motivo_cancelamento = ? " +
            "WHERE id = ?";
    private static final String SQL_CANCEL = 
            "UPDATE vendas SET status = 'CANCELADA', " +
            "data_cancelamento = ?, motivo_cancelamento = ? " +
            "WHERE id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM vendas WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM vendas ORDER BY data_venda DESC";
    private static final String SQL_SELECT_BY_PERIOD = 
            "SELECT * FROM vendas WHERE data_venda BETWEEN ? AND ? ORDER BY data_venda DESC";
    private static final String SQL_SELECT_BY_CLIENTE = 
            "SELECT * FROM vendas WHERE cliente_id = ? ORDER BY data_venda DESC";
    private static final String SQL_SELECT_BY_USUARIO = 
            "SELECT * FROM vendas WHERE usuario_id = ? ORDER BY data_venda DESC";
    private static final String SQL_SELECT_TOTAL_BY_PERIOD = 
            "SELECT SUM(valor_total) FROM vendas " +
            "WHERE data_venda BETWEEN ? AND ? AND status = 'FINALIZADA'";
    private static final String SQL_INSERT_ITEM = 
            "INSERT INTO itens_pedido (venda_id, produto_id, quantidade, preco_unitario, valor_total) " +
            "VALUES (?, ?, ?, ?, ?)";
    
    private VendaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized VendaDAO getInstance() {
        if (instance == null) {
            instance = new VendaDAO();
        }
        return instance;
    }
    
    /**
     * Cria uma nova venda no banco de dados.
     * 
     * @param venda A venda a ser criada
     * @return A venda com o ID gerado
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Venda create(Venda venda) throws SQLException {
        Objects.requireNonNull(venda, "Venda não pode ser nula");
        Objects.requireNonNull(venda.getPedido(), "Pedido da venda não pode ser nulo");
        Objects.requireNonNull(venda.getUsuario(), "Usuário da venda não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Iniciando criação de venda para o pedido ID: %d", 
                transactionId, venda.getPedido().getId()));
        
        // Certifique-se de que a data da venda está definida
        if (venda.getDataVenda() == null) {
            venda.setDataVenda(LocalDateTime.now());
        }
        
        // Certifique-se de que o status está definido
        if (venda.getStatus() == null || venda.getStatus().isEmpty()) {
            venda.setStatus("FINALIZADA");
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = databaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, venda.getPedido().getId());
            stmt.setObject(2, venda.getCliente() != null ? venda.getCliente().getId() : null);
            stmt.setInt(3, venda.getUsuario().getId());
            stmt.setTimestamp(4, Timestamp.valueOf(venda.getDataVenda()));
            stmt.setBigDecimal(5, venda.getValorTotal());
            stmt.setBigDecimal(6, venda.getValorDesconto());
            stmt.setBigDecimal(7, venda.getValorPago());
            stmt.setBigDecimal(8, venda.getTroco());
            stmt.setString(9, venda.getFormaPagamento());
            stmt.setString(10, venda.getStatus());
            stmt.setString(11, venda.getNumeroNF());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                conn.rollback();
                String errorMsg = String.format("[%s] Falha ao criar venda, nenhuma linha afetada.", transactionId);
                LogUtil.error(VendaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }
            
            generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                venda.setId(generatedKeys.getInt(1));
                
                // Insere as parcelas, se existirem
                ParcelaDAO parcelaDAO = ParcelaDAO.getInstance();
                if (venda.getParcelas() != null && !venda.getParcelas().isEmpty()) {
                    for (Parcela parcela : venda.getParcelas()) {
                        parcela.setVenda(venda);
                        parcelaDAO.create(parcela, conn);
                    }
                }
                
                // Insere os itens da venda, se existirem
                if (venda.getItens() != null && !venda.getItens().isEmpty()) {
                    saveItems(venda, conn);
                }
                
                // Atualiza o status do pedido para FINALIZADO
                String updatePedidoSQL = "UPDATE pedidos SET status = 'FINALIZADO' WHERE id = ?";
                try (PreparedStatement pedidoStmt = conn.prepareStatement(updatePedidoSQL)) {
                    pedidoStmt.setInt(1, venda.getPedido().getId());
                    int pedidoUpdated = pedidoStmt.executeUpdate();
                    LogUtil.info(VendaDAO.class, String.format("[%s] Status do pedido atualizado: %d linhas afetadas", 
                            transactionId, pedidoUpdated));
                }
                
                conn.commit();
                LogUtil.info(VendaDAO.class, String.format("[%s] Venda criada com sucesso. ID: %d", 
                        transactionId, venda.getId()));
                return venda;
            } else {
                conn.rollback();
                String errorMsg = String.format("[%s] Falha ao criar venda, nenhum ID obtido.", transactionId);
                LogUtil.error(VendaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LogUtil.error(VendaDAO.class, "Erro ao fazer rollback", ex);
                }
            }
            String errorMsg = String.format("[%s] Erro ao inserir venda: %s", transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, generatedKeys);
        }
    }
    
    /**
     * Salva os itens de uma venda
     * 
     * @param venda A venda contendo os itens
     * @param conn A conexão com o banco de dados
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    private void saveItems(Venda venda, Connection conn) throws SQLException {
        if (venda.getItens() == null || venda.getItens().isEmpty()) {
            return;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT_ITEM)) {
            for (ItemPedido item : venda.getItens()) {
                stmt.setInt(1, venda.getId());
                stmt.setLong(2, item.getProduto().getId());
                stmt.setInt(3, item.getQuantidade());
                stmt.setBigDecimal(4, item.getValorUnitario());
                stmt.setBigDecimal(5, item.getValorTotal());
                
                stmt.executeUpdate();
            }
        }
    }
    
    /**
     * Atualiza uma venda existente no banco de dados.
     * 
     * @param venda A venda a ser atualizada
     * @return true se a venda foi atualizada com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean update(Venda venda) throws SQLException {
        Objects.requireNonNull(venda, "Venda não pode ser nula");
        Objects.requireNonNull(venda.getId(), "ID da venda não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Iniciando atualização de venda. ID: %d", 
                transactionId, venda.getId()));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            stmt = conn.prepareStatement(SQL_UPDATE);
            
            stmt.setInt(1, venda.getPedido().getId());
            stmt.setObject(2, venda.getCliente() != null ? venda.getCliente().getId() : null);
            stmt.setInt(3, venda.getUsuario().getId());
            stmt.setTimestamp(4, Timestamp.valueOf(venda.getDataVenda()));
            stmt.setBigDecimal(5, venda.getValorTotal());
            stmt.setBigDecimal(6, venda.getValorDesconto());
            stmt.setBigDecimal(7, venda.getValorPago());
            stmt.setBigDecimal(8, venda.getTroco());
            stmt.setString(9, venda.getFormaPagamento());
            stmt.setString(10, venda.getStatus());
            stmt.setString(11, venda.getNumeroNF());
            
            if (venda.getDataCancelamento() != null) {
                stmt.setTimestamp(12, Timestamp.valueOf(venda.getDataCancelamento()));
            } else {
                stmt.setNull(12, java.sql.Types.TIMESTAMP);
            }
            
            stmt.setString(13, venda.getMotivoCancelamento());
            stmt.setInt(14, venda.getId());
            
            int affectedRows = stmt.executeUpdate();
            conn.commit();
            
            if (affectedRows > 0) {
                LogUtil.info(VendaDAO.class, String.format("[%s] Venda atualizada com sucesso. ID: %d", 
                        transactionId, venda.getId()));
                return true;
            } else {
                LogUtil.warn(VendaDAO.class, String.format("[%s] Nenhuma venda atualizada - ID: %d", 
                        transactionId, venda.getId()));
                return false;
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LogUtil.error(VendaDAO.class, "Erro ao realizar rollback", ex);
                }
            }
            String errorMsg = String.format("[%s] Erro ao atualizar venda: %s", transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
    
    
    /**
     * Conta o número de vendas finalizadas
     * 
     * @return Número de vendas finalizadas
     * @throws SQLException Em caso de erro no banco de dados
     */
    public int contarVendasFinalizadas() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT COUNT(*) FROM vendas WHERE status = 'FINALIZADA'";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
            
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Calcula o valor total de todas as vendas finalizadas
     * 
     * @return Valor total das vendas
     * @throws SQLException Em caso de erro no banco de dados
     */
    public BigDecimal calcularValorTotalVendas() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT SUM(valor_total) FROM vendas WHERE status = 'FINALIZADA'";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal total = rs.getBigDecimal(1);
                return total != null ? total : BigDecimal.ZERO;
            }
            
            return BigDecimal.ZERO;
            
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Cancela uma venda no banco de dados.
     * 
     * @param venda A venda a ser cancelada
     * @param motivo O motivo do cancelamento
     * @return true se a venda foi cancelada com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean cancelar(Venda venda, String motivo) throws SQLException {
        Objects.requireNonNull(venda, "Venda não pode ser nula");
        Objects.requireNonNull(venda.getId(), "ID da venda não pode ser nulo");
        Objects.requireNonNull(motivo, "Motivo do cancelamento não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Iniciando cancelamento de venda. ID: %d", 
                transactionId, venda.getId()));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Primeiro cancela as parcelas, se existirem
            ParcelaDAO parcelaDAO = ParcelaDAO.getInstance();
            if (venda.getParcelas() != null && !venda.getParcelas().isEmpty()) {
                for (Parcela parcela : venda.getParcelas()) {
                    parcelaDAO.cancelar(parcela.getId(), conn);
                }
            }
            
            stmt = conn.prepareStatement(SQL_CANCEL);
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, motivo);
            stmt.setInt(3, venda.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            // Restaura o status do pedido para ABERTO
            if (venda.getPedido() != null) {
                try (PreparedStatement pedidoStmt = conn.prepareStatement(
                        "UPDATE pedidos SET status = 'ABERTO' WHERE id = ?")) {
                    pedidoStmt.setInt(1, venda.getPedido().getId());
                    pedidoStmt.executeUpdate();
                    
                    // Atualiza o objeto Pedido localmente
                    venda.getPedido().setStatus("ABERTO");
                }
            }
            
            conn.commit();
            
            // Atualiza o objeto venda
            if (affectedRows > 0) {
                venda.setStatus("CANCELADA");
                venda.cancelar(motivo);
                LogUtil.info(VendaDAO.class, String.format("[%s] Venda cancelada com sucesso. ID: %d", 
                        transactionId, venda.getId()));
                return true;
            } else {
                LogUtil.warn(VendaDAO.class, String.format("[%s] Nenhuma venda cancelada - ID: %d", 
                        transactionId, venda.getId()));
                return false;
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LogUtil.error(VendaDAO.class, "Erro ao realizar rollback", ex);
                }
            }
            String errorMsg = String.format("[%s] Erro ao cancelar venda: %s", transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
    
    /**
     * Busca uma venda pelo ID.
     * 
     * @param id O ID da venda
     * @return A venda encontrada ou null se não existir
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Venda findById(Integer id) throws SQLException {
        Objects.requireNonNull(id, "ID não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Buscando venda por ID: %d", transactionId, id));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Capturamos os dados básicos da venda
                int vendaId = rs.getInt("id");
                int pedidoId = rs.getInt("pedido_id");
                Integer clienteId = rs.getObject("cliente_id") != null ? rs.getInt("cliente_id") : null;
                int usuarioId = rs.getInt("usuario_id");
                LocalDateTime dataVenda = rs.getTimestamp("data_venda").toLocalDateTime();
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                BigDecimal valorDesconto = rs.getBigDecimal("valor_desconto");
                BigDecimal valorPago = rs.getBigDecimal("valor_pago");
                BigDecimal troco = rs.getBigDecimal("troco");
                String formaPagamento = rs.getString("forma_pagamento");
                String status = rs.getString("status");
                String numeroNF = rs.getString("numero_nf");
                Timestamp dataCancelamentoTs = rs.getTimestamp("data_cancelamento");
                LocalDateTime dataCancelamento = dataCancelamentoTs != null ? dataCancelamentoTs.toLocalDateTime() : null;
                String motivoCancelamento = rs.getString("motivo_cancelamento");
                
                // Fechamos o ResultSet antes de fazer outras consultas
                rs.close();
                rs = null;
                
                // Buscamos as entidades relacionadas
                PedidoDAO pedidoDAO = PedidoDAO.getInstance();
                Pedido pedido = pedidoDAO.findById(pedidoId);
                
                Cliente cliente = null;
                if (clienteId != null) {
                    ClienteDAO clienteDAO = ClienteDAO.getInstance();
                    cliente = clienteDAO.findById(clienteId.longValue());
                }
                
                UsuarioDAO usuarioDAO = UsuarioDAO.getInstance();
                Usuario usuario = usuarioDAO.findById(usuarioId);
                
                // Construímos a venda
                Venda venda = new Venda();
                venda.setId(vendaId);
                venda.setPedido(pedido);
                venda.setCliente(cliente);
                venda.setUsuario(usuario);
                venda.setDataVenda(dataVenda);
                venda.setValorTotal(valorTotal);
                venda.setValorDesconto(valorDesconto);
                venda.setValorPago(valorPago);
                venda.setTroco(troco);
                venda.setFormaPagamento(formaPagamento);
                venda.setStatus(status);
                venda.setNumeroNF(numeroNF);
                
                // Tratamos o cancelamento, se aplicável
                if (dataCancelamento != null && "CANCELADA".equals(status)) {
                    venda.cancelar(motivoCancelamento);
                }
                
                // Buscamos as parcelas
                ParcelaDAO parcelaDAO = ParcelaDAO.getInstance();
                List<Parcela> parcelas = parcelaDAO.findByVenda(vendaId);
                venda.setParcelas(parcelas);
                
                LogUtil.info(VendaDAO.class, String.format("[%s] Venda encontrada - ID: %d", transactionId, id));
                return venda;
            }
            
            LogUtil.warn(VendaDAO.class, String.format("[%s] Nenhuma venda encontrada com ID: %d", transactionId, id));
            return null;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao buscar venda por ID: %s", transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Lista todas as vendas.
     * 
     * @return Uma lista com todas as vendas
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public List<Venda> findAll() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Listando todas as vendas", transactionId));
        
        List<Venda> vendas = new ArrayList<>();
        List<Integer> vendaIds = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement("SELECT id FROM vendas ORDER BY data_venda DESC");
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                vendaIds.add(rs.getInt("id"));
            }
            
            // Fechamos os recursos antes de fazer outras consultas
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            
            // Buscamos cada venda individualmente
            for (Integer vendaId : vendaIds) {
                try {
                    Venda venda = findById(vendaId);
                    if (venda != null) {
                        vendas.add(venda);
                    }
                } catch (SQLException e) {
                    LogUtil.warn(VendaDAO.class, String.format("[%s] Erro ao buscar venda ID %d: %s", 
                            transactionId, vendaId, e.getMessage()));
                    // Continuamos mesmo se houver erro em uma venda específica
                }
            }
            
            LogUtil.info(VendaDAO.class, String.format("[%s] Total de vendas encontradas: %d", 
                    transactionId, vendas.size()));
            return vendas;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar vendas: %s", transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Busca vendas em um período específico.
     *
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de vendas no período
     * @throws SQLException Se houver um erro na operação de banco de dados
     */
    
    /*
    public List<Venda> findByPeriod(LocalDateTime dataInicio, LocalDateTime dataFim) throws SQLException {
        Objects.requireNonNull(dataInicio, "Data de início não pode ser nula");
        Objects.requireNonNull(dataFim, "Data de fim não pode ser nula");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Listando vendas por período de %s a %s", 
                transactionId, dataInicio, dataFim));
        
        List<Venda> vendas = new ArrayList<>();
        List<Integer> vendaIds = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_PERIOD);
            
            stmt.setTimestamp(1, Timestamp.valueOf(dataInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(dataFim));
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                vendaIds.add(rs.getInt("id"));
            }
            
            // Fechamos os recursos antes de fazer outras consultas
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            
            // Buscamos cada venda individualmente
            for (Integer vendaId : vendaIds) {
                try {
                    Venda venda = findById(vendaId);
                    if (venda != null) {
                        vendas.add(venda);
                    }
                } catch (SQLException e) {
                    LogUtil.warn(VendaDAO.class, String.format("[%s] Erro ao buscar venda ID %d: %s", 
                            transactionId, vendaId, e.getMessage()));
                    // Continuamos mesmo se houver erro em uma venda específica
                }
            }
            
            LogUtil.info(VendaDAO.class, String.format("[%s] Total de vendas encontradas no período: %d", 
                    transactionId, vendas.size()));
            return vendas;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar vendas por período: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }*/
    
    /**
     * Busca vendas por período
     * 
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Lista de vendas no período
     * @throws SQLException Em caso de erro no banco de dados
     */
    public List<Venda> findByPeriod(LocalDateTime dataInicio, LocalDateTime dataFim) throws SQLException {
        List<Venda> vendas = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            
            String sql = "SELECT * FROM vendas " +
                         "WHERE data_venda BETWEEN ? AND ? " +
                         "AND status = 'FINALIZADA' " +
                         "ORDER BY data_venda";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Venda venda = mapResultSetToVenda(rs);
                vendas.add(venda);
            }
            
            return vendas;
            
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    
    /**
     * Mapeia um ResultSet para um objeto Venda
     * 
     * @param rs ResultSet contendo os dados
     * @return Objeto Venda preenchido
     * @throws SQLException Em caso de erro ao acessar os dados
     */
    private Venda mapResultSetToVenda(ResultSet rs) throws SQLException {
        Venda venda = new Venda();
        
        venda.setId(rs.getInt("id"));
        venda.setDataVenda(rs.getTimestamp("data_venda").toLocalDateTime());
        venda.setValorTotal(rs.getBigDecimal("valor_total"));
        venda.setValorDesconto(rs.getBigDecimal("valor_desconto"));
        venda.setStatus(rs.getString("status"));
        venda.setFormaPagamento(rs.getString("forma_pagamento"));
        
        // Se você tiver relacionamentos com outras entidades (ex: Cliente, Usuário),
        // você precisa fazer joins na consulta SQL ou carregar esses dados aqui
        // usando seus respectivos DAOs
        
        // Por exemplo:
        Integer clienteId = rs.getInt("cliente_id");
        if (!rs.wasNull()) {
            Cliente cliente = new Cliente();
            cliente.setId(clienteId);
            venda.setCliente(cliente);
            
            // Opcionalmente, carregar o cliente completo:
            // ClienteDAO clienteDAO = ClienteDAO.getInstance();
            // venda.setCliente(clienteDAO.findById(clienteId));
        }
        
        Integer usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            Usuario usuario = new Usuario();
            usuario.setId(usuarioId);
            venda.setUsuario(usuario);
        }
        
        // Se você também quiser carregar os itens do pedido associados:
        // ItemPedidoDAO itemPedidoDAO = ItemPedidoDAO.getInstance();
        // venda.setItens(itemPedidoDAO.findByVendaId(venda.getId()));
        
        return venda;
    }
    
    
    /**
     * Lista as vendas de um cliente.
     * 
     * @param clienteId O ID do cliente
     * @return Uma lista com as vendas do cliente
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public List<Venda> findByCliente(Long clienteId) throws SQLException {
        Objects.requireNonNull(clienteId, "ID do cliente não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Listando vendas do cliente ID: %d", 
                transactionId, clienteId));
        
        List<Venda> vendas = new ArrayList<>();
        List<Integer> vendaIds = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_CLIENTE);
            
            stmt.setLong(1, clienteId);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                vendaIds.add(rs.getInt("id"));
            }
            
            // Fechamos os recursos antes de fazer outras consultas
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            
            // Buscamos cada venda individualmente
            for (Integer vendaId : vendaIds) {
                try {
                    Venda venda = findById(vendaId);
                    if (venda != null) {
                        vendas.add(venda);
                    }
                } catch (SQLException e) {
                    LogUtil.warn(VendaDAO.class, String.format("[%s] Erro ao buscar venda ID %d: %s", 
                            transactionId, vendaId, e.getMessage()));
                    // Continuamos mesmo se houver erro em uma venda específica
                }
            }
            
            LogUtil.info(VendaDAO.class, String.format("[%s] Total de vendas encontradas para o cliente: %d", 
                    transactionId, vendas.size()));
            return vendas;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar vendas por cliente: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Lista as vendas realizadas por um usuário.
     *
     * @param usuarioId O ID do usuário
     * @return Uma lista com as vendas do usuário
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public List<Venda> findByUsuario(Integer usuarioId) throws SQLException {
        Objects.requireNonNull(usuarioId, "ID do usuário não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Listando vendas do usuário ID: %d", 
                transactionId, usuarioId));
        
        List<Venda> vendas = new ArrayList<>();
        List<Integer> vendaIds = new ArrayList<>();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_USUARIO);
            
            stmt.setInt(1, usuarioId);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                vendaIds.add(rs.getInt("id"));
            }
            
            // Fechamos os recursos antes de fazer outras consultas
            rs.close();
            rs = null;
            stmt.close();
            stmt = null;
            
            // Buscamos cada venda individualmente
            for (Integer vendaId : vendaIds) {
                try {
                    Venda venda = findById(vendaId);
                    if (venda != null) {
                        vendas.add(venda);
                    }
                } catch (SQLException e) {
                    LogUtil.warn(VendaDAO.class, String.format("[%s] Erro ao buscar venda ID %d: %s", 
                            transactionId, vendaId, e.getMessage()));
                    // Continuamos mesmo se houver erro em uma venda específica
                }
            }
            
            LogUtil.info(VendaDAO.class, String.format("[%s] Total de vendas encontradas para o usuário: %d", 
                    transactionId, vendas.size()));
            return vendas;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar vendas por usuário: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Calcula o total de vendas em um período específico.
     *
     * @param dataInicio Data de início do período
     * @param dataFim Data de fim do período
     * @return Total de vendas no período
     * @throws SQLException Se houver um erro na operação de banco de dados
     */
    public BigDecimal calcularTotalVendasPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) throws SQLException {
        Objects.requireNonNull(dataInicio, "Data de início não pode ser nula");
        Objects.requireNonNull(dataFim, "Data de fim não pode ser nula");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(VendaDAO.class, String.format("[%s] Consultando total de vendas por período de %s a %s", 
                transactionId, dataInicio, dataFim));
        
        BigDecimal total = BigDecimal.ZERO;
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_TOTAL_BY_PERIOD);
            
            stmt.setTimestamp(1, Timestamp.valueOf(dataInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(dataFim));
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal resultado = rs.getBigDecimal(1);
                if (resultado != null) {
                    total = resultado;
                }
            }
            
            LogUtil.info(VendaDAO.class, String.format("[%s] Total de vendas no período: %s", 
                    transactionId, total));
            return total;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao calcular total de vendas por período: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(VendaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Fecha os recursos utilizados (conexão, statement e resultset).
     * Garante que os recursos sejam fechados adequadamente para evitar vazamentos de memória.
     * 
     * @param conn Conexão com o banco de dados
     * @param stmt Statement SQL
     * @param rs ResultSet
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LogUtil.warn(VendaDAO.class, "Erro ao fechar ResultSet: " + e.getMessage());
            }
        }
        
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LogUtil.warn(VendaDAO.class, "Erro ao fechar Statement: " + e.getMessage());
            }
        }
        
        if (conn != null) {
            try {
                // Restaurar autoCommit antes de fechar, se necessário
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                conn.close();
            } catch (SQLException e) {
                LogUtil.warn(VendaDAO.class, "Erro ao fechar Connection: " + e.getMessage());
            }
        }
    }

    /**
     * Salva uma nova venda ou atualiza uma existente.
     * Se a venda já tiver ID, atualiza; caso contrário, insere uma nova.
     * 
     * @param venda A venda a ser salva
     * @return O ID da venda salva
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Integer save(Venda venda) throws SQLException {
        if (venda.getId() != null && venda.getId() > 0) {
            update(venda);
            return venda.getId();
        } else {
            Venda novavenda = create(venda);
            return novavenda.getId();
        }
    }
    
}