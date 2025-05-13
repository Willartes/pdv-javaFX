package br.com.pdv.util;

import br.com.pdv.dao.IProdutoDAO;
import br.com.pdv.dao.ProdutoDAO;
import br.com.pdv.model.Produto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Classe de teste para operações CRUD de produtos utilizando o padrão DAO.
 */
public class TesteProdutoDAO {

    private static final IProdutoDAO produtoDAO = ProdutoDAO.getInstance(); // Injeção de dependência
    private static Produto produtoTeste;

    public static void main(String[] args) {
        try {
            inicializarTeste();
            executarTestes();
            System.out.println("\nTodos os testes foram concluídos com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro durante a execução dos testes: " + e.getMessage());
            e.printStackTrace();
        } finally {
            fecharConexao();
        }
    }

    /**
     * Inicializa o ambiente de teste, obtendo uma instância do DAO.
     *
     * @throws SQLException Se houver um erro ao obter a instância do DAO.
     */
    private static void inicializarTeste() throws SQLException {
        System.out.println("Inicializando os testes do ProdutoDAO...\n");
    }

    /**
     * Executa todos os métodos de teste para operações CRUD de produtos.
     *
     * @throws SQLException Se houver um erro em alguma das operações de teste.
     */
    private static void executarTestes() throws SQLException {
        try {
            testarCriacaoProduto();
            testarBuscaPorId();
            testarBuscaPorCodigo();
            testarBuscaPorNome();
            testarAtualizacaoProduto();
            testarAtualizacaoEstoque();
            testarVerificacaoEstoqueBaixo();
            testarListagemProdutos();
            testarContagemRegistros();
            testarExclusaoProduto();
        } catch (Exception e) {
            System.err.println("Erro durante a execução dos testes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Garante que a conexão com o banco de dados seja fechada corretamente.
     */
    private static void fecharConexao() {
        try {
            DatabaseConnection.getInstance().closeAllConnections();
            System.out.println("Conexão com o banco de dados fechada com sucesso.");
        } catch (Exception e) {
            System.err.println("Erro ao fechar a conexão com o banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Testa a criação de um novo produto no banco de dados.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarCriacaoProduto() throws SQLException {
        System.out.println("Iniciando o teste de criação de produto...");
        String codigo = gerarCodigoProduto();
        Produto novoProduto = criarProdutoTeste(codigo);

        try {
            produtoTeste = produtoDAO.create(novoProduto);
            if (produtoTeste != null && produtoTeste.getId() != null) {
                System.out.println("Produto criado com sucesso! ID: " + produtoTeste.getId());
            } else {
                System.err.println("Falha ao criar o produto.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao criar o produto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a busca de um produto por ID.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarBuscaPorId() throws SQLException {
        System.out.println("\nIniciando o teste de busca por ID...");
        try {
            Optional<Produto> produtoEncontrado = Optional.ofNullable(produtoDAO.findById(produtoTeste.getId()));
            if (produtoEncontrado.isPresent()) {
                System.out.println("Produto encontrado por ID com sucesso: " + produtoEncontrado.get());
            } else {
                System.err.println("Produto não encontrado para o ID: " + produtoTeste.getId());
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar o produto por ID: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a busca de um produto por código.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarBuscaPorCodigo() throws SQLException {
        System.out.println("\nIniciando o teste de busca por código...");
        try {
            Optional<Produto> produtoEncontrado = Optional.ofNullable(produtoDAO.findByCodigo(produtoTeste.getCodigo()));
            if (produtoEncontrado.isPresent()) {
                System.out.println("Produto encontrado por código com sucesso: " + produtoEncontrado.get());
            } else {
                System.err.println("Produto não encontrado para o código: " + produtoTeste.getCodigo());
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar o produto por código: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a busca de produtos por nome.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarBuscaPorNome() throws SQLException {
        System.out.println("\nIniciando o teste de busca por nome...");
        try {
            List<Produto> produtosEncontrados = produtoDAO.findByNome("Produto Teste");
            if (produtosEncontrados != null && !produtosEncontrados.isEmpty()) {
                System.out.println("Produtos encontrados por nome com sucesso. Total: " + produtosEncontrados.size());
            } else {
                System.err.println("Nenhum produto encontrado com o nome: Produto Teste");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar produtos por nome: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a atualização de um produto existente.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarAtualizacaoProduto() throws SQLException {
        System.out.println("\nIniciando o teste de atualização de produto...");
        try {
            String novoNome = "Produto Atualizado " + LocalDateTime.now().getNano();
            produtoTeste.setNome(novoNome);
            boolean atualizado = produtoDAO.update(produtoTeste);
            if (atualizado) {
                System.out.println("Produto atualizado com sucesso para o nome: " + novoNome);
            } else {
                System.err.println("Falha ao atualizar o produto.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar o produto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a atualização do estoque de um produto.
     * 
     * Este método foi refatorado para evitar problemas com gerenciamento de conexão.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarAtualizacaoEstoque() throws SQLException {
        System.out.println("\nIniciando o teste de atualização de estoque...");
        
        // Primeiro, testamos a entrada de estoque
        try {
            // Determinar o estoque inicial
            int estoqueInicial = produtoTeste.getEstoqueAtual();
            int quantidadeEntrada = 5;
            
            System.out.println("Entrada de " + quantidadeEntrada + " unidades de " + produtoTeste.getNome());
            
            // Atualizar o estoque no objeto
            produtoTeste.setEstoqueAtual(estoqueInicial + quantidadeEntrada);
            
            // Persistir a alteração
            boolean sucessoEntrada = produtoDAO.update(produtoTeste);
            
            if (sucessoEntrada) {
                System.out.println("Estoque de " + produtoTeste.getNome() + 
                                 " atualizado com sucesso. Novo estoque: " + produtoTeste.getEstoqueAtual());
            } else {
                throw new SQLException("Falha ao atualizar o estoque para entrada.");
            }
            
            // Agora testamos a saída de estoque
            int estoqueAtual = produtoTeste.getEstoqueAtual();
            int quantidadeSaida = 2;
            
            // Verificar se há estoque suficiente
            if (estoqueAtual < quantidadeSaida) {
                throw new SQLException("Estoque insuficiente para a saída de " + quantidadeSaida + " unidades.");
            }
            
            System.out.println("Saída de " + quantidadeSaida + " unidades de " + produtoTeste.getNome());
            
            // Atualizar o estoque no objeto
            produtoTeste.setEstoqueAtual(estoqueAtual - quantidadeSaida);
            
            // Persistir a alteração
            boolean sucessoSaida = produtoDAO.update(produtoTeste);
            
            if (sucessoSaida) {
                System.out.println("Estoque de " + produtoTeste.getNome() + 
                                 " atualizado com sucesso. Novo estoque: " + produtoTeste.getEstoqueAtual());
            } else {
                throw new SQLException("Falha ao atualizar o estoque para saída.");
            }
            
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar o estoque: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a verificação de produtos com estoque baixo.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarVerificacaoEstoqueBaixo() throws SQLException {
        System.out.println("\nIniciando o teste de verificação de estoque baixo...");
        try {
            // Garantir que o produto de teste tenha estoque abaixo do mínimo
            if (produtoTeste != null) {
                // Certificar-se de que o produto tem um nome válido
                if (produtoTeste.getNome() == null || produtoTeste.getNome().trim().isEmpty()) {
                    produtoTeste.setNome("Produto Teste");
                }
                
                // Configurar estoque mínimo maior que o estoque atual para teste
                int estoqueAtual = produtoTeste.getEstoqueAtual();
                produtoTeste.setEstoqueMinimo(estoqueAtual + 5);
                produtoDAO.update(produtoTeste);
            }
            
            List<Produto> produtosEstoqueBaixo = produtoDAO.findEstoqueBaixo();
            if (produtosEstoqueBaixo != null && !produtosEstoqueBaixo.isEmpty()) {
                System.out.println("Produtos com estoque baixo encontrados: " + produtosEstoqueBaixo.size());
            } else {
                System.out.println("Nenhum produto com estoque baixo encontrado.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar produtos com estoque baixo: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("Erro de argumento ilegal: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a listagem de todos os produtos.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarListagemProdutos() throws SQLException {
        System.out.println("\nIniciando o teste de listagem de produtos...");
        try {
            List<Produto> produtos = produtoDAO.findAll();
            if (produtos != null && !produtos.isEmpty()) {
                System.out.println("Lista de produtos retornada com sucesso. Total: " + produtos.size());
            } else {
                System.err.println("A lista de produtos está vazia.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a contagem de registros de produtos.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarContagemRegistros() throws SQLException {
        System.out.println("\nIniciando o teste de contagem de registros...");
        try {
            long count = produtoDAO.count();
            System.out.println("Total de produtos cadastrados: " + count);
        } catch (SQLException e) {
            System.err.println("Erro ao contar registros: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Testa a exclusão de um produto.
     *
     * @throws SQLException Se houver um erro na operação de banco de dados.
     */
    private static void testarExclusaoProduto() throws SQLException {
        System.out.println("\nIniciando o teste de exclusão de produto...");
        try {
            if (produtoTeste != null) {
                boolean excluido = produtoDAO.delete(produtoTeste.getId());
                if (excluido) {
                    System.out.println("Produto excluído com sucesso!");
                } else {
                    System.err.println("Falha ao excluir o produto.");
                }
            } else {
                System.err.println("Nenhum produto para excluir.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao excluir produto: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Métodos auxiliares
    private static String gerarCodigoProduto() {
        Random random = new Random();
        return "TESTE" + random.nextInt(1000);
    }

    private static Produto criarProdutoTeste(String codigo) {
        Produto produto = new Produto(codigo, "Produto Teste", "Descrição do Produto Teste", BigDecimal.valueOf(99.90), 0, true);
        produto.setEstoqueMinimo(10);
        produto.setUnidade("UN");
        return produto;
    }
}