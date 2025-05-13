package br.com.pdv.relatorio;

import br.com.pdv.dao.VendaDAO;
import br.com.pdv.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Relatório de Receitas
 * Exibe as receitas do período, agrupadas por dia, semana e mês, com análise de tendências.
 */
public class RelatorioReceitas extends RelatorioBase {
    private final VendaDAO vendaDAO;
    
    /**
     * Construtor do relatório de receitas
     * 
     * @param dataInicio Data inicial do período
     * @param dataFim Data final do período
     */
    public RelatorioReceitas(LocalDateTime dataInicio, LocalDateTime dataFim) {
        super("RELATÓRIO DE RECEITAS", dataInicio, dataFim);
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
            
            // Parte 1: Receitas por dia
            relatorio.append("RECEITAS POR DIA\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            String sql = "SELECT DATE(v.data_venda) as data, " +
                       "COUNT(v.id) as total_vendas, " +
                       "SUM(v.valor_total) as valor_bruto, " +
                       "SUM(v.valor_desconto) as valor_desconto, " +
                       "SUM(v.valor_total - v.valor_desconto) as valor_liquido " +
                       "FROM vendas v " +
                       "WHERE v.data_venda BETWEEN ? AND ? " +
                       "AND v.status = 'FINALIZADA' " +
                       "GROUP BY DATE(v.data_venda) " +
                       "ORDER BY data";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-12s %-12s %-15s %-15s %-15s %-15s\n", 
                                         "DATA", "DIA", "QTD VENDAS", "VALOR BRUTO", "DESCONTOS", "VALOR LÍQUIDO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal totalBruto = BigDecimal.ZERO;
            BigDecimal totalDescontos = BigDecimal.ZERO;
            BigDecimal totalLiquido = BigDecimal.ZERO;
            int totalVendas = 0;
            
            // Armazenar dados para análises adicionais
            Map<LocalDate, BigDecimal> valoresPorDia = new HashMap<>();
            
            while (rs.next()) {
                LocalDate data = rs.getDate("data").toLocalDate();
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorBruto = rs.getBigDecimal("valor_bruto");
                BigDecimal valorDesconto = rs.getBigDecimal("valor_desconto");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                // Obter o dia da semana
                DayOfWeek diaSemana = data.getDayOfWeek();
                String nomeDiaSemana = diaSemana.getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
                
                relatorio.append(String.format("%-12s %-12s %-15d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                             data.format(dateFormatter),
                                             nomeDiaSemana,
                                             qtdVendas,
                                             valorBruto,
                                             valorDesconto,
                                             valorLiquido));
                
                totalBruto = totalBruto.add(valorBruto);
                totalDescontos = totalDescontos.add(valorDesconto);
                totalLiquido = totalLiquido.add(valorLiquido);
                totalVendas += qtdVendas;
                
                valoresPorDia.put(data, valorLiquido);
            }
            
            relatorio.append("--------------------------------------------------------------------------------\n");
            relatorio.append(String.format("%-25s %-15d R$ %-12.2f R$ %-12.2f R$ %-12.2f\n", 
                                         "TOTAL",
                                         totalVendas,
                                         totalBruto,
                                         totalDescontos,
                                         totalLiquido));
            
            // Calcular média diária
            long diasNoPeriodo = ChronoUnit.DAYS.between(dataInicio.toLocalDate(), dataFim.toLocalDate()) + 1;
            if (diasNoPeriodo > 0) {
                BigDecimal mediaDiaria = totalLiquido.divide(new BigDecimal(diasNoPeriodo), 2, RoundingMode.HALF_UP);
                relatorio.append(String.format("MÉDIA DIÁRIA: R$ %.2f\n", mediaDiaria));
            }
            
            // Parte 2: Receitas por forma de pagamento
            relatorio.append("\nRECEITAS POR FORMA DE PAGAMENTO\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT forma_pagamento, " +
                 "COUNT(id) as total_vendas, " +
                 "SUM(valor_total) as valor_bruto, " +
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
            
            relatorio.append(String.format("%-20s %-15s %-15s %-15s %-15s\n", 
                                         "FORMA PAGAMENTO", "QTD VENDAS", "VALOR BRUTO", "DESCONTOS", "VALOR LÍQUIDO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            while (rs.next()) {
                String formaPagamento = rs.getString("forma_pagamento");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorBruto = rs.getBigDecimal("valor_bruto");
                BigDecimal valorDesconto = rs.getBigDecimal("valor_desconto");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                // Calcular percentual do total
                double percentual = totalLiquido.compareTo(BigDecimal.ZERO) > 0 ? 
                        valorLiquido.multiply(new BigDecimal(100)).divide(totalLiquido, 2, RoundingMode.HALF_UP).doubleValue() : 0;
                
                relatorio.append(String.format("%-20s %-15d R$ %-12.2f R$ %-12.2f R$ %-12.2f (%.2f%%)\n", 
                                             formaPagamento,
                                             qtdVendas,
                                             valorBruto,
                                             valorDesconto,
                                             valorLiquido,
                                             percentual));
            }
            
            // Parte 3: Receitas por dia da semana
            relatorio.append("\nRECEITAS POR DIA DA SEMANA\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT DAYOFWEEK(data_venda) as dia_semana, " +
                 "COUNT(id) as total_vendas, " +
                 "SUM(valor_total - valor_desconto) as valor_liquido " +
                 "FROM vendas " +
                 "WHERE data_venda BETWEEN ? AND ? " +
                 "AND status = 'FINALIZADA' " +
                 "GROUP BY DAYOFWEEK(data_venda) " +
                 "ORDER BY dia_semana";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-15s %-15s %-15s %-15s\n", 
                                         "DIA SEMANA", "QTD VENDAS", "VALOR TOTAL", "MÉDIA P/DIA"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Contador de ocorrências de cada dia da semana no período
            Map<Integer, Integer> ocorrenciasDiaSemana = new HashMap<>();
            
            // Contar ocorrências de cada dia da semana no período
            LocalDate dataAtual = dataInicio.toLocalDate();
            while (!dataAtual.isAfter(dataFim.toLocalDate())) {
                int diaSemana = dataAtual.getDayOfWeek().getValue() + 1; // Ajuste para o formato DAYOFWEEK do MySQL (1=DOM, 7=SAB)
                if (diaSemana == 8) diaSemana = 1; // Ajuste adicional para domingo
                
                ocorrenciasDiaSemana.put(diaSemana, ocorrenciasDiaSemana.getOrDefault(diaSemana, 0) + 1);
                dataAtual = dataAtual.plusDays(1);
            }
            
            // Mapeamento de dias da semana
            Map<Integer, String> diasSemana = new HashMap<>();
            diasSemana.put(1, "Domingo");
            diasSemana.put(2, "Segunda-feira");
            diasSemana.put(3, "Terça-feira");
            diasSemana.put(4, "Quarta-feira");
            diasSemana.put(5, "Quinta-feira");
            diasSemana.put(6, "Sexta-feira");
            diasSemana.put(7, "Sábado");
            
            while (rs.next()) {
                int diaSemana = rs.getInt("dia_semana");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                String nomeDiaSemana = diasSemana.getOrDefault(diaSemana, "Dia " + diaSemana);
                
                // Calcular média considerando o número de ocorrências deste dia no período
                int ocorrencias = ocorrenciasDiaSemana.getOrDefault(diaSemana, 1);
                BigDecimal mediaPorDia = valorLiquido.divide(new BigDecimal(ocorrencias), 2, RoundingMode.HALF_UP);
                
                relatorio.append(String.format("%-15s %-15d R$ %-12.2f R$ %-12.2f\n", 
                                             nomeDiaSemana,
                                             qtdVendas,
                                             valorLiquido,
                                             mediaPorDia));
            }
            
            // Parte 4: Evolução mensal das receitas
            relatorio.append("\nEVOLUÇÃO MENSAL DAS RECEITAS\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            sql = "SELECT YEAR(data_venda) as ano, MONTH(data_venda) as mes, " +
                 "COUNT(id) as total_vendas, " +
                 "SUM(valor_total - valor_desconto) as valor_liquido " +
                 "FROM vendas " +
                 "WHERE data_venda BETWEEN DATE_SUB(?, INTERVAL 12 MONTH) AND ? " +
                 "AND status = 'FINALIZADA' " +
                 "GROUP BY YEAR(data_venda), MONTH(data_venda) " +
                 "ORDER BY ano, mes";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            
            rs = stmt.executeQuery();
            
            relatorio.append(String.format("%-10s %-15s %-15s %-15s\n", 
                                         "MÊS/ANO", "QTD VENDAS", "VALOR TOTAL", "VARIAÇÃO"));
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            BigDecimal valorMesAnterior = null;
            BigDecimal valorPrimeiroMes = null;
            
            while (rs.next()) {
                int ano = rs.getInt("ano");
                int mes = rs.getInt("mes");
                int qtdVendas = rs.getInt("total_vendas");
                BigDecimal valorLiquido = rs.getBigDecimal("valor_liquido");
                
                // Formatar mês/ano
                LocalDate dataMes = LocalDate.of(ano, mes, 1);
                String mesAnoFormatado = dataMes.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                
                // Calcular variação percentual em relação ao mês anterior
                String variacaoStr = "—";
                if (valorMesAnterior != null && valorMesAnterior.compareTo(BigDecimal.ZERO) > 0) {
                    double variacao = valorLiquido.subtract(valorMesAnterior)
                                     .multiply(new BigDecimal(100))
                                     .divide(valorMesAnterior, 2, RoundingMode.HALF_UP)
                                     .doubleValue();
                    
                    String sinal = variacao >= 0 ? "+" : "";
                    variacaoStr = String.format("%s%.2f%%", sinal, variacao);
                }
                
                relatorio.append(String.format("%-10s %-15d R$ %-12.2f %s\n", 
                                             mesAnoFormatado,
                                             qtdVendas,
                                             valorLiquido,
                                             variacaoStr));
                
                // Armazenar para próxima iteração
                valorMesAnterior = valorLiquido;
                
                // Armazenar valor do primeiro mês para cálculo da variação total
                if (valorPrimeiroMes == null) {
                    valorPrimeiroMes = valorLiquido;
                }
            }
            
            // Calcular variação total entre o primeiro e último mês
            if (valorPrimeiroMes != null && valorMesAnterior != null && valorPrimeiroMes.compareTo(BigDecimal.ZERO) > 0) {
                double variacaoTotal = valorMesAnterior.subtract(valorPrimeiroMes)
                                     .multiply(new BigDecimal(100))
                                     .divide(valorPrimeiroMes, 2, RoundingMode.HALF_UP)
                                     .doubleValue();
                
                String sinal = variacaoTotal >= 0 ? "+" : "";
                relatorio.append(String.format("\nVariação total no período: %s%.2f%%\n", sinal, variacaoTotal));
            }
            
            // Parte 5: Análise de tendências
            relatorio.append("\nANÁLISE DE TENDÊNCIAS\n");
            relatorio.append("--------------------------------------------------------------------------------\n");
            
            // Verificar tendência comparando períodos
            LocalDateTime dataInicioAnterior = dataInicio.minusDays(diasNoPeriodo);
            
            sql = "SELECT " +
                 "SUM(CASE WHEN data_venda BETWEEN ? AND ? THEN valor_total - valor_desconto ELSE 0 END) as valor_periodo_atual, " +
                 "SUM(CASE WHEN data_venda BETWEEN ? AND ? THEN valor_total - valor_desconto ELSE 0 END) as valor_periodo_anterior, " +
                 "COUNT(CASE WHEN data_venda BETWEEN ? AND ? THEN 1 ELSE NULL END) as vendas_periodo_atual, " +
                 "COUNT(CASE WHEN data_venda BETWEEN ? AND ? THEN 1 ELSE NULL END) as vendas_periodo_anterior " +
                 "FROM vendas " +
                 "WHERE data_venda BETWEEN ? AND ? " +
                 "AND status = 'FINALIZADA'";
            
            stmt = conn.prepareStatement(sql);
            stmt.setObject(1, dataInicio);
            stmt.setObject(2, dataFim);
            stmt.setObject(3, dataInicioAnterior);
            stmt.setObject(4, dataInicio.minusDays(1));
            stmt.setObject(5, dataInicio);
            stmt.setObject(6, dataFim);
            stmt.setObject(7, dataInicioAnterior);
            stmt.setObject(8, dataInicio.minusDays(1));
            stmt.setObject(9, dataInicioAnterior);
            stmt.setObject(10, dataFim);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                BigDecimal valorPeriodoAtual = rs.getBigDecimal("valor_periodo_atual");
                BigDecimal valorPeriodoAnterior = rs.getBigDecimal("valor_periodo_anterior");
                int vendasPeriodoAtual = rs.getInt("vendas_periodo_atual");
                int vendasPeriodoAnterior = rs.getInt("vendas_periodo_anterior");
                
                // Calcular variação
                if (valorPeriodoAnterior != null && valorPeriodoAnterior.compareTo(BigDecimal.ZERO) > 0) {
                    double variacaoValor = valorPeriodoAtual.subtract(valorPeriodoAnterior)
                                          .multiply(new BigDecimal(100))
                                          .divide(valorPeriodoAnterior, 2, RoundingMode.HALF_UP)
                                          .doubleValue();
                    
                    double variacaoVendas = vendasPeriodoAnterior > 0 ? 
                                          (double)(vendasPeriodoAtual - vendasPeriodoAnterior) * 100 / vendasPeriodoAnterior : 0;
                    
                    relatorio.append(String.format("Período atual (%s a %s):\n", 
                                                 dataInicio.format(dateFormatter), 
                                                 dataFim.format(dateFormatter)));
                    relatorio.append(String.format("  Valor total: R$ %.2f (%d vendas)\n", valorPeriodoAtual, vendasPeriodoAtual));
                    
                    relatorio.append(String.format("Período anterior (%s a %s):\n", 
                                                 dataInicioAnterior.format(dateFormatter), 
                                                 dataInicio.minusDays(1).format(dateFormatter)));
                    relatorio.append(String.format("  Valor total: R$ %.2f (%d vendas)\n", valorPeriodoAnterior, vendasPeriodoAnterior));
                    
                    String tendenciaValor = variacaoValor > 0 ? "↗️ CRESCIMENTO" : (variacaoValor < 0 ? "↘️ QUEDA" : "➡️ ESTÁVEL");
                    relatorio.append(String.format("Variação do valor: %s%.2f%% (%s)\n", 
                                                 variacaoValor >= 0 ? "+" : "",
                                                 variacaoValor,
                                                 tendenciaValor));
                    
                    String tendenciaVendas = variacaoVendas > 0 ? "↗️ CRESCIMENTO" : (variacaoVendas < 0 ? "↘️ QUEDA" : "➡️ ESTÁVEL");
                    relatorio.append(String.format("Variação de vendas: %s%.2f%% (%s)\n", 
                                                 variacaoVendas >= 0 ? "+" : "",
                                                 variacaoVendas,
                                                 tendenciaVendas));
                }
            }
            
        } catch (SQLException e) {
            LogUtil.error(this.getClass(), "Erro ao gerar relatório de receitas", e);
            throw e;
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return relatorio.toString();
    }
}