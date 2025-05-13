package br.com.pdv.util;

import br.com.pdv.dao.*;
import br.com.pdv.model.*;
import br.com.pdv.exception.CpfCnpjDuplicadoException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe para testar as operações do VendaDAO.
 */
public class TesteVendaDAO {

    private static final Logger logger = Logger.getLogger(TesteVendaDAO.class.getName());
    private static Connection connection;
    private static VendaDAO vendaDAO;
    private static PedidoDAO pedidoDAO;
    private static ClienteDAO clienteDAO;
    private static UsuarioDAO usuarioDAO;
    private static ProdutoDAO produtoDAO;
    private static ItemPedidoDAO itemPedidoDAO;
    private static ParcelaDAO parcelaDAO;

    // Dados para teste
    private static Cliente cliente;
    private static Usuario usuario;
    private static Produto produto1;
    private static Produto produto2;
    private static Pedido pedidoTeste;
    private static Venda vendaTeste;

    public static void main(String[] args) {
        try {
            inicializarConexao();
            inicializarDAOs();
            criarDadosTeste();

            executarTestes();

            System.out.println("\n=== Testes concluídos com sucesso! ===");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "\nErro ao executar testes: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            fecharConexao();
        }
    }

    private static void inicializarConexao() throws SQLException {
        try {
            // Obtém uma conexão do singleton DatabaseConnection
            connection = DatabaseConnection.getInstance().getConnection();
            logger.info("Conexão estabelecida com sucesso.");
        } catch (SQLException e) {
            throw new SQLException("Erro ao estabelecer conexão: " + e.getMessage(), e);
        }
    }

    private static void inicializarDAOs() throws SQLException {
        try {
            // Inicializa todos os DAOs usando o padrão singleton
            clienteDAO = ClienteDAO.getInstance();
            usuarioDAO = UsuarioDAO.getInstance();
            produtoDAO = ProdutoDAO.getInstance();
            itemPedidoDAO = ItemPedidoDAO.getInstance();
            pedidoDAO = PedidoDAO.getInstance();
            vendaDAO = VendaDAO.getInstance();
            parcelaDAO = ParcelaDAO.getInstance();
            
            logger.info("Todos os DAOs inicializados com sucesso.");
        } catch (Exception e) {
            throw new SQLException("Falha ao inicializar DAOs: " + e.getMessage(), e);
        }
    }

