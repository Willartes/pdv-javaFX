package br.com.pdv.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe para gerenciar a conexão com o banco de dados usando o padrão Singleton e pool de conexões.
 */
public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());
    
    // Configurações do banco de dados
    private static final String URL = "jdbc:mysql://localhost:3306/pdv_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    // Configurações adicionais do MySQL
    private static final Properties properties = new Properties();
    
    // Pool de conexões
    private static final int MAX_POOL_SIZE = 10;
    private final BlockingQueue<Connection> connectionPool;
    
    static {
        properties.setProperty("connectTimeout", "30000"); // 30 segundos
        properties.setProperty("socketTimeout", "300000"); // 5 minutos
        properties.setProperty("autoReconnect", "true");
        properties.setProperty("failOverReadOnly", "false");
        properties.setProperty("maxReconnects", "3");
        properties.setProperty("user", USER);
        properties.setProperty("password", PASSWORD);
        properties.setProperty("useSSL", "false");
        properties.setProperty("serverTimezone", "UTC");
        properties.setProperty("allowPublicKeyRetrieval", "true");
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LogUtil.error(DatabaseConnection.class, "Driver MySQL não encontrado", e);
            throw new RuntimeException("Driver MySQL não encontrado", e);
        }
    }
    
    /**
     * Construtor privado que inicializa o pool de conexões
     */
    private DatabaseConnection() {
        connectionPool = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        // Inicializar o pool com algumas conexões
        for (int i = 0; i < MAX_POOL_SIZE / 2; i++) {
            try {
                connectionPool.offer(createConnection());
            } catch (SQLException e) {
                LogUtil.error(DatabaseConnection.class, "Erro ao inicializar pool de conexões", e);
            }
        }
    }
    
    /**
     * Retorna a instância única da classe
     * @return Instância de DatabaseConnection
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }
    
    /**
     * Cria uma nova conexão com o banco de dados
     * @return Uma nova conexão
     * @throws SQLException Se ocorrer um erro ao criar a conexão
     */
    private Connection createConnection() throws SQLException {
        try {
            Connection newConnection = DriverManager.getConnection(URL, properties);
            // Configurar timeout para um valor razoável
            try (Statement stmt = newConnection.createStatement()) {
                // 50 segundos é um valor mais razoável para timeout de bloqueio
                stmt.execute("SET innodb_lock_wait_timeout = 50");
                // Transações com nível de isolamento menor para reduzir bloqueios
                stmt.execute("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            }
            LogUtil.info(DatabaseConnection.class, "Nova conexão com banco de dados criada");
            return newConnection;
        } catch (SQLException e) {
            LogUtil.error(DatabaseConnection.class, "Erro ao criar conexão com banco de dados", e);
            throw e;
        }
    }
    
    /**
     * Obtém uma conexão do pool ou cria uma nova se necessário
     * @return Uma conexão com o banco de dados
     * @throws SQLException Se ocorrer um erro ao obter a conexão
     */
    public Connection getConnection() throws SQLException {
        Connection connection = connectionPool.poll();
        
        if (connection == null) {
            // Pool vazio, criar nova conexão
            connection = createConnection();
            LogUtil.info(DatabaseConnection.class, "Criada nova conexão fora do pool");
        } else {
            // Verificar se a conexão do pool ainda é válida
            try {
                if (connection.isClosed() || !connection.isValid(2)) {
                    LogUtil.info(DatabaseConnection.class, "Conexão do pool inválida, criando nova");
                    connection.close();
                    connection = createConnection();
                }
            } catch (SQLException e) {
                // Se houver erro ao validar, criar nova
                try {
                    connection.close();
                } catch (Exception ex) {
                    // Ignorar erros ao fechar conexão inválida
                }
                connection = createConnection();
            }
        }
        
        return connection;
    }
    
    /**
     * Retorna uma conexão para o pool após o uso
     * @param connection A conexão a ser retornada
     */
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed() && connection.isValid(2)) {
                    // Remover autocommit para impedir transações abertas
                    if (connection.getAutoCommit() == false) {
                        connection.setAutoCommit(true);
                    }
                    
                    // Tentar adicionar ao pool, se estiver cheio, fecha a conexão
                    boolean returned = connectionPool.offer(connection);
                    if (!returned) {
                        connection.close();
                        LogUtil.info(DatabaseConnection.class, "Pool cheio, conexão fechada");
                    } else {
                        LogUtil.info(DatabaseConnection.class, "Conexão retornada ao pool");
                    }
                } else {
                    connection.close();
                    LogUtil.info(DatabaseConnection.class, "Conexão inválida fechada");
                }
            } catch (SQLException e) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    // Ignorar erros ao fechar
                }
                LogUtil.warn(DatabaseConnection.class, "Erro ao processar conexão para retorno ao pool: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fecha todas as conexões e limpa o pool
     */
    public void closeAllConnections() {
        synchronized (connectionPool) {
            Connection conn;
            while ((conn = connectionPool.poll()) != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LogUtil.warn(DatabaseConnection.class, "Erro ao fechar conexão: " + e.getMessage());
                }
            }
            LogUtil.info(DatabaseConnection.class, "Todas as conexões foram fechadas");
        }
    }
    
    /**
     * Método utilitário para fechar recursos JDBC
     * @param connection Conexão a ser fechada
     * @param statement Statement a ser fechado
     * @param resultSet ResultSet a ser fechado
     */
    public static void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LogUtil.warn(DatabaseConnection.class, "Erro ao fechar ResultSet: " + e.getMessage());
            }
        }
        
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LogUtil.warn(DatabaseConnection.class, "Erro ao fechar Statement: " + e.getMessage());
            }
        }
        
        if (connection != null) {
            // Em vez de fechar, devolvemos ao pool
            getInstance().releaseConnection(connection);
        }
    }
    
    /**
     * Testa se a conexão com o banco está funcionando
     * @return true se a conexão foi estabelecida com sucesso
     */
    public boolean testConnection() {
        Connection testConn = null;
        try {
            testConn = getConnection();
            boolean isConnected = testConn != null && !testConn.isClosed();
            if (isConnected) {
                LogUtil.info(DatabaseConnection.class, "Teste de conexão realizado com sucesso");
            } else {
                LogUtil.warn(DatabaseConnection.class, "Falha no teste de conexão");
            }
            return isConnected;
        } catch (SQLException e) {
            LogUtil.error(DatabaseConnection.class, "Erro ao testar conexão com banco de dados", e);
            return false;
        } finally {
            if (testConn != null) {
                releaseConnection(testConn);
            }
        }
    }
    
    /**
     * Limpa transações pendentes em uma conexão
     * @param connection A conexão a ser limpa
     * @return true se a operação foi bem-sucedida
     */
    public static boolean cleanPendingTransactions(Connection connection) {
        if (connection == null) {
            return false;
        }
        
        try {
            // Tentar fazer rollback de qualquer transação pendente
            if (!connection.getAutoCommit()) {
                connection.rollback();
                connection.setAutoCommit(true);
                LogUtil.info(DatabaseConnection.class, "Transação pendente revertida");
            }
            return true;
        } catch (SQLException e) {
            LogUtil.error(DatabaseConnection.class, "Erro ao limpar transações pendentes", e);
            return false;
        }
    }
}