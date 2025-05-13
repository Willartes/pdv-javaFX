package br.com.pdv.exemplos;

import br.com.pdv.relatorio.*;
import br.com.pdv.relatorio.RelatorioManager.TipoRelatorio;
import br.com.pdv.util.LogUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Exemplo de como utilizar o sistema de relatórios
 */
public class ExemploUsoRelatorios {

    public static void main(String[] args) {
        try {
            // Obtém a instância do gerenciador de relatórios
            RelatorioManager relatorioManager = RelatorioManager.getInstance();
            
            // Define o período para os relatórios (último mês)
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(30);
            LocalDateTime dataFim = LocalDateTime.now();
            
            System.out.println("===== GERANDO RELATÓRIOS =====");
            
            // Exemplo 1: Gerar relatório de vendas totais
            try {
                RelatorioBase relatorioVendas = relatorioManager.criarRelatorio(
                        TipoRelatorio.VENDAS_TOTAIS, dataInicio, dataFim);
                
                String conteudoRelatorio = relatorioVendas.gerarRelatorio();
                System.out.println(conteudoRelatorio);
                
                // Também seria possível salvar em arquivo:
                // salvarRelatorioEmArquivo(conteudoRelatorio, "vendas_totais.txt");
            } catch (SQLException e) {
                System.err.println("Erro ao gerar relatório de vendas totais: " + e.getMessage());
            }
            
            // Exemplo 2: Gerar relatório de nível de estoque (não precisa de período)
            try {
                RelatorioBase relatorioEstoque = relatorioManager.criarRelatorio(
                        TipoRelatorio.NIVEL_ESTOQUE, null, null);
                
                String conteudoRelatorio = relatorioEstoque.gerarRelatorio();
                System.out.println(conteudoRelatorio);
            } catch (SQLException e) {
                System.err.println("Erro ao gerar relatório de nível de estoque: " + e.getMessage());
            }
            
            // Exemplo 3: Gerar relatório financeiro por formas de pagamento
            try {
                RelatorioBase relatorioFinanceiro = relatorioManager.criarRelatorio(
                        TipoRelatorio.FINANCEIRO_FORMAS_PAGAMENTO, dataInicio, dataFim);
                
                String conteudoRelatorio = relatorioFinanceiro.gerarRelatorio();
                System.out.println(conteudoRelatorio);
            } catch (SQLException e) {
                System.err.println("Erro ao gerar relatório financeiro: " + e.getMessage());
            }
            
            // Exemplo 4: Gerar relatório de comissão de vendedores
            try {
                RelatorioBase relatorioComissao = relatorioManager.criarRelatorio(
                        TipoRelatorio.COMISSAO_VENDEDORES, dataInicio, dataFim);
                
                String conteudoRelatorio = relatorioComissao.gerarRelatorio();
                System.out.println(conteudoRelatorio);
            } catch (SQLException e) {
                System.err.println("Erro ao gerar relatório de comissão: " + e.getMessage());
            }
            
            System.out.println("===== FIM DOS RELATÓRIOS =====");
            
        } catch (Exception e) {
            LogUtil.error(ExemploUsoRelatorios.class, "Erro ao executar exemplos de relatórios", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Método exemplo para salvar o relatório em arquivo
     */
    private static void salvarRelatorioEmArquivo(String conteudo, String nomeArquivo) {
        // Implementação para salvar em arquivo
        // Exemplo utilizando java.nio.file:
        /*
        try {
            Path path = Paths.get("./relatorios/" + nomeArquivo);
            Files.createDirectories(path.getParent());
            Files.writeString(path, conteudo);
            System.out.println("Relatório salvo em: " + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar relatório em arquivo: " + e.getMessage());
        }
        */
    }
}