    private static void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) {
                DatabaseConnection.getInstance().closeConnection();
                logger.info("Conexão com o banco de dados fechada com sucesso.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao fechar conexão: " + e.getMessage(), e);
        }
    }

    private static void criarDadosTeste() throws SQLException, CpfCnpjDuplicadoException {
        logger.info("\n=== Criando dados para teste ===");

        // Criar um cliente para teste
        String cpfUnico = UUID.randomUUID().toString().substring(0, 8);
        cliente = new Cliente(
            "Cliente Teste Venda",
            cpfUnico,
            "Rua da Venda, 456",
            "(11) 98765-4321",
            "venda@email.com"
        );
        
        // Definir a data de cadastro como a data atual
        cliente.setDataCadastro(new Date());
        
        try {
            cliente = clienteDAO.create(cliente);
            logger.info("Cliente criado com sucesso. ID: " + cliente.getId());
        } catch (CpfCnpjDuplicadoException e) {
            logger.warning("Erro ao criar cliente devido a CPF/CNPJ duplicado. Buscando cliente existente...");
            cliente = clienteDAO.findByCpfCnpj(cpfUnico);
            if (cliente == null) {
                throw new SQLException("Não foi possível encontrar ou criar cliente para teste.");
            }
            logger.info("Cliente existente localizado. ID: " + cliente.getId());
        }

        // Criar um usuário para teste
        String loginUnico = "vendateste" + UUID.randomUUID().toString().substring(0, 4);
        usuario = new Usuario(
            "Usuario Teste Venda", 
            loginUnico, 
            "senha123", 
            "VENDEDOR"
        );
        
        try {
            usuario = usuarioDAO.create(usuario);
            logger.info("Usuário criado com sucesso. ID: " + usuario.getId());
        } catch (SQLException e) {
            logger.warning("Erro ao criar usuário. Usando usuário existente.");
            List<Usuario> usuarios = usuarioDAO.findAll();
            if (usuarios != null && !usuarios.isEmpty()) {
                usuario = usuarios.get(0);
                logger.info("Usuário existente localizado. ID: " + usuario.getId());
            } else {
                throw new SQLException("Não foi possível encontrar ou criar usuário para teste.");
            }
        }

        // Criar produtos para teste com códigos únicos
        String codigoProduto1 = "VENDA-TESTE-1-" + UUID.randomUUID().toString().substring(0, 4);
        String codigoProduto2 = "VENDA-TESTE-2-" + UUID.randomUUID().toString().substring(0, 4);

        produto1 = new Produto(
            codigoProduto1,
            "Produto Venda Teste 1",
            "Descrição do Produto Venda Teste 1",
            new BigDecimal("100.00"),
            20,
            true
        );
        produto2 = new Produto(
            codigoProduto2,
            "Produto Venda Teste 2",
            "Descrição do Produto Venda Teste 2",
            new BigDecimal("150.50"),
            15,
            true
        );
        
        try {
            produto1 = produtoDAO.create(produto1);
            logger.info("Produto 1 criado com sucesso. ID: " + produto1.getId());
        } catch (SQLException e) {
            logger.warning("Erro ao criar produto 1. Buscando produto existente...");
            List<Produto> produtos = produtoDAO.findAll();
            if (produtos != null && !produtos.isEmpty()) {
                produto1 = produtos.get(0);
                logger.info("Produto 1 existente localizado. ID: " + produto1.getId());
            } else {
                throw new SQLException("Não foi possível encontrar ou criar produto 1 para teste.");
            }
        }
        
        try {
            produto2 = produtoDAO.create(produto2);
            logger.info("Produto 2 criado com sucesso. ID: " + produto2.getId());
        } catch (SQLException e) {
            logger.warning("Erro ao criar produto 2. Buscando produto existente...");
            List<Produto> produtos = produtoDAO.findAll();
            if (produtos != null && !produtos.isEmpty() && produtos.size() > 1) {
                produto2 = produtos.get(1);
                logger.info("Produto 2 existente localizado. ID: " + produto2.getId());
            } else if (produtos != null && !produtos.isEmpty()) {
                produto2 = produtos.get(0);
                logger.info("Produto 2 existente localizado. ID: " + produto2.getId());
            } else {
                throw new SQLException("Não foi possível encontrar ou criar produto 2 para teste.");
            }
        }
        
        // Criar pedido para teste
        criarPedidoTeste();

        logger.info("Dados de teste criados com sucesso!");
    }

    private static void criarPedidoTeste() throws SQLException {
        pedidoTeste = new Pedido();
        pedidoTeste.setCliente(cliente);
        pedidoTeste.setUsuario(usuario);
        pedidoTeste.setVendedor(usuario);
        pedidoTeste.setDataPedido(LocalDateTime.now());
        pedidoTeste.setStatus("ABERTO");
        
        // Calcular valor total inicial como zero (será atualizado após adicionar itens)
        pedidoTeste.setValorTotal(BigDecimal.ZERO);
        
        try {
            pedidoTeste = pedidoDAO.create(pedidoTeste);
            logger.info("Pedido criado com sucesso. ID: " + pedidoTeste.getId());
            
            // Adicionar itens ao pedido
            BigDecimal valorTotal = BigDecimal.ZERO;
            
            // Item 1
            ItemPedido item1 = new ItemPedido(produto1, 2);
            item1.setPedido(pedidoTeste);
            item1 = itemPedidoDAO.create(item1);
            valorTotal = valorTotal.add(item1.getValorTotal());
            
            // Item 2
            ItemPedido item2 = new ItemPedido(produto2, 1);
            item2.setPedido(pedidoTeste);
            item2 = itemPedidoDAO.create(item2);
            valorTotal = valorTotal.add(item2.getValorTotal());
            
            // Atualizar valor total do pedido
            pedidoTeste.setValorTotal(valorTotal);
            pedidoDAO.update(pedidoTeste);
            
            // Atualizar pedido para incluir itens na lista
            List<ItemPedido> itens = new ArrayList<>();
            itens.add(item1);
            itens.add(item2);
            pedidoTeste.setItens(itens);
            
            logger.info("Itens adicionados ao pedido com sucesso. Valor total: " + valorTotal);
        } catch (SQLException e) {
            throw new SQLException("Erro ao criar pedido de teste: " + e.getMessage(), e);
        }
    }

    private static void executarTestes() throws SQLException {
        testarCriacaoVenda();
        testarBuscaVendaPorId();
        testarListagemVendas();
        testarVendaParcelada();
        testarCalculoTotalVendasPorPeriodo();
        testarBuscaVendaPorCliente();
        testarBuscaVendaPorUsuario();
        testarCancelamentoVenda();
    }

    private static void testarCriacaoVenda() throws SQLException {
        logger.info("\n=== Teste: Criação de Venda ===");
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLES");
            logger.info("Tabelas disponíveis:");
            while (rs.next()) {
                logger.info(rs.getString(1));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erro ao listar tabelas: " + e.getMessage(), e);
        }
        try {
        	vendaTeste = new Venda(pedidoTeste);
        	vendaTeste.setCliente(cliente);
        	vendaTeste.setUsuario(usuario);
        	vendaTeste.setDataVenda(LocalDateTime.now()); // Certifique-se de definir a data
        	vendaTeste.setNumeroNF("NF" + System.currentTimeMillis());
        	vendaTeste.setFormaPagamento("DINHEIRO");
        	vendaTeste.setValorDesconto(BigDecimal.ZERO);
        	vendaTeste.setValorPago(vendaTeste.getValorTotal());
        	vendaTeste.setStatus("FINALIZADA"); // Certifique-se de definir o status
            
        	vendaTeste = vendaDAO.create(vendaTeste);
            
            if (vendaTeste.getId() != null) {
                logger.info("Venda criada com sucesso. ID: " + vendaTeste.getId());
                logger.info("Teste de criação de venda concluído com sucesso!");
            } else {
                throw new SQLException("Falha ao criar venda: ID não foi atribuído.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao criar venda", e);
        }
    }

    private static void testarBuscaVendaPorId() throws SQLException {
        logger.info("\n=== Teste: Busca de Venda por ID ===");

        try {
            Venda vendaEncontrada = vendaDAO.findById(vendaTeste.getId());

            if (vendaEncontrada != null) {
                imprimirDetalhesVenda(vendaEncontrada);
                logger.info("Teste de busca por ID concluído com sucesso!");
            } else {
                throw new SQLException("Venda não encontrada");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar venda por ID", e);
        }
    }

    private static void testarListagemVendas() throws SQLException {
        logger.info("\n=== Teste: Listagem de Vendas ===");

        try {
            List<Venda> vendas = vendaDAO.findAll();
            logger.info("Total de vendas encontradas: " + vendas.size());
            
            if (!vendas.isEmpty()) {
                logger.info("Primeira venda da lista: ");
                imprimirDetalhesVenda(vendas.get(0));
            }
            
            logger.info("Teste de listagem de vendas concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao listar vendas", e);
        }
    }

    private static void testarVendaParcelada() throws SQLException {
        logger.info("\n=== Teste: Venda Parcelada ===");

        try {
            // Criar um novo pedido para a venda parcelada
            Pedido pedidoParcelado = new Pedido();
            pedidoParcelado.setCliente(cliente);
            pedidoParcelado.setUsuario(usuario);
            pedidoParcelado.setVendedor(usuario);
            pedidoParcelado.setDataPedido(LocalDateTime.now());
            pedidoParcelado.setStatus("ABERTO");
            pedidoParcelado.setValorTotal(new BigDecimal("1000.00"));
            
            pedidoParcelado = pedidoDAO.create(pedidoParcelado);
            logger.info("Pedido para venda parcelada criado com sucesso. ID: " + pedidoParcelado.getId());
            
            // Criar a venda parcelada
            Venda vendaParcelada = new Venda(pedidoParcelado);
            vendaParcelada.setCliente(cliente);
            vendaParcelada.setUsuario(usuario);
            vendaParcelada.setNumeroNF("NF-PARC-" + System.currentTimeMillis());
            vendaParcelada.setFormaPagamento("CARTAO_CREDITO");
            vendaParcelada.setValorDesconto(BigDecimal.ZERO);
            vendaParcelada.setValorPago(vendaParcelada.getValorTotal());
            
            // Criar parcelas
            int numeroParcelas = 3;
            BigDecimal valorParcela = vendaParcelada.getValorTotal().divide(new BigDecimal(numeroParcelas), 2, BigDecimal.ROUND_HALF_UP);
            LocalDate dataVencimento = LocalDate.now();
            
            for (int i = 1; i <= numeroParcelas; i++) {
                Parcela parcela = new Parcela(i, numeroParcelas, valorParcela, dataVencimento.plusMonths(i));
                parcela.setVenda(vendaParcelada);
                vendaParcelada.adicionarParcela(parcela);
            }
            
            vendaParcelada = vendaDAO.create(vendaParcelada);
            
            if (vendaParcelada.getId() != null) {
                logger.info("Venda parcelada criada com sucesso. ID: " + vendaParcelada.getId());
                
                // Verificar se as parcelas foram criadas
                List<Parcela> parcelas = parcelaDAO.findByVenda(vendaParcelada.getId());
                logger.info("Parcelas criadas: " + parcelas.size());
                
                for (Parcela parcela : parcelas) {
                    logger.info("Parcela " + parcela.getNumeroParcela() + "/" + 
                                parcela.getTotalParcelas() + " - Valor: " + parcela.getValor() + 
                                " - Vencimento: " + parcela.getDataVencimento());
                }
                
                // Registrar pagamento da primeira parcela
                if (!parcelas.isEmpty()) {
                    Parcela primeiraParcela = parcelas.get(0);
                    parcelaDAO.pagar(primeiraParcela.getId(), LocalDate.now());
                    logger.info("Pagamento da primeira parcela registrado com sucesso!");
                    
                    // Verificar se o status foi atualizado
                    Parcela parcelaAtualizada = parcelaDAO.findById(primeiraParcela.getId());
                    logger.info("Status da parcela após pagamento: " + parcelaAtualizada.getStatus());
                }
                
                logger.info("Teste de venda parcelada concluído com sucesso!");
            } else {
                throw new SQLException("Falha ao criar venda parcelada: ID não foi atribuído.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao criar venda parcelada", e);
        }
    }

    private static void testarCalculoTotalVendasPorPeriodo() throws SQLException {
        logger.info("\n=== Teste: Cálculo de Total de Vendas por Período ===");

        try {
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(30);
            LocalDateTime dataFim = LocalDateTime.now().plusDays(1);
            
            BigDecimal totalVendas = vendaDAO.getTotalByPeriod(dataInicio, dataFim);
            
            logger.info("Total de vendas no período de " + 
                      formatarData(dataInicio) + " a " + formatarData(dataFim) + 
                      ": " + totalVendas);
            
            // Listar vendas no período
            List<Venda> vendasPeriodo = vendaDAO.findByPeriod(dataInicio, dataFim);
            logger.info("Número de vendas no período: " + vendasPeriodo.size());
            
            logger.info("Teste de cálculo de total de vendas por período concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao calcular total de vendas por período", e);
        }
    }

    private static void testarBuscaVendaPorCliente() throws SQLException {
        logger.info("\n=== Teste: Busca de Vendas por Cliente ===");

        try {
            List<Venda> vendasCliente = vendaDAO.findByCliente(cliente.getId());
            
            logger.info("Total de vendas do cliente " + cliente.getNome() + ": " + vendasCliente.size());
            
            if (!vendasCliente.isEmpty()) {
                logger.info("Primeira venda do cliente: ");
                imprimirDetalhesVenda(vendasCliente.get(0));
            }
            
            logger.info("Teste de busca de vendas por cliente concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar vendas por cliente", e);
        }
    }

    private static void testarBuscaVendaPorUsuario() throws SQLException {
        logger.info("\n=== Teste: Busca de Vendas por Usuário ===");

        try {
            List<Venda> vendasUsuario = vendaDAO.findByUsuario(usuario.getId());
            
            logger.info("Total de vendas do usuário " + usuario.getNome() + ": " + vendasUsuario.size());
            
            if (!vendasUsuario.isEmpty()) {
                logger.info("Primeira venda do usuário: ");
                imprimirDetalhesVenda(vendasUsuario.get(0));
            }
            
            logger.info("Teste de busca de vendas por usuário concluído com sucesso!");
        } catch (SQLException e) {
            throw new SQLException("Erro ao buscar vendas por usuário", e);
        }
    }

    private static void testarCancelamentoVenda() throws SQLException {
        logger.info("\n=== Teste: Cancelamento de Venda ===");

        try {
            String motivoCancelamento = "Teste de cancelamento de venda";
            
            boolean cancelado = vendaDAO.cancelar(vendaTeste, motivoCancelamento);
            
            if (cancelado) {
                logger.info("Venda cancelada com sucesso!");
                
                // Verificar se o status foi atualizado
                Venda vendaCancelada = vendaDAO.findById(vendaTeste.getId());
                logger.info("Status da venda após cancelamento: " + vendaCancelada.getStatus());
                
                // Verificar se o pedido foi restaurado para ABERTO
                Pedido pedidoRestaurado = pedidoDAO.findById(pedidoTeste.getId());
                logger.info("Status do pedido após cancelamento da venda: " + pedidoRestaurado.getStatus());
                
                logger.info("Teste de cancelamento de venda concluído com sucesso!");
            } else {
                throw new SQLException("Falha ao cancelar venda.");
            }
        } catch (SQLException e) {
            throw new SQLException("Erro ao cancelar venda", e);
        }
    }

    private static void imprimirDetalhesVenda(Venda venda) {
        logger.info("==== Detalhes da Venda ====");
        logger.info("ID: " + venda.getId());
        logger.info("Pedido ID: " + (venda.getPedido() != null ? venda.getPedido().getId() : "N/A"));
        logger.info("Cliente: " + (venda.getCliente() != null ? venda.getCliente().getNome() : "N/A"));
        logger.info("Usuário: " + (venda.getUsuario() != null ? venda.getUsuario().getNome() : "N/A"));
        logger.info("Data da Venda: " + formatarData(venda.getDataVenda()));
        logger.info("Valor Total: " + venda.getValorTotal());
        logger.info("Valor Desconto: " + venda.getValorDesconto());
        logger.info("Valor Pago: " + venda.getValorPago());
        logger.info("Troco: " + venda.getTroco());
        logger.info("Forma de Pagamento: " + venda.getFormaPagamento());
        logger.info("Número NF: " + venda.getNumeroNF());
        logger.info("Status: " + venda.getStatus());
        
        if ("CANCELADA".equals(venda.getStatus())) {
            logger.info("Data de Cancelamento: " + formatarData(venda.getDataCancelamento()));
            logger.info("Motivo do Cancelamento: " + venda.getMotivoCancelamento());
        }
        
        // Listar parcelas se for uma venda parcelada
        if (venda.getParcelas() != null && !venda.getParcelas().isEmpty()) {
            logger.info("Parcelas:");
            for (Parcela parcela : venda.getParcelas()) {
                logger.info("  " + parcela.getNumeroParcela() + "/" + parcela.getTotalParcelas() + 
                          " - Valor: " + parcela.getValor() + 
                          " - Vencimento: " + parcela.getDataVencimento() +
                          " - Status: " + parcela.getStatus());
            }
        }
    }

    private static String formatarData(LocalDateTime data) {
        return data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A";
    }
}