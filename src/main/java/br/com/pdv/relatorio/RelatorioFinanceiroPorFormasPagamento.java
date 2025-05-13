package br.com.pdv.relatorio;

import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Relatório Financeiro por Formas de Pagamento
 * Exibe os valores arrecadados por cada forma de pagamento em um período
 */
public class RelatorioFinanceiroPorFormasPagamento extends RelatorioBase {
    private final VendaDAO vendaDAO;
    
    /**
     * Construtor do relatório financeiro por formas de pagamento
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioFinanceiroPorFormasPagamento(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO FINANCEIRO POR FORMAS DE PAGAMENTO", dataInicio, dataFim);
        this.vendaDAO = RelatorioManager.getInstance().getVendaDAO();
    }
    
    @Override
    public String gerarRelatorio() throws SQLException {
        StringBuilder relatorio = new StringBuilder(gerarCabecalho());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = databaseConnection.getConnection();
            
            // Parte 1: Resumo geral por forma de pagamento
            relatorio.append("RESUMO GERAL POR FORMA DE PAGAMENTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            String sql = "SELECT forma_pagamento, COUNT(*) as total_vendas, " +
                       "SUM(valor_total) as valor_total, " +
                       "SUM(valor_desconto) as valor_desconto, " +
                       "SUM(valor_total - valor_desconto) as valor_liquido " +
                       "FROM vendas " +
                       "WHERE data_venda BETWEEN ? AND ? " +
                       "AND status = 'FINALIZADA' " +
                       "GROUP BY forma_pagamento " +
                       "ORDER BY valor_liquido DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-20s %-12s %-15s %-15s %-15s\n", 
                                         "FORMA PAGAMENTO", "QTD VENDAS", "VALOR BRUTO", "DESCONTOS", "VALOR LÍQUIDO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal totalBruto = BigDecimal.ZERO;
            BigDecimal totalDescontos = BigDecimal.ZERO;
            BigDecimal totalLiquido = BigDecimal.ZERO;
            int totalVendas = 0;
            
            Map<String, BigDecimal> valoresPorFormaPagamento = new LinkedHashMap<>();
            
            while (rs.next()) {
                String formaPagamento = rs.getString("forma_pagamento");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorBruto = rs.getBigDecimal("valor_total");
                BigDecimal valorDesconto = rs.getBigDecimal("valor_desconto");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                relatorio.append(String.format("%-20s %-12d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                             formaPagamento,
                                             qtdVendas,
                                             valorBruto,
                                             valorDesconto,
                                             valorLiquido));
                
                totalBruto = totalBruto.add(valorBruto);
                totalDescontos = totalDescontos.add(valorDesconto);
                totalLiquido = totalLiquido.add(valorLiquido);
                totalVendas += qtdVendas;
                
                valoresPorFormaPagamento.put(formaPagamento, valorLiquido);
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-20s %-12d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                         "TOTAL",
                                         totalVendas,
                                         totalBruto,
                                         totalDescontos,
                                         totalLiquido));
            
            // Parte 2: Gráfico de distribuição
            relatorio.append("\nDISTRIBUIÇÃO POR FORMA DE PAGAMENTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            for (Map.Entry<String, BigDecimal> entry : valoresPorFormaPagamento.entrySet()) {
                String formaPagamento = entry.getKey();
                BigDecimal valor = entry.getValue();
                
                double percentual = totalLiquido.compareTo(BigDecimal.ZERO) > 0 ? 
                        valor.multiply(new BigDecimal(100)).divide(totalLiquido, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
                
                int barras = (int) (percentual / 2); // Cada barra representa 2%
                StringBuilder grafico = new StringBuilder();
                for (int i = 0; i < barras; i++) {
                    grafico.append("█");
                }
                
                relatorio.append(String.format("%-20s [%-50s] %.1f%% (R$ %.2f)\n", 
                                             formaPagamento,
                                             grafico.toString(),
                                             percentual,
                                             valor));
            }
            
            // Parte 3: Evolução diária por forma de pagamento
            relatorio.append("\nEVOLUÇÃO DIÁRIA POR FORMA DE PAGAMENTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT DATE(data_venda) as data, forma_pagamento, " +
                 "COUNT(*) as total_vendas, " +
                 "SUM(valor_total - valor_desconto) as valor_liquido " +
                 "FROM vendas " +
                 "WHERE data_venda BETWEEN ? AND ? " +
                 "AND status = 'FINALIZADA' " +
                 "GROUP BY DATE(data_venda), forma_pagamento " +
                 "ORDER BY data, valor_liquido DESC";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-12s %-20s %-12s %-15s\n", 
                                         "DATA", "FORMA PAGAMENTO", "QTD VENDAS", "VALOR LÍQUIDO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            LocalDate dataAtual = null;
            while (rs.next()) {
                LocalDate data = rs.getDate("data").toLocalDate();
                String formaPagamento = rs.getString("forma_pagamento");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                // Se mudou o dia, imprime uma linha separadora
                if (dataAtual != null && !dataAtual.equals(data)) {
                    relatorio.append("--------------------------------------------------------------------------------\n");
                }
                
                relatorio.append(String.format("%-12s %-20s %-12d R$ %-12.2f\n", 
                                             data.format(dateFormatter),
                                             formaPagamento,
                                             qtdVendas,
                                             valorLiquido));
                
                dataAtual = data;
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório financeiro por formas de pagamento", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}