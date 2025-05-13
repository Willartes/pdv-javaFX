package br.com.pdv.dao;

import br.com.pdv.model.Compra;
import br.com.pdv.model.ItemCompra;
import br.com.pdv.model.Fornecedor;
import br.com.pdv.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompraDAO {
    private Connection connection;
    private ItemCompraDAO itemCompraDAO;
    
    public CompraDAO(Connection connection) {
        this.connection = connection;
        this.itemCompraDAO = new ItemCompraDAO(connection);
    }
    
    public void create(Compra compra) throws SQLException {
        String sql = "INSERT INTO compras (data_compra, fornecedor_id, valor_total, status, " +
                    "numero_nf, usuario_id, observacao, data_cancelamento, motivo_cancelamento) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, compra.getDataCompra());
            pstmt.setInt(2, compra.getFornecedor().getId());
            pstmt.setBigDecimal(3, compra.getValorTotal());
            pstmt.setString(4, compra.getStatus());
            pstmt.setString(5, compra.getNumeroNF());
            pstmt.setInt(6, compra.getUsuario().getId());
            pstmt.setString(7, compra.getObservacao());
            pstmt.setObject(8, compra.getDataCancelamento());
            pstmt.setString(9, compra.getMotivoCancelamento());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    compra.setId(rs.getInt(1));
                    
                    // Salva os itens da compra
                    for (ItemCompra item : compra.getItens()) {
                        item.setCompra(compra);
                        itemCompraDAO.create(item);
                    }
                }
            }
        }
    }
    
    public Compra read(int id) throws SQLException {
        String sql = "SELECT * FROM compras WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Compra compra = createCompraFromResultSet(rs);
                    // Carrega os itens da compra
                    compra.getItens().addAll(itemCompraDAO.readAllByCompra(compra));
                    return compra;
                }
            }
        }
        return null;
    }
    
    public List<Compra> readAll() throws SQLException {
        List<Compra> compras = new ArrayList<>();
        String sql = "SELECT * FROM compras ORDER BY data_compra DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Compra compra = createCompraFromResultSet(rs);
                // Carrega os itens da compra
                compra.getItens().addAll(itemCompraDAO.readAllByCompra(compra));
                compras.add(compra);
            }
        }
        return compras;
    }
    
    public void update(Compra compra) throws SQLException {
        String sql = "UPDATE compras SET data_compra = ?, fornecedor_id = ?, valor_total = ?, " +
                    "status = ?, numero_nf = ?, usuario_id = ?, observacao = ?, " +
                    "data_cancelamento = ?, motivo_cancelamento = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setObject(1, compra.getDataCompra());
            pstmt.setInt(2, compra.getFornecedor().getId());
            pstmt.setBigDecimal(3, compra.getValorTotal());
            pstmt.setString(4, compra.getStatus());
            pstmt.setString(5, compra.getNumeroNF());
            pstmt.setInt(6, compra.getUsuario().getId());
            pstmt.setString(7, compra.getObservacao());
            pstmt.setObject(8, compra.getDataCancelamento());
            pstmt.setString(9, compra.getMotivoCancelamento());
            pstmt.setInt(10, compra.getId());
            
            pstmt.executeUpdate();
            
            // Atualiza os itens
            itemCompraDAO.deleteByCompra(compra.getId());
            for (ItemCompra item : compra.getItens()) {
                item.setCompra(compra);
                itemCompraDAO.create(item);
            }
        }
    }
    
    public void delete(int id) throws SQLException {
        // Primeiro deleta os itens da compra
        itemCompraDAO.deleteByCompra(id);
        
        // Depois deleta a compra
        String sql = "DELETE FROM compras WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
    
    private Compra createCompraFromResultSet(ResultSet rs) throws SQLException {
        Compra compra = new Compra();
        compra.setId(rs.getInt("id"));
        compra.setDataCompra(rs.getObject("data_compra", LocalDateTime.class));
        
        // Carrega o fornecedor
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setId(rs.getInt("fornecedor_id"));
        compra.setFornecedor(fornecedor);
        
        // Carrega o usuário
        Usuario usuario = new Usuario();
        usuario.setId(rs.getInt("usuario_id"));
        compra.setUsuario(usuario);
        
        compra.setNumeroNF(rs.getString("numero_nf"));
        compra.setStatus(rs.getString("status"));
        compra.setObservacao(rs.getString("observacao"));
        
        LocalDateTime dataCancelamento = rs.getObject("data_cancelamento", LocalDateTime.class);
        if (dataCancelamento != null) {
            compra.cancelar(rs.getString("motivo_cancelamento"));
        }
        
        return compra;
    }
    
    public List<Compra> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        List<Compra> compras = new ArrayList<>();
        String sql = "SELECT * FROM compras WHERE data_compra BETWEEN ? AND ? ORDER BY data_compra DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setObject(1, inicio);
            pstmt.setObject(2, fim);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Compra compra = createCompraFromResultSet(rs);
                    compra.getItens().addAll(itemCompraDAO.readAllByCompra(compra));
                    compras.add(compra);
                }
            }
        }
        return compras;
    }
    
    public List<Compra> buscarPorFornecedor(int fornecedorId) throws SQLException {
        List<Compra> compras = new ArrayList<>();
        String sql = "SELECT * FROM compras WHERE fornecedor_id = ? ORDER BY data_compra DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, fornecedorId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Compra compra = createCompraFromResultSet(rs);
                    compra.getItens().addAll(itemCompraDAO.readAllByCompra(compra));
                    compras.add(compra);
                }
            }
        }
        return compras;
    }
}