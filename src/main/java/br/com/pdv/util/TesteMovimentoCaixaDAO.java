package br.com.pdv.util;

import br.com.pdv.dao.CaixaDAO;
import br.com.pdv.dao.MovimentoCaixaDAO;
import br.com.pdv.dao.UsuarioDAO;
import br.com.pdv.model.Caixa;
import br.com.pdv.model.MovimentoCaixa;
import br.com.pdv.model.Usuario;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe para testar as operações do MovimentoCaixaDAO.
 */
public class TesteMovimentoCaixaDAO {

    private static final Logger logger = Logger.getLogger(TesteMovimentoCaixaDAO.class.getName());
    
    private static final String URL = "jdbc:mysql://localhost:3306/pdv_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root";
    
    private static Connection connection;
    private static MovimentoCaixaDAO movimentoCaixaDAO;
    private static CaixaDAO caixaDAO;
    private static UsuarioDAO usuarioDAO;
    
    // Dados para teste
    private static Usuario usuario;
    private static Caixa caixa;
    private static MovimentoCaixa movimentoTeste;
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Iniciando Testes do MovimentoCaixaDAO ===\n");
            
            inicializarConexao();
            inicializarDAOs();
            limparBancoDados();
            criarDadosTeste();
            
            executarTestes();
            
