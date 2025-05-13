package br.com.pdv.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class TesteConexao {
    // Lista de tabelas do PDV
    private static final List<String> PDV_TABLES = Arrays.asList(
        "usuarios", "produtos", "clientes", "fornecedores", 
        "compras", "pedidos", "itens_compra", "itens_pedido",
        "contas_financeiras", "fluxo_caixa", "movimentos_caixa"
    );
    
    public static void main(String[] args) {
        testarConexao();
    }
    
    public static void testarConexao() {
        try {
            DatabaseConnection dbConnection = DatabaseConnection.getInstance();
            
            if (dbConnection.testConnection()) {
                System.out.println("✅ Conexão com o banco de dados estabelecida com sucesso!");
                
                try (Connection conn = dbConnection.getConnection()) {
                    DatabaseMetaData metaData = conn.getMetaData();
                    
                    System.out.println("\nInformações do Banco de Dados:");
                    System.out.println("Driver: " + metaData.getDriverName());
                    System.out.println("Versão: " + metaData.getDatabaseProductVersion());
                    
                    System.out.println("\nEstrutura das Tabelas PDV:");
                    for (String tableName : PDV_TABLES) {
                        try {
                            System.out.println("\n📋 Tabela: " + tableName);
                            
                            // Verifica se a tabela existe
                            ResultSet tableCheck = conn.createStatement()
                                .executeQuery("SHOW TABLES LIKE '" + tableName + "'");
                            if (!tableCheck.next()) {
                                System.out.println("⚠️ Tabela não encontrada!");
                                continue;
                            }
                            
                            // Mostra as colunas
                            try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
                                System.out.println("  Colunas:");
                                while (columns.next()) {
                                    String columnName = columns.getString("COLUMN_NAME");
                                    String columnType = columns.getString("TYPE_NAME");
                                    int columnSize = columns.getInt("COLUMN_SIZE");
                                    boolean isNullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                                    
                                    System.out.printf("   - %s (%s(%d))%s%n", 
                                        columnName, columnType, columnSize,
                                        isNullable ? " NULL" : " NOT NULL");
                                }
                            }
                            
                            // Mostra as chaves primárias
                            try (ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName)) {
                                if (primaryKeys.next()) {
                                    System.out.println("  Chave Primária:");
                                    System.out.println("   - " + primaryKeys.getString("COLUMN_NAME"));
                                }
                            }
                            
                            // Mostra as chaves estrangeiras
                            try (ResultSet foreignKeys = metaData.getImportedKeys(null, null, tableName)) {
                                if (foreignKeys.next()) {
                                    System.out.println("  Chaves Estrangeiras:");
                                    do {
                                        String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                                        String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                                        String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                                        System.out.printf("   - %s -> %s(%s)%n", 
                                            fkColumnName, pkTableName, pkColumnName);
                                    } while (foreignKeys.next());
                                }
                            }
                            
                            // Conta registros
                            try (ResultSet count = conn.createStatement()
                                    .executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                                if (count.next()) {
                                    System.out.println("  Total de registros: " + count.getInt(1));
                                }
                            }
                            
                        } catch (SQLException e) {
                            System.out.println("⚠️ Erro ao analisar tabela " + tableName + ": " + e.getMessage());
                        }
                    }
                }
            } else {
                System.err.println("❌ Não foi possível estabelecer conexão com o banco de dados!");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erro ao testar conexão: " + e.getMessage());
            e.printStackTrace();
        }
    }
}