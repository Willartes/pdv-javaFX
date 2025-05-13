package br.com.pdv.relatorio;

import br.com.pdv.dao.VendaDAO;
import br.com.pdv.dao.ClienteDAO;
import br.com.pdv.model.Cliente;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório de Crédito Por Cliente
 * Exibe informações sobre vendas no crediário agrupadas por cliente,
 * incluindo histórico de pagamentos, limites de crédito e análise de inadimplência.
 */
public class RelatorioCreditoPorCliente extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final ClienteDAO clienteDAO;
    
    // Categorias de atraso para análise de inadimplência
    private static final int ATRASO_BAIXO = 15;    // Até 15 dias
    private static final int ATRASO_MEDIO = 30;    // Até 30 dias
    private static final int ATRASO_ALTO = 60;     // Até 60 dias
    private static final int ATRASO_CRITICO = 90;  // Até 90 dias
    
    /**
     * Construtor do relatório de crédito por cliente
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioCreditoPorCliente(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE CREDIÁRIO POR CLIENTE", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
        this.clienteDAO = RelatorioManager.getInstance().getClienteDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            // Verificar se as tabelas necessárias existem
            boolean tabelaParcelas = verificarTabelaExiste(conn, "parcelas_crediario");
            
            // Verificar se a coluna limite_credito existe na tabela clientes
            boolean colunaLimiteCredito = verificarColunaExiste(conn, "clientes", "limite_credito");
            
            // Verificar se a coluna valor_entrada existe na tabela vendas
            boolean colunaValorEntrada = verificarColunaExiste(conn, "vendas", "valor_entrada");
            
            // Parte 1: Resumo de crediário por cliente
            relatorio.append("RESUMO DE CREDIÁRIO POR CLIENTE\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            if (!tabelaParcelas) {
                relatorio.append("ATENÇÃO: A tabela 'parcelas_crediario' não existe no banco de dados.\n");
                relatorio.append("Este relatório utilizará apenas dados básicos da tabela 'vendas'.\n");
                relatorio.append("Para um relatório completo, crie a tabela 'parcelas_crediario'.\n\n");
            }
            
            if (!colunaLimiteCredito) {
                relatorio.append("NOTA: A coluna 'limite_credito' não foi encontrada na tabela 'clientes'.\n");
                relatorio.append("Usando valor padrão de R$ 1.000,00 para todos os clientes.\n\n");
            }
            
            if (!colunaValorEntrada) {
                relatorio.append("NOTA: A coluna 'valor_entrada' não foi encontrada na tabela 'vendas'.\n");
                relatorio.append("Assumindo que não há valores de entrada nas vendas.\n\n");
            }
            
            String sql;
            
            // Construir a consulta SQL adequada com base nas colunas disponíveis
            StringBuilder sqlBuilder = new StringBuilder();
            
            sqlBuilder.append("SELECT c.id as cliente_id, c.nome as cliente_nome, c.cpf_cnpj, ");
            
            if (colunaLimiteCredito) {
                sqlBuilder.append("c.limite_credito, ");
            } else {
                sqlBuilder.append("1000.00 as limite_credito, "); // Valor padrão para limite de crédito
            }
            
            sqlBuilder.append("COUNT(v.id) as total_vendas, ");
            sqlBuilder.append("SUM(v.valor_total) as valor_total, ");
            
            if (colunaValorEntrada) {
                sqlBuilder.append("SUM(v.valor_entrada) as valor_entrada, ");
                sqlBuilder.append("SUM(v.valor_total - v.valor_entrada) as valor_credito, ");
            } else {
                sqlBuilder.append("0 as valor_entrada, ");
                sqlBuilder.append("SUM(v.valor_total) as valor_credito, ");
            }
            
            if (tabelaParcelas) {
                sqlBuilder.append("SUM(CASE WHEN pc.status_pagamento = 'PAGO' THEN pc.valor_parcela ELSE 0 END) as valor_pago, ");
                sqlBuilder.append("SUM(CASE WHEN pc.status_pagamento != 'PAGO' THEN pc.valor_parcela ELSE 0 END) as valor_pendente ");
                sqlBuilder.append("FROM clientes c ");
                sqlBuilder.append("JOIN vendas v ON v.cliente_id = c.id ");
                sqlBuilder.append("LEFT JOIN parcelas_crediario pc ON pc.venda_id = v.id ");
            } else {
                sqlBuilder.append("0 as valor_pago, ");
                sqlBuilder.append("SUM(v.valor_total) as valor_pendente ");
                sqlBuilder.append("FROM clientes c ");
                sqlBuilder.append("JOIN vendas v ON v.cliente_id = c.id ");
            }
            
            sqlBuilder.append("WHERE v.data_venda BETWEEN ? AND ? ");
            sqlBuilder.append("AND v.forma_pagamento = 'CREDIARIO' ");
            sqlBuilder.append("AND v.status = 'FINALIZADA' ");
            
            if (colunaLimiteCredito) {
                sqlBuilder.append("GROUP BY c.id, c.nome, c.cpf_cnpj, c.limite_credito ");
            } else {
                sqlBuilder.append("GROUP BY c.id, c.nome, c.cpf_cnpj ");
            }
            
            sqlBuilder.append("ORDER BY valor_pendente DESC");
            
            sql = sqlBuilder.toString();
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-30s %-15s %-12s %-12s %-15s %-15s %-15s\n",
                                         "CLIENTE", "CPF/CNPJ", "LIMITE", "VENDAS", "CREDIADO", "PAGO", "PENDENTE"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Contadores para totais
            BigDecimal totalLimites = BigDecimal.ZERO;
            BigDecimal totalCredito = BigDecimal.ZERO;
            BigDecimal totalPago = BigDecimal.ZERO;
            BigDecimal totalPendente = BigDecimal.ZERO;
            int totalVendas = 0;
            
            // Lista para armazenar clientes e seus valores pendentes para análise posterior
            List<ClienteCredito> clientesCredito = new ArrayList<>();
            
            while (rs.next()) {
                int clienteId = rs.getInt("cliente_id");
                String clienteNome = rs.getString("cliente_nome");
                String cpfCnpj = rs.getString("cpf_cnpj");
                BigDecimal limiteCredito = rs.getBigDecimal("limite_credito");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorCredito = rs.getBigDecimal("valor_credito");
                BigDecimal valorPago = rs.getBigDecimal("valor_pago");
                BigDecimal valorPendente = rs.getBigDecimal("valor_pendente");
                
                // Formatar CPF/CNPJ para exibição
                String cpfCnpjFormatado = formatarCpfCnpj(cpfCnpj);
                
                relatorio.append(String.format("%-30s %-15s R$%-10.2f %-12d R$%-13.2f R$%-13.2f R$%-13.2f\n",
                                             (clienteNome.length() > 28 ? clienteNome.substring(0, 25) + "..." : clienteNome),
                                             cpfCnpjFormatado,
                                             limiteCredito,
                                             qtdVendas,
                                             valorCredito,
                                             valorPago,
                                             valorPendente));
                
                // Acumular totais
                totalLimites = totalLimites.add(limiteCredito);
                totalCredito = totalCredito.add(valorCredito);
                totalPago = totalPago.add(valorPago);
                totalPendente = totalPendente.add(valorPendente);
                totalVendas += qtdVendas;
                
                // Armazenar para análise posterior
                clientesCredito.add(new ClienteCredito(clienteId, clienteNome, cpfCnpj, 
                                                     limiteCredito, valorCredito, valorPago, valorPendente));
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-30s %-15s R$%-10.2f %-12d R$%-13.2f R$%-13.2f R$%-13.2f\n",
                                         "TOTAL", "",
                                         totalLimites,
                                         totalVendas,
                                         totalCredito,
                                         totalPago,
                                         totalPendente));
            
            // Parte 2: Análise de utilização de limite de crédito
            relatorio.append("\nANÁLISE DE UTILIZAÇÃO DE LIMITE DE CRÉDITO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            relatorio.append(String.format("%-30s %-15s %-15s %-15s %-15s\n",
                                         "CLIENTE", "LIMITE", "UTILIZADO", "DISPONÍVEL", "% UTILIZADO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Ordenar clientes por percentual de utilização do limite (decrescente)
            clientesCredito.sort((c1, c2) -> {
                BigDecimal percentual1 = c1.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0 ? 
                        c1.getValorPendente().multiply(new BigDecimal(100)).divide(c1.getLimiteCredito(), 2, RoundingMode.HALF_UP) : 
                        new BigDecimal(0);
                        
                BigDecimal percentual2 = c2.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0 ? 
                        c2.getValorPendente().multiply(new BigDecimal(100)).divide(c2.getLimiteCredito(), 2, RoundingMode.HALF_UP) : 
                        new BigDecimal(0);
                        
                return percentual2.compareTo(percentual1);
            });
            
            for (ClienteCredito cliente : clientesCredito) {
                BigDecimal disponivel = cliente.getLimiteCredito().subtract(cliente.getValorPendente());
                if (disponivel.compareTo(BigDecimal.ZERO) < 0) {
                    disponivel = BigDecimal.ZERO;
                }
                
                BigDecimal percentualUtilizado = BigDecimal.ZERO;
                if (cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                    percentualUtilizado = cliente.getValorPendente()
                            .multiply(new BigDecimal(100))
                            .divide(cliente.getLimiteCredito(), 2, RoundingMode.HALF_UP);
                }
                
                // Indicador visual para clientes próximos ou acima do limite
                String indicador = "";
                if (percentualUtilizado.compareTo(new BigDecimal(90)) >= 0) {
                    indicador = " [CRÍTICO]";
                } else if (percentualUtilizado.compareTo(new BigDecimal(75)) >= 0) {
                    indicador = " [ALERTA]";
                }
                
                relatorio.append(String.format("%-30s R$%-13.2f R$%-13.2f R$%-13.2f %-15.2f%%%s\n",
                                             (cliente.getNome().length() > 28 ? cliente.getNome().substring(0, 25) + "..." : cliente.getNome()),
                                             cliente.getLimiteCredito(),
                                             cliente.getValorPendente(),
                                             disponivel,
                                             percentualUtilizado,
                                             indicador));
            }
            
            // Parte 3: Análise de inadimplência por cliente
            relatorio.append("\nANÁLISE DE INADIMPLÊNCIA POR CLIENTE\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Se não existe a tabela parcelas_crediario, informar e parar
            if (!tabelaParcelas) {
                relatorio.append("Análise de inadimplência não disponível sem a tabela 'parcelas_crediario'.\n");
                return relatorio.toString();
            }
            
            // Para cada cliente, analisar parcelas em atraso
            for (ClienteCredito cliente : clientesCredito) {
                // Consulta para obter parcelas em atraso por cliente
                sql = "SELECT " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) <= ? THEN valor_parcela ELSE 0 END) as atraso_baixo, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > ? AND DATEDIFF(NOW(), data_vencimento) <= ? THEN valor_parcela ELSE 0 END) as atraso_medio, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > ? AND DATEDIFF(NOW(), data_vencimento) <= ? THEN valor_parcela ELSE 0 END) as atraso_alto, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > ? AND DATEDIFF(NOW(), data_vencimento) <= ? THEN valor_parcela ELSE 0 END) as atraso_critico, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > ? THEN valor_parcela ELSE 0 END) as atraso_grave, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > 0 THEN valor_parcela ELSE 0 END) as total_atraso " +
                     "FROM parcelas_crediario " +
                     "JOIN vendas ON parcelas_crediario.venda_id = vendas.id " +
                     "WHERE vendas.cliente_id = ? " +
                     "AND vendas.data_venda BETWEEN ? AND ? " +
                     "AND vendas.forma_pagamento = 'CREDIARIO' " +
                     "AND vendas.status = 'FINALIZADA'";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, ATRASO_BAIXO);
                stmt.setInt(2, ATRASO_BAIXO);
                stmt.setInt(3, ATRASO_MEDIO);
                stmt.setInt(4, ATRASO_MEDIO);
                stmt.setInt(5, ATRASO_ALTO);
                stmt.setInt(6, ATRASO_ALTO);
                stmt.setInt(7, ATRASO_CRITICO);
                stmt.setInt(8, ATRASO_CRITICO);
                stmt.setInt(9, cliente.getId());
                stmt.setObject(10, dataInicio);
                stmt.setObject(11, dataFim);
                
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    BigDecimal totalAtraso = rs.getBigDecimal("total_atraso");
                    
                    // Se o cliente tem valores em atraso, mostrar detalhes
                    if (totalAtraso != null && totalAtraso.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal atrasoBaixo = rs.getBigDecimal("atraso_baixo");
                        BigDecimal atrasoMedio = rs.getBigDecimal("atraso_medio");
                        BigDecimal atrasoAlto = rs.getBigDecimal("atraso_alto");
                        BigDecimal atrasoCritico = rs.getBigDecimal("atraso_critico");
                        BigDecimal atrasoGrave = rs.getBigDecimal("atraso_grave");
                        
                        relatorio.append(String.format("\nCliente: %s (CPF/CNPJ: %s)\n", 
                                                     cliente.getNome(), 
                                                     formatarCpfCnpj(cliente.getCpfCnpj())));
                        relatorio.append(String.format("Total em atraso: R$ %.2f\n", totalAtraso));
                        relatorio.append(String.format("Até %d dias: R$ %.2f\n", ATRASO_BAIXO, atrasoBaixo));
                        relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_BAIXO + 1, ATRASO_MEDIO, atrasoMedio));
                        relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_MEDIO + 1, ATRASO_ALTO, atrasoAlto));
                        relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_ALTO + 1, ATRASO_CRITICO, atrasoCritico));
                        relatorio.append(String.format("Acima de %d dias: R$ %.2f\n", ATRASO_CRITICO, atrasoGrave));
                        
                        // Listar parcelas em atraso
                        sql = "SELECT pc.numero_parcela, pc.data_vencimento, pc.valor_parcela, " +
                             "DATEDIFF(NOW(), pc.data_vencimento) as dias_atraso, " +
                             "v.id as venda_id, v.data_venda " +
                             "FROM parcelas_crediario pc " +
                             "JOIN vendas v ON pc.venda_id = v.id " +
                             "WHERE v.cliente_id = ? " +
                             "AND v.data_venda BETWEEN ? AND ? " +
                             "AND v.forma_pagamento = 'CREDIARIO' " +
                             "AND v.status = 'FINALIZADA' " +
                             "AND pc.status_pagamento != 'PAGO' " +
                             "AND pc.data_vencimento < NOW() " +
                             "ORDER BY pc.data_vencimento";
                        
                        stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, cliente.getId());
                        stmt.setObject(2, dataInicio);
                        stmt.setObject(3, dataFim);
                        
                        rs = stmt.executeQuery();
                        
                        relatorio.append("\nParcelas em atraso:\n");
                        relatorio.append(String.format("%-12s %-15s %-12s %-15s %-15s\n",
                                                     "VENDA", "DATA VENDA", "PARCELA", "VENCIMENTO", "DIAS ATRASO"));
                        relatorio.append("--------------------------------------------------------------------------------\n");
                        
                        while (rs.next()) {
                            int vendaId = rs.getInt("venda_id");
                            LocalDate dataVenda = rs.getDate("data_venda").toLocalDate();
                            int numeroParcela = rs.getInt("numero_parcela");
                            LocalDate dataVencimento = rs.getDate("data_vencimento").toLocalDate();
                            BigDecimal valorParcela = rs.getBigDecimal("valor_parcela");
                            int diasAtraso = rs.getInt("dias_atraso");
                            
                            relatorio.append(String.format("%-12d %-15s %-12d %-15s %-15d R$ %.2f\n",
                                                         vendaId,
                                                         dataVenda.format(dateFormatter),
                                                         numeroParcela,
                                                         dataVencimento.format(dateFormatter),
                                                         diasAtraso,
                                                         valorParcela));
                        }
                        
                        relatorio.append("--------------------------------------------------------------------------------\n");
                    }
                }
            }
            
            // Parte 4: Histórico de pagamentos por cliente
            relatorio.append("\nHISTÓRICO DE PAGAMENTOS RECENTES\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Selecionar os top 5 clientes com maior volume de pagamentos no período
            sql = "SELECT c.id as cliente_id, c.nome as cliente_nome, " +
                 "COUNT(DISTINCT v.id) as total_vendas, " +
                 "SUM(CASE WHEN pc.status_pagamento = 'PAGO' THEN pc.valor_parcela ELSE 0 END) as valor_pago " +
                 "FROM clientes c " +
                 "JOIN vendas v ON v.cliente_id = c.id " +
                 "JOIN parcelas_crediario pc ON pc.venda_id = v.id " +
                 "WHERE v.data_venda BETWEEN ? AND ? " +
                 "AND pc.data_pagamento BETWEEN ? AND ? " +
                 "AND v.forma_pagamento = 'CREDIARIO' " +
                 "AND v.status = 'FINALIZADA' " +
                 "GROUP BY c.id, c.nome " +
                 "ORDER BY valor_pago DESC " +
                 "LIMIT 5";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            stmt.setObject(3, dataInicio);
            stmt.setObject(4, dataFim);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                int clienteId = rs.getInt("cliente_id");
                String clienteNome = rs.getString("cliente_nome");
                BigDecimal valorPago = rs.getBigDecimal("valor_pago");
                
                relatorio.append(String.format("\nCliente: %s\n", clienteNome));
                relatorio.append(String.format("Total pago no período: R$ %.2f\n", valorPago));
                
                // Buscar detalhes dos pagamentos deste cliente
                sql = "SELECT pc.venda_id, pc.numero_parcela, pc.data_vencimento, " +
                     "pc.data_pagamento, pc.valor_parcela, pc.juros_multa " +
                     "FROM parcelas_crediario pc " +
                     "JOIN vendas v ON pc.venda_id = v.id " +
                     "WHERE v.cliente_id = ? " +
                     "AND pc.data_pagamento BETWEEN ? AND ? " +
                     "AND pc.status_pagamento = 'PAGO' " +
                     "ORDER BY pc.data_pagamento DESC " +
                     "LIMIT 10";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, clienteId);
                stmt.setObject(2, dataInicio);
                stmt.setObject(3, dataFim);
                
                ResultSet rsPagamentos = stmt.executeQuery();
                
                relatorio.append(String.format("%-12s %-12s %-15s %-15s %-12s %-12s\n",
                                             "VENDA", "PARCELA", "VENCIMENTO", "PAGAMENTO", "VALOR", "JUROS/MULTA"));
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                while (rsPagamentos.next()) {
                    int vendaId = rsPagamentos.getInt("venda_id");
                    int numeroParcela = rsPagamentos.getInt("numero_parcela");
                    LocalDate dataVencimento = rsPagamentos.getDate("data_vencimento").toLocalDate();
                    LocalDate dataPagamento = rsPagamentos.getDate("data_pagamento").toLocalDate();
                    BigDecimal valorParcela = rsPagamentos.getBigDecimal("valor_parcela");
                    BigDecimal jurosMulta = rsPagamentos.getBigDecimal("juros_multa");
                    
                    relatorio.append(String.format("%-12d %-12d %-15s %-15s R$ %-9.2f R$ %-9.2f\n",
                                                 vendaId,
                                                 numeroParcela,
                                                 dataVencimento.format(dateFormatter),
                                                 dataPagamento.format(dateFormatter),
                                                 valorParcela,
                                                 jurosMulta));
                }
                
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                // Fechar o ResultSet adicional
                rsPagamentos.close();
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de crédito por cliente", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
    
    /**
     * Verifica se uma tabela existe no banco de dados
     * 
     * @param conn Conexão com o banco de dados
     * @param nomeTabela Nome da tabela a verificar
     * @return true se a tabela existe, false caso contrário
     */
    private boolean verificarTabelaExiste(Connection conn, String nomeTabela) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                       "WHERE table_schema = DATABASE() AND table_name = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomeTabela);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }
    
    /**
     * Verifica se uma coluna existe em uma tabela
     * 
     * @param conn Conexão com o banco de dados
     * @param nomeTabela Nome da tabela
     * @param nomeColuna Nome da coluna
     * @return true se a coluna existe, false caso contrário
     */
    private boolean verificarColunaExiste(Connection conn, String nomeTabela, String nomeColuna) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                       "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nomeTabela);
            stmt.setString(2, nomeColuna);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }
    
    /**
     * Formata o CPF/CNPJ para exibição
     */
    private String formatarCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null) {
            return "";
        }
        
        // Remove caracteres não numéricos
        String numeros = cpfCnpj.replaceAll("\\D", "");
        
        // Formata CPF: XXX.XXX.XXX-XX
        if (numeros.length() == 11) {
            return numeros.substring(0, 3) + "." + 
                   numeros.substring(3, 6) + "." + 
                   numeros.substring(6, 9) + "-" + 
                   numeros.substring(9);
        } 
        // Formata CNPJ: XX.XXX.XXX/XXXX-XX
        else if (numeros.length() == 14) {
            return numeros.substring(0, 2) + "." + 
                   numeros.substring(2, 5) + "." + 
                   numeros.substring(5, 8) + "/" + 
                   numeros.substring(8, 12) + "-" + 
                   numeros.substring(12);
        }
        
        // Se não for CPF nem CNPJ, retorna como está
        return cpfCnpj;
    }
    
    /**
     * Classe interna auxiliar para armazenar informações de crédito por cliente
     */
    private static class ClienteCredito {
        private final int id;
        private final String nome;
        private final String cpfCnpj;
        private final BigDecimal limiteCredito;
        private final BigDecimal valorCredito;
        private final BigDecimal valorPago;
        private final BigDecimal valorPendente;
        
        public ClienteCredito(int id, String nome, String cpfCnpj, BigDecimal limiteCredito, 
                             BigDecimal valorCredito, BigDecimal valorPago, BigDecimal valorPendente) {
            this.id = id;
            this.nome = nome;
            this.cpfCnpj = cpfCnpj;
            this.limiteCredito = limiteCredito;
            this.valorCredito = valorCredito;
            this.valorPago = valorPago;
            this.valorPendente = valorPendente;
        }
        
        public int getId() {
            return id;
        }
        
        public String getNome() {
            return nome;
        }
        
        public String getCpfCnpj() {
            return cpfCnpj;
        }
        
        public BigDecimal getLimiteCredito() {
            return limiteCredito;
        }
        
        public BigDecimal getValorCredito() {
            return valorCredito;
        }
        
        public BigDecimal getValorPago() {
            return valorPago;
        }
        
        public BigDecimal getValorPendente() {
            return valorPendente;
        }
    }
}