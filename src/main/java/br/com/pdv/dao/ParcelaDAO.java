package br.com.pdv.dao;

import br.com.pdv.model.Parcela;
import br.com.pdv.model.Venda;
import br.com.pdv.util.DatabaseConnection;
import br.com.pdv.util.LogUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Classe responsável por realizar operações de acesso a dados para a entidade Parcela.
 */
public class ParcelaDAO {
    
    private static ParcelaDAO instance;
    private final DatabaseConnection databaseConnection;
    
    // SQL statements
    private static final String SQL_INSERT = 
            "INSERT INTO parcelas (venda_id, numero_parcela, total_parcelas, " +
            "valor, data_vencimento, data_pagamento, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = 
            "UPDATE parcelas SET numero_parcela = ?, total_parcelas = ?, " +
            "valor = ?, data_vencimento = ?, data_pagamento = ?, status = ? " +
            "WHERE id = ?";
    private static final String SQL_PAY = 
            "UPDATE parcelas SET data_pagamento = ?, status = 'PAGA' WHERE id = ?";
    private static final String SQL_CANCEL = 
            "UPDATE parcelas SET status = 'CANCELADA' WHERE id = ?";
    private static final String SQL_UPDATE_STATUS = 
            "UPDATE parcelas SET status = 'ATRASADA' " +
            "WHERE data_vencimento < ? AND status = 'PENDENTE'";
    private static final String SQL_SELECT_BY_ID = 
            "SELECT * FROM parcelas WHERE id = ?";
    private static final String SQL_SELECT_BY_VENDA = 
            "SELECT * FROM parcelas WHERE venda_id = ? ORDER BY numero_parcela";
    private static final String SQL_SELECT_PENDING = 
            "SELECT * FROM parcelas WHERE status = 'PENDENTE' ORDER BY data_vencimento";
    
