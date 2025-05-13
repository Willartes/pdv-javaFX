package br.com.pdv.util;

import br.com.pdv.dao.ItemCompraDAO;
import br.com.pdv.dao.CompraDAO;
import br.com.pdv.model.ItemCompra;
import br.com.pdv.model.Compra;
import br.com.pdv.model.Produto;
import br.com.pdv.model.Fornecedor;
import br.com.pdv.model.Usuario;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TesteItemCompraDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pdv_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }
    
    private static void limparDados(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Desabilita temporariamente as foreign keys
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // Limpa as tabelas
            stmt.execute("TRUNCATE TABLE itens_compra");
            stmt.execute("TRUNCATE TABLE compras");
            stmt.execute("TRUNCATE TABLE fornecedores");
            stmt.execute("TRUNCATE TABLE usuarios");
            
            // Habilita novamente as foreign keys
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            
            System.out.println("Dados limpos com sucesso");
        }
    }
    
    private static Fornecedor criarFornecedor(Connection conn) throws SQLException {
        String sql = "INSERT INTO fornecedores (nome, cpf_cnpj, endereco, telefone, email, ativo) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Fornecedor Teste");
            pstmt.setString(2, "12.345.678/0001-90");
            pstmt.setString(3, "Rua Teste, 123");
            pstmt.setString(4, "(11) 1234-5678");
            pstmt.setString(5, "teste@fornecedor.com");
            pstmt.setBoolean(6, true);
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Fornecedor fornecedor = new Fornecedor();
                    fornecedor.setId(rs.getInt(1));
                    return fornecedor;
                }
            }
        }
        throw new SQLException("Falha ao criar fornecedor de teste");
    }
    
    private static Usuario criarUsuario(Connection conn) throws SQLException {
        String sql = "INSERT INTO usuarios (nome, login, senha, perfil, ativo) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Usuario Teste");
            pstmt.setString(2, "teste");
            pstmt.setString(3, "123456");
            pstmt.setString(4, "ADMIN");
            pstmt.setBoolean(5, true);
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario();
                    usuario.setId(rs.getInt(1));
                    return usuario;
                }
            }
        }
        throw new SQLException("Falha ao criar usuário de teste");
    }
    
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("=== Iniciando Testes do ItemCompraDAO ===\n");
            
            // Limpa os dados antes dos testes
            limparDados(conn);
            
            ItemCompraDAO itemCompraDAO = new ItemCompraDAO(conn);
            CompraDAO compraDAO = new CompraDAO(conn);
            
            // Cria objetos necessários para os testes
            Fornecedor fornecedor = criarFornecedor(conn);
            System.out.println("Fornecedor criado com ID: " + fornecedor.getId());
            
            Usuario usuario = criarUsuario(conn);
            System.out.println("Usuário criado com ID: " + usuario.getId());
            
            Produto produto = new Produto();
            produto.setId(1); // Assumindo que existe um produto com ID 1
            
            // Teste 1: Criar uma compra e adicionar itens
            System.out.println("=== Teste de Inserção ===");
            Compra compra = new Compra(fornecedor, usuario);
            compraDAO.create(compra);
            
            ItemCompra novoItem = new ItemCompra(
                produto,               // produto
                10,                    // quantidade
                new BigDecimal("100") // preço unitário
            );
            novoItem.setCompra(compra);
            
            itemCompraDAO.create(novoItem);
            System.out.println("Item inserido com sucesso! ID: " + novoItem.getId());
            
            // Teste 2: Buscar item por ID
            System.out.println("\n=== Teste de Busca por ID ===");
            ItemCompra itemEncontrado = itemCompraDAO.read(novoItem.getId());
            if (itemEncontrado != null) {
                System.out.println("Item encontrado: " + itemEncontrado);
            } else {
                System.out.println("Item não encontrado");
            }
            
            // Teste 3: Buscar itens por compra
            System.out.println("\n=== Teste de Busca por Compra ===");
            List<ItemCompra> itensCompra = itemCompraDAO.readAllByCompra(compra);
            System.out.println("Itens da compra:");
            for (ItemCompra item : itensCompra) {
                System.out.println(item);
            }
            
            // Teste 4: Atualizar item
            System.out.println("\n=== Teste de Atualização ===");
            itemEncontrado.setQuantidade(15);
            itemEncontrado.setPrecoUnitario(new BigDecimal("95"));
            itemCompraDAO.update(itemEncontrado);
            System.out.println("Item atualizado");
            
            // Verifica atualização
            ItemCompra itemAtualizado = itemCompraDAO.read(itemEncontrado.getId());
            System.out.println("Dados após atualização: " + itemAtualizado);
            
            // Teste 5: Buscar por produto
            System.out.println("\n=== Teste de Busca por Produto ===");
            List<ItemCompra> itensProduto = itemCompraDAO.buscarPorProduto(produto.getId());
            System.out.println("Itens do produto:");
            for (ItemCompra item : itensProduto) {
                System.out.println(item);
            }
            
            // Teste 6: Calcular total da compra
            System.out.println("\n=== Teste de Cálculo de Total ===");
            BigDecimal totalCompra = itemCompraDAO.calcularTotalCompra(compra.getId());
            System.out.println("Total da compra: " + totalCompra);
            
            // Teste 7: Deletar item
            System.out.println("\n=== Teste de Exclusão ===");
            itemCompraDAO.delete(novoItem.getId());
            System.out.println("Item deletado");
            
            // Verifica exclusão
            ItemCompra itemDeletado = itemCompraDAO.read(novoItem.getId());
            if (itemDeletado == null) {
                System.out.println("OK: Item foi realmente deletado");
            } else {
                System.out.println("ERRO: Item ainda existe no banco");
            }
            
            System.out.println("\n=== Testes Concluídos com Sucesso! ===");
            
        } catch (SQLException e) {
            System.out.println("Erro na execução dos testes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}