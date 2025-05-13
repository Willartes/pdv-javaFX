package br.com.pdv.relatorio;

import br.com.pdv.dao.VendaDAO;
import br.com.pdv.dao.ClienteDAO;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Relatório de Crédito Por Período
 * Exibe informações sobre vendas realizadas no crediário durante um determinado período,
 * incluindo valores a receber, pagamentos recebidos e análise de inadimplência.
 */
public class RelatorioCreditoPorPeriodo extends RelatorioBase {
    private final VendaDAO vendaDAO;
    private final ClienteDAO clienteDAO;
    
    // Define categorias de atraso para análise de inadimplência
    private static final int ATRASO_BAIXO = 15;     // Até 15 dias
    private static final int ATRASO_MEDIO = 30;     // Até 30 dias
    private static final int ATRASO_ALTO = 60;      // Até 60 dias
    private static final int ATRASO_CRITICO = 90;   // Até 90 dias
    
    /**
     * Construtor do relatório de crédito por período
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioCreditoPorPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE CREDIÁRIO POR PERÍODO", dataInicio, dataFim);
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
            
            // Verificar estrutura do banco de dados
            boolean tabelaParcelas = verificarTabelaExiste(conn, "parcelas_crediario");
            boolean colunaValorEntrada = verificarColunaExiste(conn, "vendas", "valor_entrada");
            
            // --------- PARTE 1: RESUMO GERAL DO CREDIÁRIO ---------
            relatorio.append("RESUMO GERAL DO CREDIÁRIO NO PERÍODO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Avisos sobre estrutura do banco de dados
            if (!tabelaParcelas) {
                relatorio.append("NOTA: A tabela 'parcelas_crediario' não foi encontrada no banco de dados.\n");
                relatorio.append("      Algumas informações detalhadas de crediário não estarão disponíveis.\n\n");
            }
            
            if (!colunaValorEntrada) {
                relatorio.append("NOTA: A coluna 'valor_entrada' não foi encontrada na tabela 'vendas'.\n");
                relatorio.append("      O sistema assumirá que não há valores de entrada nas vendas a crédito.\n\n");
            }
            
            // Consulta básica que funciona mesmo sem a tabela parcelas_crediario
            String sql = "SELECT COUNT(*) as total_vendas, " +
                        "SUM(valor_total) as valor_total ";
                        
            if (colunaValorEntrada) {
                sql += ", SUM(valor_entrada) as valor_entrada, " +
                      "SUM(valor_total - valor_entrada) as valor_credito ";
            } else {
                sql += ", 0 as valor_entrada, " +
                      "SUM(valor_total) as valor_credito ";
            }
            
            sql += "FROM vendas " +
                  "WHERE data_venda BETWEEN ? AND ? " +
                  "AND forma_pagamento = 'CREDIARIO' " +
                  "AND status = 'FINALIZADA'";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int totalVendas = rs.getInt("total_vendas");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                if (valorTotal == null) valorTotal = BigDecimal.ZERO;
                
                BigDecimal valorEntrada = rs.getBigDecimal("valor_entrada");
                if (valorEntrada == null) valorEntrada = BigDecimal.ZERO;
                
                BigDecimal valorCredito = rs.getBigDecimal("valor_credito");
                if (valorCredito == null) valorCredito = BigDecimal.ZERO;
                
                relatorio.append(String.format("Total de vendas no crediário: %d\n", totalVendas));
                relatorio.append(String.format("Valor total de vendas: R$ %.2f\n", valorTotal));
                relatorio.append(String.format("Valor total de entradas: R$ %.2f\n", valorEntrada));
                relatorio.append(String.format("Valor total parcelado: R$ %.2f\n", valorCredito));
                
                // Valores recebidos e pendentes (somente se existir a tabela de parcelas)
                if (tabelaParcelas) {
                    String sqlParcelas = "SELECT " +
                                       "SUM(CASE WHEN status_pagamento = 'PAGO' THEN valor_parcela ELSE 0 END) as valor_recebido, " +
                                       "SUM(CASE WHEN status_pagamento != 'PAGO' THEN valor_parcela ELSE 0 END) as valor_pendente " +
                                       "FROM parcelas_crediario " +
                                       "JOIN vendas ON parcelas_crediario.venda_id = vendas.id " +
                                       "WHERE vendas.data_venda BETWEEN ? AND ? " +
                                       "AND vendas.forma_pagamento = 'CREDIARIO' " +
                                       "AND vendas.status = 'FINALIZADA'";
                    
                    PreparedStatement stmtParcelas = conn.prepareStatement(sqlParcelas);
                    stmtParcelas.setObject(1, dataInicio);
                    stmtParcelas.setObject(2, dataFim);
                    
                    ResultSet rsParcelas = stmtParcelas.executeQuery();
                    
                    if (rsParcelas.next()) {
                        BigDecimal valorRecebido = rsParcelas.getBigDecimal("valor_recebido");
                        if (valorRecebido == null) valorRecebido = BigDecimal.ZERO;
                        
                        BigDecimal valorPendente = rsParcelas.getBigDecimal("valor_pendente");
                        if (valorPendente == null) valorPendente = BigDecimal.ZERO;
                        
                        relatorio.append(String.format("Valor já recebido: R$ %.2f\n", valorRecebido));
                        relatorio.append(String.format("Valor pendente: R$ %.2f\n", valorPendente));
                        
                        // Calcular percentual de inadimplência
                        if (valorCredito.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal percentualInadimplencia = valorPendente.multiply(new BigDecimal(100))
                                                              .divide(valorCredito, 2, RoundingMode.HALF_UP);
                            relatorio.append(String.format("Percentual de valores pendentes: %.2f%%\n", percentualInadimplencia));
                        }
                    }
                    
                    rsParcelas.close();
                    stmtParcelas.close();
                } else {
                    relatorio.append("Valor já recebido: Informação não disponível\n");
                    relatorio.append("Valor pendente: Informação não disponível\n");
                }
            }
            
            // --------- PARTE 2: VENDAS NO CREDIÁRIO POR DIA ---------
            relatorio.append("\nVENDAS NO CREDIÁRIO POR DIA\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT DATE(data_venda) as data, COUNT(*) as total_vendas, " +
                 "SUM(valor_total) as valor_total ";
                 
            if (colunaValorEntrada) {
                sql += ", SUM(valor_entrada) as valor_entrada, " +
                      "SUM(valor_total - valor_entrada) as valor_credito ";
            } else {
                sql += ", 0 as valor_entrada, " +
                      "SUM(valor_total) as valor_credito ";
            }
            
            sql += "FROM vendas " +
                  "WHERE data_venda BETWEEN ? AND ? " +
                  "AND forma_pagamento = 'CREDIARIO' " +
                  "AND status = 'FINALIZADA' " +
                  "GROUP BY DATE(data_venda) " +
                  "ORDER BY data";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-12s %-12s %-15s %-15s %-15s\n", 
                                         "DATA", "QTD VENDAS", "VALOR TOTAL", "ENTRADAS", "PARCELADO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal totalVendas = BigDecimal.ZERO;
            BigDecimal totalEntradas = BigDecimal.ZERO;
            BigDecimal totalParcelado = BigDecimal.ZERO;
            int totalQuantidadeVendas = 0;
            
            while (rs.next()) {
                LocalDate data = rs.getDate("data").toLocalDate();
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                if (valorTotal == null) valorTotal = BigDecimal.ZERO;
                
                BigDecimal valorEntrada = rs.getBigDecimal("valor_entrada");
                if (valorEntrada == null) valorEntrada = BigDecimal.ZERO;
                
                BigDecimal valorCredito = rs.getBigDecimal("valor_credito");
                if (valorCredito == null) valorCredito = BigDecimal.ZERO;
                
                relatorio.append(String.format("%-12s %-12d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                             data.format(dateFormatter),
                                             qtdVendas,
                                             valorTotal,
                                             valorEntrada,
                                             valorCredito));
                
                totalVendas = totalVendas.add(valorTotal);
                totalEntradas = totalEntradas.add(valorEntrada);
                totalParcelado = totalParcelado.add(valorCredito);
                totalQuantidadeVendas += qtdVendas;
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-12s %-12d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                         "TOTAL",
                                         totalQuantidadeVendas,
                                         totalVendas,
                                         totalEntradas,
                                         totalParcelado));
            
            // --------- PARTE 3: ANÁLISE DE INADIMPLÊNCIA ---------
            relatorio.append("\nANÁLISE DE INADIMPLÊNCIA\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            if (!tabelaParcelas) {
                relatorio.append("Análise de inadimplência não disponível (tabela 'parcelas_crediario' não existe).\n");
            } else {
                sql = "SELECT " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) BETWEEN 1 AND ? THEN valor_parcela ELSE 0 END) as atraso_baixo, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) BETWEEN ? AND ? THEN valor_parcela ELSE 0 END) as atraso_medio, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) BETWEEN ? AND ? THEN valor_parcela ELSE 0 END) as atraso_alto, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) BETWEEN ? AND ? THEN valor_parcela ELSE 0 END) as atraso_critico, " +
                     "SUM(CASE WHEN status_pagamento != 'PAGO' AND DATEDIFF(NOW(), data_vencimento) > ? THEN valor_parcela ELSE 0 END) as atraso_grave " +
                     "FROM parcelas_crediario " +
                     "JOIN vendas ON parcelas_crediario.venda_id = vendas.id " +
                     "WHERE vendas.data_venda BETWEEN ? AND ? " +
                     "AND vendas.forma_pagamento = 'CREDIARIO' " +
                     "AND vendas.status = 'FINALIZADA'";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, ATRASO_BAIXO);
                stmt.setInt(2, ATRASO_BAIXO + 1);
                stmt.setInt(3, ATRASO_MEDIO);
                stmt.setInt(4, ATRASO_MEDIO + 1);
                stmt.setInt(5, ATRASO_ALTO);
                stmt.setInt(6, ATRASO_ALTO + 1);
                stmt.setInt(7, ATRASO_CRITICO);
                stmt.setInt(8, ATRASO_CRITICO);
                stmt.setObject(9, dataInicio);
                stmt.setObject(10, dataFim);
                
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    BigDecimal atrasoBaixo = rs.getBigDecimal("atraso_baixo");
                    if (atrasoBaixo == null) atrasoBaixo = BigDecimal.ZERO;
                    
                    BigDecimal atrasoMedio = rs.getBigDecimal("atraso_medio");
                    if (atrasoMedio == null) atrasoMedio = BigDecimal.ZERO;
                    
                    BigDecimal atrasoAlto = rs.getBigDecimal("atraso_alto");
                    if (atrasoAlto == null) atrasoAlto = BigDecimal.ZERO;
                    
                    BigDecimal atrasoCritico = rs.getBigDecimal("atraso_critico");
                    if (atrasoCritico == null) atrasoCritico = BigDecimal.ZERO;
                    
                    BigDecimal atrasoGrave = rs.getBigDecimal("atraso_grave");
                    if (atrasoGrave == null) atrasoGrave = BigDecimal.ZERO;
                    
                    // Total de valores em atraso
                    BigDecimal totalAtraso = atrasoBaixo.add(atrasoMedio).add(atrasoAlto).add(atrasoCritico).add(atrasoGrave);
                    
                    relatorio.append(String.format("Valores em atraso por categoria:\n"));
                    relatorio.append(String.format("Até %d dias: R$ %.2f\n", ATRASO_BAIXO, atrasoBaixo));
                    relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_BAIXO + 1, ATRASO_MEDIO, atrasoMedio));
                    relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_MEDIO + 1, ATRASO_ALTO, atrasoAlto));
                    relatorio.append(String.format("De %d a %d dias: R$ %.2f\n", ATRASO_ALTO + 1, ATRASO_CRITICO, atrasoCritico));
                    relatorio.append(String.format("Acima de %d dias: R$ %.2f\n", ATRASO_CRITICO, atrasoGrave));
                    relatorio.append(String.format("Total em atraso: R$ %.2f\n", totalAtraso));
                    
                    // Gráfico de distribuição dos atrasos
                    if (totalAtraso.compareTo(BigDecimal.ZERO) > 0) {
                        relatorio.append("\nDISTRIBUIÇÃO DOS VALORES EM ATRASO\n");
                        relatorio.append("--------------------------------------------------------------------------------\n");
                        
                        Map<String, BigDecimal> valoresPorCategoria = new HashMap<>();
                        valoresPorCategoria.put(String.format("Até %d dias", ATRASO_BAIXO), atrasoBaixo);
                        valoresPorCategoria.put(String.format("%d-%d dias", ATRASO_BAIXO + 1, ATRASO_MEDIO), atrasoMedio);
                        valoresPorCategoria.put(String.format("%d-%d dias", ATRASO_MEDIO + 1, ATRASO_ALTO), atrasoAlto);
                        valoresPorCategoria.put(String.format("%d-%d dias", ATRASO_ALTO + 1, ATRASO_CRITICO), atrasoCritico);
                        valoresPorCategoria.put(String.format("+%d dias", ATRASO_CRITICO), atrasoGrave);
                        
                        for (Map.Entry<String, BigDecimal> entry : valoresPorCategoria.entrySet()) {
                            String categoria = entry.getKey();
                            BigDecimal valor = entry.getValue();
                            
                            if (valor.compareTo(BigDecimal.ZERO) > 0) {
                                double percentual = valor.multiply(new BigDecimal(100))
                                                    .divide(totalAtraso, 2, RoundingMode.HALF_UP)
                                                    .doubleValue();
                                
                                int barras = (int) (percentual / 2); // Cada barra representa 2%
                                StringBuilder grafico = new StringBuilder();
                                for (int i = 0; i < barras; i++) {
                                    grafico.append("█");
                                }
                                
                                relatorio.append(String.format("%-15s [%-50s] %.1f%% (R$ %.2f)\n", 
                                                             categoria,
                                                             grafico.toString(),
                                                             percentual,
                                                             valor));
                            }
                        }
                    }
                }
            }
            
            // --------- PARTE 4: PREVISÃO DE RECEBIMENTOS FUTUROS ---------
            relatorio.append("\nPREVISÃO DE RECEBIMENTOS FUTUROS\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            if (!tabelaParcelas) {
                relatorio.append("Previsão de recebimentos futuros não disponível (tabela 'parcelas_crediario' não existe).\n");
            } else {
                sql = "SELECT DATE_FORMAT(data_vencimento, '%Y-%m') as mes_vencimento, " +
                     "SUM(valor_parcela) as valor_previsto " +
                     "FROM parcelas_crediario " +
                     "JOIN vendas ON parcelas_crediario.venda_id = vendas.id " +
                     "WHERE vendas.data_venda BETWEEN ? AND ? " +
                     "AND vendas.forma_pagamento = 'CREDIARIO' " +
                     "AND vendas.status = 'FINALIZADA' " +
                     "AND parcelas_crediario.status_pagamento != 'PAGO' " +
                     "GROUP BY DATE_FORMAT(data_vencimento, '%Y-%m') " +
                     "ORDER BY mes_vencimento";
                
                stmt = conn.prepareStatement(sql);
                stmt.setObject(1, dataInicio);
                stmt.setObject(2, dataFim);
                
                rs = stmt.executeQuery();
                
                boolean temDados = false;
                
                relatorio.append(String.format("%-20s %-20s\n", "MÊS DE VENCIMENTO", "VALOR PREVISTO"));
                relatorio.append("--------------------------------------------------------------------------------\n");
                
                while (rs.next()) {
                    temDados = true;
                    String mesVencimento = rs.getString("mes_vencimento");
                    BigDecimal valorPrevisto = rs.getBigDecimal("valor_previsto");
                    if (valorPrevisto == null) valorPrevisto = BigDecimal.ZERO;
                    
                    // Transformar o formato YYYY-MM em uma data para formatação
                    LocalDate dataVencimento = LocalDate.parse(mesVencimento + "-01");
                    String mesFormatado = dataVencimento.format(DateTimeFormatter.ofPattern("MMMM/yyyy"));
                    
                    relatorio.append(String.format("%-20s R$ %-17.2f\n", 
                                                 mesFormatado,
                                                 valorPrevisto));
                }
                
                if (!temDados) {
                    relatorio.append("Não há previsão de recebimentos futuros para o período informado.\n");
                }
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de crédito por período", e);
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
}