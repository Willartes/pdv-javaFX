package br.com.pdv.dao;

import br.com.pdv.model.MovimentoCaixa;
import br.com.pdv.model.Caixa;
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

public class MovimentoCaixaDAO {

    private static MovimentoCaixaDAO instance;
    private final DatabaseConnection databaseConnection;

    // SQL statements - Modificadas para refletir a estrutura real da tabela
    private static final String SQL_INSERT = 
            "INSERT INTO movimentos_caixa (fluxo_caixa_id, tipo, valor, descricao, data_hora, usuario_id, forma_pagamento) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = 
            "UPDATE movimentos_caixa SET fluxo_caixa_id = ?, tipo = ?, valor = ?, descricao = ?, data_hora = ?, " +
            "usuario_id = ?, forma_pagamento = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM movimentos_caixa WHERE id = ?";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM movimentos_caixa WHERE id = ?";
    private static final String SQL_SELECT_BY_CAIXA = 
            "SELECT * FROM movimentos_caixa WHERE fluxo_caixa_id = ? ORDER BY data_hora";
    private static final String SQL_SELECT_ALL = "SELECT * FROM movimentos_caixa ORDER BY data_hora";

    private MovimentoCaixaDAO() {
        this.databaseConnection = DatabaseConnection.getInstance();
    }

    public static synchronized MovimentoCaixaDAO getInstance() {
        if (instance == null) {
            instance = new MovimentoCaixaDAO();
        }
        return instance;
    }

