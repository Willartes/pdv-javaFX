package br.com.pdv.relatorio;

import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Relatório de Vendas Totais
 * Exibe o total de vendas por dia dentro de um período
 */
public class RelatorioVendasTotais extends RelatorioBase {
    private final VendaDAO vendaDAO;
    
    /**
     * Construtor do relatório de vendas totais
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioVendasTotais(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE VENDAS TOTAIS", dataInicio, dataFim);
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
            
            // Consulta para obter vendas totais por dia
            String sql = "SELECT DATE(data_venda) as data, COUNT(*) as total_vendas, " +
                       "SUM(valor_total) as valor_total " +
                       "FROM vendas " +
                       "WHERE data_venda BETWEEN ? AND ? " +
                       "AND status = 'FINALIZADA' " +
                       "GROUP BY DATE(data_venda) " +
                       "ORDER BY data";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-12s %-15s %-15s\n", "DATA", "TOTAL VENDAS", "VALOR TOTAL"));
            relatorio.append("--------------------------------------------\n");
            
            BigDecimal valorTotalPeriodo = BigDecimal.ZERO;
            int totalVendasPeriodo = 0;
            
            while (rs.next()) {
                LocalDate data = rs.getDate("data").toLocalDate();
                int totalVendas = rs.getInt("total_vendas");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                
                relatorio.append(String.format("%-12s %-15d R$ %-12.2f\n", 
                                             data.format(dateFormatter), 
                                             totalVendas,
                                             valorTotal));
                
                valorTotalPeriodo = valorTotalPeriodo.add(valorTotal);
                totalVendasPeriodo += totalVendas;
            }
            
            relatorio.append("--------------------------------------------\n");
            relatorio.append(String.format("%-12s %-15d R$ %-12.2f\n", 
                                         "TOTAL", 
                                         totalVendasPeriodo,
                                         valorTotalPeriodo));
            
            // Estatísticas adicionais
            relatorio.append("\nESTATÍSTICAS ADICIONAIS\n");
            relatorio.append("--------------------------------------------\n");
            
            // Ticket médio
            if (totalVendasPeriodo > 0) {
                BigDecimal ticketMedio = valorTotalPeriodo.divide(
                        new BigDecimal(totalVendasPeriodo), 2, RoundingMode.HALF_UP);
                relatorio.append(String.format("Ticket médio: R$ %.2f\n", ticketMedio));
            }
            
            // Consulta para melhor dia de vendas
            sql = "SELECT DATE(data_venda) as data, COUNT(*) as total_vendas, " +
                 "SUM(valor_total) as valor_total " +
                 "FROM vendas " +
                 "WHERE data_venda BETWEEN ? AND ? " +
                 "AND status = 'FINALIZADA' " +
                 "GROUP BY DATE(data_venda) " +
                 "ORDER BY valor_total DESC LIMIT 1";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                LocalDate melhorDia = rs.getDate("data").toLocalDate();
                BigDecimal valorMelhorDia = rs.getBigDecimal("valor_total");
                relatorio.append(String.format("Melhor dia de vendas: %s - R$ %.2f\n", 
                                             melhorDia.format(dateFormatter), 
                                             valorMelhorDia));
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de vendas totais", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}