            System.out.println("\n=== Testes Concluídos com Sucesso! ===");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro na execução dos testes: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            fecharConexao();
        }
    }
    
    private static void inicializarConexao() throws SQLException {
        try {
            // Obter conexão
            connection = getConnection();
            logger.info("Conexão estabelecida com sucesso.");
        } catch (SQLException e) {
            throw new SQLException("Erro ao estabelecer conexão: " + e.getMessage(), e);
        }
    }
    
    private static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado", e);
        }
    }
    
    private static void inicializarDAOs() throws SQLException {
        try {
            // Inicializa os DAOs necessários
            caixaDAO = CaixaDAO.getInstance();
            usuarioDAO = UsuarioDAO.getInstance();
            movimentoCaixaDAO = MovimentoCaixaDAO.getInstance();
            
            logger.info("Todos os DAOs inicializados com sucesso.");
        } catch (Exception e) {
            throw new SQLException("Falha ao inicializar DAOs: " + e.getMessage(), e);
        }
    }
    
    private static void limparBancoDados() throws SQLException {
        try {
            // Desabilitar verificação de chaves estrangeiras temporariamente
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            
            // Limpar a tabela de movimentos_caixa primeiro
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM movimentos_caixa");
                System.out.println("Registros da tabela movimentos_caixa removidos");
            }
            
            // Limpar a tabela de fluxo_caixa
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM fluxo_caixa");
                System.out.println("Registros da tabela fluxo_caixa removidos");
            }
            
            // Reabilitar verificação de chaves estrangeiras
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            
            System.out.println("Banco de dados limpo para iniciar os testes");
        } catch (SQLException e) {
            System.err.println("Erro ao limpar o banco de dados: " + e.getMessage());
            throw e;
        }
    }
    
    private static void criarDadosTeste() throws SQLException {
        logger.info("\n=== Criando dados para teste ===");
        
        // Buscar um usuário existente ou criar um novo para o teste
        try {
            usuario = usuarioDAO.findById(1);
            if (usuario == null) {
                usuario = new Usuario("Usuario Teste", "usuarioteste", "senha123", "CAIXA");
                usuario = usuarioDAO.create(usuario);
            }
            logger.info("Usuário para teste obtido/criado. ID: " + usuario.getId());
        } catch (SQLException e) {
            throw new SQLException("Falha ao obter/criar usuário para teste: " + e.getMessage(), e);
        }
        
        // Criar um caixa para o teste
        try {
            caixa = new Caixa(usuario, new BigDecimal("1000.00"));
            caixa = caixaDAO.create(caixa);
            logger.info("Caixa para teste criado. ID: " + caixa.getId());
        } catch (SQLException e) {
            throw new SQLException("Falha ao criar caixa para teste: " + e.getMessage(), e);
        }
        
        logger.info("Dados de teste criados com sucesso!");
    }
    
    private static void executarTestes() throws SQLException {
        testarCriacaoMovimento();
        testarBuscaMovimentoPorId();
        testarListagemMovimentosPorCaixa();
        testarListagemTodosMovimentos();
        testarAtualizacaoMovimento();
        testarExclusaoMovimento();
    }
    
    private static void testarCriacaoMovimento() throws SQLException {
        logger.info("\n=== Teste: Criação de Movimento de Caixa ===");
        
        try {
            // Criar movimento de entrada
            MovimentoCaixa novoMovimento = new MovimentoCaixa(
                caixa,
                "ENTRADA",
                new BigDecimal("150.00"),
                "Teste de entrada de caixa"
            );
            
            // Garantir que o usuário está definido
            if (novoMovimento.getUsuario() == null) {
                novoMovimento.setUsuario(usuario);
            }
            
            movimentoTeste = movimentoCaixaDAO.create(novoMovimento);
            
            if (movimentoTeste != null && movimentoTeste.getId() != null) {
                logger.info("Movimento de caixa criado com sucesso! ID: " + movimentoTeste.getId());
                imprimirDetalhesMovimento(movimentoTeste);
                logger.info("Teste de criação de movimento concluído com sucesso!");
            } else {
                throw new SQLException("Falha ao criar movimento: ID não foi atribuído.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao criar movimento", e);
        }
    }
    
    private static void testarBuscaMovimentoPorId() throws SQLException {
        logger.info("\n=== Teste: Busca de Movimento por ID ===");
        
        try {
            MovimentoCaixa movimentoEncontrado = movimentoCaixaDAO.findById(movimentoTeste.getId());
            
            if (movimentoEncontrado != null) {
                logger.info("Movimento encontrado por ID:");
                imprimirDetalhesMovimento(movimentoEncontrado);
                logger.info("Teste de busca por ID concluído com sucesso!");
            } else {
                throw new SQLException("Movimento não encontrado com ID: " + movimentoTeste.getId());
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar movimento por ID", e);
        }
    }
    
    private static void testarListagemMovimentosPorCaixa() throws SQLException {
        logger.info("\n=== Teste: Listagem de Movimentos por Caixa ===");
        
        try {
            List<MovimentoCaixa> movimentos = movimentoCaixaDAO.findByCaixa(caixa);
            
            logger.info("Total de movimentos encontrados para o caixa ID " + caixa.getId() + ": " + movimentos.size());
            
            if (!movimentos.isEmpty()) {
                logger.info("Primeiro movimento encontrado:");
                imprimirDetalhesMovimento(movimentos.get(0));
            }
            
            logger.info("Teste de listagem de movimentos por caixa concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao listar movimentos por caixa", e);
        }
    }
    
    private static void testarListagemTodosMovimentos() throws SQLException {
        logger.info("\n=== Teste: Listagem de Todos os Movimentos ===");
        
        try {
            List<MovimentoCaixa> movimentos = movimentoCaixaDAO.findAll();
            
            logger.info("Total de movimentos encontrados: " + movimentos.size());
            
            if (!movimentos.isEmpty()) {
                logger.info("Listando até 3 movimentos:");
                int count = 0;
                for (MovimentoCaixa movimento : movimentos) {
                    imprimirDetalhesMovimento(movimento);
                    count++;
                    if (count >= 3) break;
                }
            }
            
            logger.info("Teste de listagem de todos os movimentos concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao listar todos os movimentos", e);
        }
    }
    
    private static void testarAtualizacaoMovimento() throws SQLException {
        logger.info("\n=== Teste: Atualização de Movimento ===");
        
        try {
            // Alterar dados do movimento
            BigDecimal novoValor = new BigDecimal("200.00");
            String novaDescricao = "Descrição atualizada - " + UUID.randomUUID().toString().substring(0, 8);
            
            movimentoTeste.setValor(novoValor);
            movimentoTeste.setDescricao(novaDescricao);
            
            boolean atualizado = movimentoCaixaDAO.update(movimentoTeste);
            
            if (atualizado) {
                logger.info("Movimento atualizado com sucesso!");
                
                // Verificar se os dados foram atualizados
                MovimentoCaixa movimentoAtualizado = movimentoCaixaDAO.findById(movimentoTeste.getId());
                
                if (movimentoAtualizado != null && 
                    movimentoAtualizado.getValor().equals(novoValor) && 
                    movimentoAtualizado.getDescricao().equals(novaDescricao)) {
                    
                    logger.info("Dados atualizados corretamente:");
                    imprimirDetalhesMovimento(movimentoAtualizado);
                    logger.info("Teste de atualização concluído com sucesso!");
                } else {
                    throw new SQLException("Os dados não foram atualizados corretamente.");
                }
            } else {
                throw new SQLException("Falha ao atualizar movimento.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao atualizar movimento", e);
        }
    }
    
    private static void testarExclusaoMovimento() throws SQLException {
        logger.info("\n=== Teste: Exclusão de Movimento ===");
        
        try {
            boolean excluido = movimentoCaixaDAO.delete(movimentoTeste.getId());
            
            if (excluido) {
                logger.info("Movimento excluído com sucesso!");
                
                // Verificar se o movimento foi realmente excluído
                MovimentoCaixa movimentoExcluido = movimentoCaixaDAO.findById(movimentoTeste.getId());
                
                if (movimentoExcluido == null) {
                    logger.info("Verificação confirmada: movimento não existe mais no banco de dados.");
                    logger.info("Teste de exclusão concluído com sucesso!");
                } else {
                    throw new SQLException("O movimento ainda existe no banco após a exclusão.");
                }
            } else {
                throw new SQLException("Falha ao excluir movimento.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao excluir movimento", e);
        }
    }
    
    private static void imprimirDetalhesMovimento(MovimentoCaixa movimento) {
        logger.info("------------------------------------------------------");
        logger.info("ID: " + movimento.getId());
        logger.info("Caixa ID: " + (movimento.getCaixa() != null ? movimento.getCaixa().getId() : "N/A"));
        logger.info("Tipo: " + movimento.getTipo());
        logger.info("Valor: " + movimento.getValor());
        logger.info("Descrição: " + movimento.getDescricao());
        logger.info("Data/Hora: " + formatarData(movimento.getDataHora()));
        logger.info("------------------------------------------------------");
    }
    
    private static String formatarData(LocalDateTime data) {
        return data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
    }
    
    private static void fecharConexao() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Conexão com o banco de dados fechada com sucesso.");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Erro ao fechar conexão: " + e.getMessage(), e);
            }
        }
    }
}