    public MovimentoCaixa create(MovimentoCaixa movimento) throws SQLException {
        Objects.requireNonNull(movimento, "Movimento não pode ser nulo");
        Objects.requireNonNull(movimento.getCaixa(), "Caixa do movimento não pode ser nulo");
        Objects.requireNonNull(movimento.getUsuario(), "Usuário do movimento não pode ser nulo");

        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Iniciando criação de movimento de caixa - Tipo: %s, Valor: %s", 
                        transactionId, movimento.getTipo(), movimento.getValor()));

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, movimento.getCaixa().getId());
            stmt.setString(2, movimento.getTipo());
            stmt.setBigDecimal(3, movimento.getValor());
            stmt.setString(4, movimento.getDescricao());
            stmt.setTimestamp(5, Timestamp.valueOf(movimento.getDataHora()));
            stmt.setInt(6, movimento.getUsuario().getId());
            // Tratar formaPagamento como opcional
            if (movimento.getFormaPagamento() != null) {
                stmt.setString(7, movimento.getFormaPagamento());
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                String errorMsg = String.format("[%s] Falha ao criar movimento, nenhuma linha afetada.", transactionId);
                LogUtil.error(MovimentoCaixaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }

            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                movimento.setId(rs.getInt(1));
                LogUtil.info(MovimentoCaixaDAO.class, 
                        String.format("[%s] Movimento criado com sucesso. ID: %d", transactionId, movimento.getId()));
                return movimento;
            } else {
                String errorMsg = String.format("[%s] Falha ao criar movimento, nenhum ID obtido.", transactionId);
                LogUtil.error(MovimentoCaixaDAO.class, errorMsg, null);
                throw new SQLException(errorMsg);
            }
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao criar movimento: %s", transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public MovimentoCaixa findById(Integer id) throws SQLException {
        Objects.requireNonNull(id, "ID não pode ser nulo");

        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Buscando movimento por ID: %d", transactionId, id));

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_ID);
            stmt.setInt(1, id);

            rs = stmt.executeQuery();

            if (rs.next()) {
                MovimentoCaixa movimento = construirMovimentoCaixa(rs, conn);
                LogUtil.info(MovimentoCaixaDAO.class, 
                        String.format("[%s] Movimento encontrado - ID: %d", transactionId, id));
                return movimento;
            }

            LogUtil.warn(MovimentoCaixaDAO.class, 
                    String.format("[%s] Nenhum movimento encontrado com ID: %d", transactionId, id));
            return null;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao buscar movimento por ID: %s", transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<MovimentoCaixa> findByCaixa(Caixa caixa) throws SQLException {
        Objects.requireNonNull(caixa, "Caixa não pode ser nulo");
        Objects.requireNonNull(caixa.getId(), "ID do caixa não pode ser nulo");

        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Listando movimentos do caixa ID: %d", transactionId, caixa.getId()));

        List<MovimentoCaixa> movimentos = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_BY_CAIXA);
            stmt.setInt(1, caixa.getId());

            rs = stmt.executeQuery();

            while (rs.next()) {
                movimentos.add(construirMovimentoCaixa(rs, conn));
            }

            LogUtil.info(MovimentoCaixaDAO.class, 
                    String.format("[%s] Total de movimentos encontrados para o caixa: %d", 
                            transactionId, movimentos.size()));
            return movimentos;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar movimentos por caixa: %s", 
                    transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public List<MovimentoCaixa> findAll() throws SQLException {
        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Listando todos os movimentos de caixa", transactionId));

        List<MovimentoCaixa> movimentos = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_SELECT_ALL);

            rs = stmt.executeQuery();

            while (rs.next()) {
                movimentos.add(construirMovimentoCaixa(rs, conn));
            }

            LogUtil.info(MovimentoCaixaDAO.class, 
                    String.format("[%s] Total de movimentos encontrados: %d", 
                            transactionId, movimentos.size()));
            return movimentos;
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao listar movimentos: %s", transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    public boolean update(MovimentoCaixa movimento) throws SQLException {
        Objects.requireNonNull(movimento, "Movimento não pode ser nulo");
        Objects.requireNonNull(movimento.getId(), "ID do movimento não pode ser nulo");
        Objects.requireNonNull(movimento.getCaixa(), "Caixa do movimento não pode ser nulo");
        Objects.requireNonNull(movimento.getUsuario(), "Usuário do movimento não pode ser nulo");

        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Iniciando atualização de movimento. ID: %d", 
                        transactionId, movimento.getId()));

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_UPDATE);

            stmt.setInt(1, movimento.getCaixa().getId());
            stmt.setString(2, movimento.getTipo());
            stmt.setBigDecimal(3, movimento.getValor());
            stmt.setString(4, movimento.getDescricao());
            stmt.setTimestamp(5, Timestamp.valueOf(movimento.getDataHora()));
            stmt.setInt(6, movimento.getUsuario().getId());
            // Tratar formaPagamento como opcional
            if (movimento.getFormaPagamento() != null) {
                stmt.setString(7, movimento.getFormaPagamento());
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
            }
            stmt.setInt(8, movimento.getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                LogUtil.info(MovimentoCaixaDAO.class, 
                        String.format("[%s] Movimento atualizado com sucesso. ID: %d", 
                                transactionId, movimento.getId()));
                return true;
            } else {
                LogUtil.warn(MovimentoCaixaDAO.class, 
                        String.format("[%s] Nenhum movimento atualizado - ID: %d", 
                                transactionId, movimento.getId()));
                return false;
            }
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao atualizar movimento: %s", transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    public boolean delete(Integer id) throws SQLException {
        Objects.requireNonNull(id, "ID não pode ser nulo");

        UUID transactionId = UUID.randomUUID();
        LogUtil.info(MovimentoCaixaDAO.class, 
                String.format("[%s] Excluindo movimento. ID: %d", transactionId, id));

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = databaseConnection.getConnection();
            stmt = conn.prepareStatement(SQL_DELETE);

            stmt.setInt(1, id);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                LogUtil.info(MovimentoCaixaDAO.class, 
                        String.format("[%s] Movimento excluído com sucesso. ID: %d", 
                                transactionId, id));
                return true;
            } else {
                LogUtil.warn(MovimentoCaixaDAO.class, 
                        String.format("[%s] Nenhum movimento excluído - ID: %d", 
                                transactionId, id));
                return false;
            }
        } catch (SQLException e) {
            String errorMsg = String.format("[%s] Erro ao excluir movimento: %s", transactionId, e.getMessage());
            LogUtil.error(MovimentoCaixaDAO.class, errorMsg, e);
            throw new SQLException(errorMsg, e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    private MovimentoCaixa construirMovimentoCaixa(ResultSet rs, Connection conn) throws SQLException {
        MovimentoCaixa movimento = new MovimentoCaixa();

        movimento.setId(rs.getInt("id"));
        movimento.setTipo(rs.getString("tipo"));
        movimento.setValor(rs.getBigDecimal("valor"));
        movimento.setDescricao(rs.getString("descricao"));
        movimento.setDataHora(rs.getTimestamp("data_hora").toLocalDateTime());
        
        // Buscar a forma de pagamento se estiver disponível
        try {
            String formaPagamento = rs.getString("forma_pagamento");
            movimento.setFormaPagamento(formaPagamento);
        } catch (SQLException e) {
            // Ignorar se o campo não existir
            LogUtil.warn(MovimentoCaixaDAO.class, "Campo forma_pagamento não encontrado: " + e.getMessage());
        }

        // Buscar o caixa associado
        CaixaDAO caixaDAO = CaixaDAO.getInstance();
        int fluxoCaixaId = rs.getInt("fluxo_caixa_id");
        Caixa caixa = caixaDAO.findById(fluxoCaixaId);
        movimento.setCaixa(caixa);
        
        // Buscar o usuário associado
        UsuarioDAO usuarioDAO = UsuarioDAO.getInstance();
        int usuarioId = rs.getInt("usuario_id");
        movimento.setUsuario(usuarioDAO.findById(usuarioId));

        return movimento;
    }

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LogUtil.warn(MovimentoCaixaDAO.class, "Erro ao fechar ResultSet: " + e.getMessage());
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LogUtil.warn(MovimentoCaixaDAO.class, "Erro ao fechar Statement: " + e.getMessage());
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LogUtil.warn(MovimentoCaixaDAO.class, "Erro ao fechar Connection: " + e.getMessage());
            }
        }
    }
}