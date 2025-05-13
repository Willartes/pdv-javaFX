package br.com.pdv.dao;

import br.com.pdv.model.ItemCompra;
import br.com.pdv.model.Compra;
import br.com.pdv.model.Produto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ItemCompraDAO {
    private Connection connection;
    
    public ItemCompraDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void create(ItemCompra item) throws SQLException {
        String sql = "INSERT INTO itens_compra (compra_id, produto_id, quantidade, preco_unitario, " +
                    "desconto, valor_total) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getCompra().getId());
            pstmt.setInt(2, item.getProduto().getId());
            pstmt.setInt(3, item.getQuantidade());
            pstmt.setBigDecimal(4, item.getPrecoUnitario());
            pstmt.setBigDecimal(5, item.getDesconto());
            pstmt.setBigDecimal(6, item.getValorTotal());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
    }
    
    public ItemCompra read(int id) throws SQLException {
        String sql = "SELECT * FROM itens_compra WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createItemCompraFromResultSet(rs);
                }
            }
        }
        return null;
    }
    
    public List<ItemCompra> readAllByCompra(Compra compra) throws SQLException {
        List<ItemCompra> itens = new ArrayList<>();
        String sql = "SELECT * FROM itens_compra WHERE compra_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, compra.getId());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ItemCompra item = createItemCompraFromResultSet(rs);
                    item.setCompra(compra);
                    itens.add(item);
                }
            }
        }
        return itens;
    }
    
    public void update(ItemCompra item) throws SQLException {
        String sql = "UPDATE itens_compra SET quantidade = ?, preco_unitario = ?, " +
                    "desconto = ?, valor_total = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, item.getQuantidade());
            pstmt.setBigDecimal(2, item.getPrecoUnitario());
            pstmt.setBigDecimal(3, item.getDesconto());
            pstmt.setBigDecimal(4, item.getValorTotal());
            pstmt.setInt(5, item.getId());
            
            pstmt.executeUpdate();
        }
    }
    
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM itens_compra WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
    
    public void deleteByCompra(int compraId) throws SQLException {
        String sql = "DELETE FROM itens_compra WHERE compra_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, compraId);
            pstmt.executeUpdate();
        }
    }
    
    private ItemCompra createItemCompraFromResultSet(ResultSet rs) throws SQLException {
        ItemCompra item = new ItemCompra();
        item.setId(rs.getInt("id"));
        
        // Carrega o produto associado
        Produto produto = new Produto();
        produto.setId(rs.getInt("produto_id"));
        item.setProduto(produto);
        
        item.setQuantidade(rs.getInt("quantidade"));
        item.setPrecoUnitario(rs.getBigDecimal("preco_unitario"));
        item.setDesconto(rs.getBigDecimal("desconto"));
        
        return item;
    }
    
    public List<ItemCompra> buscarPorProduto(int produtoId) throws SQLException {
        List<ItemCompra> itens = new ArrayList<>();
        String sql = "SELECT * FROM itens_compra WHERE produto_id = ? ORDER BY id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, produtoId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(createItemCompraFromResultSet(rs));
                }
            }
        }
        return itens;
    }
    
    public BigDecimal calcularTotalCompra(int compraId) throws SQLException {
        String sql = "SELECT SUM(valor_total) as total FROM itens_compra WHERE compra_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, compraId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }
}