    private ParcelaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }
    
    public static synchronized ParcelaDAO getInstance() {
        if (instance == null) {
            instance = new ParcelaDAO();
        }
        return instance;
    }
    
    /**
     * Insere uma nova parcela no banco de dados.
     * 
     * @param parcela A parcela a ser inserida
     * @return A parcela com o ID gerado
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Parcela create(Parcela parcela) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseConnection.getConnection();
            return create(parcela, conn);
        } finally {
            closeResources(conn, null, null);
        }
    }
    
    /**
     * Insere uma nova parcela no banco de dados usando uma conexão fornecida.
     * 
     * @param parcela A parcela a ser inserida
     * @param conn A conexão a ser usada
     * @return A parcela com o ID gerado
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Parcela create(Parcela parcela, Connection conn) throws SQLException {
        Objects.requireNonNull(parcela, "Parcela não pode ser nula");
        Objects.requireNonNull(parcela.getVenda(), "Venda da parcela não pode ser nula");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Iniciando criação de parcela %d/%d da venda ID: %d", 
                        transactionId, parcela.getNumeroParcela(), parcela.getTotalParcelas(), 
                        parcela.getVenda().getId()));
        
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, parcela.getVenda().getId());
            stmt.setInt(2, parcela.getNumeroParcela());
            stmt.setInt(3, parcela.getTotalParcelas());
            stmt.setBigDecimal(4, parcela.getValor());
            stmt.setDate(5, Date.valueOf(parcela.getDataVencimento()));
            
            if (parcela.getDataPagamento() != null) {
                stmt.setDate(6, Date.valueOf(parcela.getDataPagamento()));
            } else {
                stmt.setNull(6, java.sql.Types.DATE);
            }
            
            stmt.setString(7, parcela.getStatus());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                String errorMsg = String.format("[%s] Falha ao criar parcela, nenhuma linha afetada.", 
                        transactionId);
                LogUtil.error(ParcelaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }
            
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                parcela.setId(rs.getInt(1));
                LogUtil.info(ParcelaDAO.class, 
                        String.format("[%s] Parcela criada com sucesso. ID: %d", 
                                transactionId, parcela.getId()));
                return parcela;
            } else {
                String errorMsg = String.format("[%s] Falha ao criar parcela, nenhum ID obtido.", 
                        transactionId);
                LogUtil.error(ParcelaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao criar parcela: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    LogUtil.warn(ParcelaDAO.class, "Erro ao fechar ResultSet: " + e.getMessage());
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LogUtil.warn(ParcelaDAO.class, "Erro ao fechar Statement: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Atualiza uma parcela existente no banco de dados.
     * 
     * @param parcela A parcela a ser atualizada
     * @return true se a parcela foi atualizada com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean update(Parcela parcela) throws SQLException {
        Objects.requireNonNull(parcela, "Parcela não pode ser nula");
        Objects.requireNonNull(parcela.getId(), "ID da parcela não pode ser nulo");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Iniciando atualização de parcela. ID: %d", 
                        transactionId, parcela.getId()));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);
            
            stmt.setInt(1, parcela.getNumeroParcela());
            stmt.setInt(2, parcela.getTotalParcelas());
            stmt.setBigDecimal(3, parcela.getValor());
            stmt.setDate(4, Date.valueOf(parcela.getDataVencimento()));
            
            if (parcela.getDataPagamento() != null) {
                stmt.setDate(5, Date.valueOf(parcela.getDataPagamento()));
            } else {
                stmt.setNull(5, java.sql.Types.DATE);
            }
            
            stmt.setString(6, parcela.getStatus());
            stmt.setInt(7, parcela.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                LogUtil.info(ParcelaDAO.class, 
                        String.format("[%s] Parcela atualizada com sucesso. ID: %d", 
                                transactionId, parcela.getId()));
                return true;
            } else {
                LogUtil.warn(ParcelaDAO.class, 
                        String.format("[%s] Nenhuma parcela atualizada - ID: %d", 
                                transactionId, parcela.getId()));
                return false;
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao atualizar parcela: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
    
    /**
     * Registra o pagamento de uma parcela.
     * 
     * @param id O ID da parcela
     * @param dataPagamento A data do pagamento
     * @return true se o pagamento foi registrado com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean pagar(int id, LocalDate dataPagamento) throws SQLException {
        Objects.requireNonNull(dataPagamento, "Data de pagamento não pode ser nula");
        
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Registrando pagamento da parcela ID: %d na data: %s", 
                        transactionId, id, dataPagamento));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_PAY);
            
            stmt.setDate(1, Date.valueOf(dataPagamento));
            stmt.setInt(2, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                LogUtil.info(ParcelaDAO.class, 
                        String.format("[%s] Pagamento registrado com sucesso para a parcela ID: %d", 
                                transactionId, id));
                return true;
            } else {
                LogUtil.warn(ParcelaDAO.class, 
                        String.format("[%s] Nenhuma parcela atualizada para pagamento - ID: %d", 
                                transactionId, id));
                return false;
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao registrar pagamento de parcela: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
    
    /**
     * Cancela uma parcela.
     * 
     * @param id O ID da parcela
     * @return true se a parcela foi cancelada com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean cancelar(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseConnection.getConnection();
            return cancelar(id, conn);
        } finally {
            closeResources(conn, null, null);
        }
    }
    
    /**
     * Cancela uma parcela usando uma conexão fornecida.
     * 
     * @param id O ID da parcela
     * @param conn A conexão a ser usada
     * @return true se a parcela foi cancelada com sucesso
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public boolean cancelar(int id, Connection conn) throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Cancelando parcela ID: %d", transactionId, id));
        
        PreparedStatement stmt = null;
        
        try {
            stmt = conn.prepareStatement(SQL_CANCEL);
            
            stmt.setInt(1, id);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                LogUtil.info(ParcelaDAO.class, 
                        String.format("[%s] Parcela cancelada com sucesso. ID: %d", 
                                transactionId, id));
                return true;
            } else {
                LogUtil.warn(ParcelaDAO.class, 
                        String.format("[%s] Nenhuma parcela cancelada - ID: %d", 
                                transactionId, id));
                return false;
            }
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao cancelar parcela: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    LogUtil.warn(ParcelaDAO.class, "Erro ao fechar Statement: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Atualiza o status das parcelas que estão atrasadas.
     * 
     * @return O número de parcelas atualizadas
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public int atualizarStatusAtrasadas() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Atualizando status de parcelas atrasadas", transactionId));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE_STATUS);
            
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            
            int affectedRows = stmt.executeUpdate();
            
            LogUtil.info(ParcelaDAO.class, 
                    String.format("[%s] %d parcelas marcadas como atrasadas", 
                            transactionId, affectedRows));
            return affectedRows;
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao atualizar status das parcelas: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }
    
    /**
     * Busca uma parcela pelo ID.
     * 
     * @param id O ID da parcela
     * @return A parcela encontrada ou null se não existir
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public Parcela findById(int id) throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Buscando parcela por ID: %d", transactionId, id));
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                Parcela parcela = construirParcela(rs);
                LogUtil.info(ParcelaDAO.class, 
                        String.format("[%s] Parcela encontrada - ID: %d", transactionId, id));
                return parcela;
            }
            
            LogUtil.warn(ParcelaDAO.class, 
                    String.format("[%s] Parcela não encontrada - ID: %d", transactionId, id));
            return null;
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao buscar parcela por ID: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Lista as parcelas de uma venda.
     * 
     * @param vendaId O ID da venda
     * @return Uma lista com as parcelas da venda
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public List<Parcela> findByVenda(int vendaId) throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Listando parcelas da venda ID: %d", transactionId, vendaId));
        
        List<Parcela> parcelas = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_VENDA);
            
            stmt.setInt(1, vendaId);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                parcelas.add(construirParcela(rs));
            }
            
            LogUtil.info(ParcelaDAO.class, 
                    String.format("[%s] Total de parcelas encontradas para a venda: %d", 
                            transactionId, parcelas.size()));
            return parcelas;
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar parcelas por venda: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Lista as parcelas com pagamento pendente.
     * 
     * @return Uma lista com as parcelas pendentes
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    public List<Parcela> findPendentes() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(ParcelaDAO.class, 
                String.format("[%s] Listando parcelas pendentes", transactionId));
        
        List<Parcela> parcelas = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_PENDING);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                parcelas.add(construirParcela(rs));
            }
            
            LogUtil.info(ParcelaDAO.class, 
                    String.format("[%s] Total de parcelas pendentes encontradas: %d", 
                            transactionId, parcelas.size()));
            return parcelas;
            
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar parcelas pendentes: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(ParcelaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Constrói um objeto Parcela a partir de um ResultSet.
     * 
     * @param rs O ResultSet com os dados da parcela
     * @return O objeto Parcela construído
     * @throws SQLException Se ocorrer algum erro de SQL
     */
    private Parcela construirParcela(ResultSet rs) throws SQLException {
        Parcela parcela = new Parcela();
        
        parcela.setId(rs.getInt("id"));
        parcela.setNumeroParcela(rs.getInt("numero_parcela"));
        parcela.setTotalParcelas(rs.getInt("total_parcelas"));
        parcela.setValor(rs.getBigDecimal("valor"));
        parcela.setDataVencimento(rs.getDate("data_vencimento").toLocalDate());
        
        Date dataPagamento = rs.getDate("data_pagamento");
        if (dataPagamento != null) {
            parcela.setDataPagamento(dataPagamento.toLocalDate());
        }
        
        parcela.setStatus(rs.getString("status"));
        
        // A venda será definida posteriormente por quem chamou o método
        
        return parcela;
    }
    
    /**
     * Fecha os recursos utilizados (conexão, statement e resultset).
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LogUtil.warn(ParcelaDAO.class, "Erro ao fechar ResultSet: " + e.getMessage());
            }
        }
        
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LogUtil.warn(ParcelaDAO.class, "Erro ao fechar Statement: " + e.getMessage());
            }
        }
        
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LogUtil.warn(ParcelaDAO.class, "Erro ao fechar Connection: " + e.getMessage());
            }
        }
    }
}