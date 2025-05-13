package br.com.pdv.exemplos;

import br.com.pdv.relatorio.*;
import br.com.pdv.relatorio.RelatorioManager.TipoRelatorio;
import br.com.pdv.relatorio.RelatorioManager.RelatorioPDVException;
import br.com.pdv.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Exemplo de como gerar relatórios em formato PDF
 */
public class ExemploRelatoriosPdf {

    // Diretório onde os relatórios PDF serão salvos
    private static final String DIRETORIO_RELATORIOS = "./relatorios/";
    
    public static void main(String[] args) {
        try {
            // Cria o diretório de relatórios se não existir
            criarDiretorioRelatorios();
            
            // Obtém a instância do gerenciador de relatórios
            RelatorioManager relatorioManager = RelatorioManager.getInstance();
            
            // Define o período para os relatórios (último mês)
            LocalDateTime dataInicio = LocalDateTime.now().minusDays(30);
            LocalDateTime dataFim = LocalDateTime.now();
            
            System.out.println("===== GERANDO RELATÓRIOS EM PDF =====");
            
            try {
                // Exemplo 1: Gerar relatório de vendas totais em PDF
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.VENDAS_TOTAIS, dataInicio, dataFim);
                
                // Exemplo 2: Gerar relatório de nível de estoque em PDF (não precisa de período)
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.NIVEL_ESTOQUE, null, null);
                
                // Exemplo 3: Gerar relatório financeiro por formas de pagamento em PDF
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.FINANCEIRO_FORMAS_PAGAMENTO, dataInicio, dataFim);
                
                // Exemplo 4: Gerar relatório de comissão de vendedores em PDF
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.COMISSAO_VENDEDORES, dataInicio, dataFim);
                
                // Exemplo 5: Gerar relatório de crédito por período em PDF
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.CREDITO_POR_PERIODO, dataInicio, dataFim);
                
                // Exemplo 6: Gerar relatório de crédito por cliente em PDF
                gerarRelatorioPdf(relatorioManager, TipoRelatorio.CREDITO_POR_CLIENTE, dataInicio, dataFim);
            } catch (RelatorioPDVException e) {
                System.err.println("Erro ao gerar relatório: " + e.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar relatório", e);
            }
            
            System.out.println("===== FIM DOS RELATÓRIOS EM PDF =====");
            
        } catch (Exception e) {
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao executar exemplos de relatórios em PDF", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Gera um relatório em formato PDF e salva em arquivo
     * 
     * @param relatorioManager O gerenciador de relatórios
     * @param tipoRelatorio O tipo de relatório a ser gerado
     * @param dataInicio A data de início do período (pode ser null para relatórios sem período)
     * @param dataFim A data de fim do período (pode ser null para relatórios sem período)
     */
    private static void gerarRelatorioPdf(RelatorioManager relatorioManager, 
                                         TipoRelatorio tipoRelatorio,
                                         LocalDateTime dataInicio,
                                         LocalDateTime dataFim) throws RelatorioPDVException {
        try {
            System.out.println("Gerando relatório PDF: " + tipoRelatorio.getDescricao());
            
            // Cria o relatório
            RelatorioBase relatorio = relatorioManager.criarRelatorio(tipoRelatorio, dataInicio, dataFim);
            
            // Gera o conteúdo do relatório em PDF
            byte[] pdfBytes = relatorio.gerarRelatorioPDF();
            
            // Define o nome do arquivo com base no tipo de relatório e data atual
            String nomeArquivo = gerarNomeArquivo(tipoRelatorio);
            
            // Salva o PDF em arquivo
            salvarPdfEmArquivo(pdfBytes, nomeArquivo);
            
            System.out.println("Relatório PDF gerado com sucesso: " + nomeArquivo);
            
        } catch (RelatorioPDVException e) {
            System.err.println("Erro ao criar relatório: " + e.getMessage());
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao criar relatório", e);
        } catch (UnsupportedOperationException e) {
            // Implementação alternativa caso o método gerarRelatorioPDF não esteja implementado
            try {
                System.out.println("Método PDF não implementado para " + tipoRelatorio.getDescricao() + 
                                 ". Gerando PDF a partir do texto...");
                
                // Cria o relatório
                RelatorioBase relatorio = relatorioManager.criarRelatorio(tipoRelatorio, dataInicio, dataFim);
                
                // Gera o conteúdo do relatório em texto
                String conteudoTexto = relatorio.gerarRelatorio();
                
                // Converte o texto para PDF usando uma implementação customizada
                byte[] pdfBytes = converterTextoPdf(conteudoTexto, tipoRelatorio.getDescricao());
                
                // Define o nome do arquivo com base no tipo de relatório e data atual
                String nomeArquivo = gerarNomeArquivo(tipoRelatorio);
                
                // Salva o PDF em arquivo
                salvarPdfEmArquivo(pdfBytes, nomeArquivo);
                
                System.out.println("Relatório PDF gerado com sucesso (método alternativo): " + nomeArquivo);
                
            } catch (SQLException | IOException e2) {
                System.err.println("Erro ao gerar relatório em texto e converter para PDF: " + e2.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar relatório em texto", e2);
            }
        } catch (Exception e) {
            System.err.println("Erro ao gerar relatório PDF: " + e.getMessage());
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar relatório PDF", e);
        }
    }
    
    /**
     * Método para converter o conteúdo de texto em PDF
     * Esta é uma implementação alternativa caso o método gerarRelatorioPDF da classe RelatorioBase
     * não esteja implementado
     * 
     * @param texto O conteúdo do relatório em formato de texto
     * @param titulo O título do relatório
     * @return Array de bytes contendo o PDF gerado
     * @throws IOException Se ocorrer erro na criação do PDF
     */
    private static byte[] converterTextoPdf(String texto, String titulo) throws IOException {
        // Aqui seria implementada a conversão de texto para PDF
        // Usando uma biblioteca como iText ou Apache PDFBox
        
        // Exemplo com iText (precisa adicionar a dependência no projeto):
        /*
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            
            document.open();
            document.addTitle(titulo);
            document.add(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("\n"));
            
            // Adiciona o texto com fonte monoespaçada para preservar a formatação
            Font fonteCourier = FontFactory.getFont(FontFactory.COURIER, 9);
            document.add(new Paragraph(texto, fonteCourier));
            
            document.close();
            
            return baos.toByteArray();
        }
        */
        
        // Como não temos a biblioteca iText no projeto, vamos apenas simular o retorno
        // Em uma implementação real, o código acima seria usado
        System.out.println("Simulando conversão de texto para PDF para: " + titulo);
        return ("PDF simulado para: " + titulo + "\n\n" + texto).getBytes();
    }
    
    /**
     * Cria o diretório de relatórios se não existir
     */
    private static void criarDiretorioRelatorios() {
        File diretorio = new File(DIRETORIO_RELATORIOS);
        if (!diretorio.exists()) {
            if (diretorio.mkdirs()) {
                System.out.println("Diretório de relatórios criado: " + diretorio.getAbsolutePath());
            } else {
                System.err.println("Não foi possível criar o diretório de relatórios: " + diretorio.getAbsolutePath());
            }
        }
    }
    
    /**
     * Gera um nome de arquivo para o relatório baseado no tipo e data atual
     * 
     * @param tipoRelatorio O tipo de relatório
     * @return O nome do arquivo gerado
     */
    private static String gerarNomeArquivo(TipoRelatorio tipoRelatorio) {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeRelatorio = tipoRelatorio.name().toLowerCase();
        
        return DIRETORIO_RELATORIOS + nomeRelatorio + "_" + dataHora + ".pdf";
    }
    
    /**
     * Salva o conteúdo do PDF em um arquivo
     * 
     * @param conteudoPdf O conteúdo do PDF em array de bytes
     * @param nomeArquivo O nome do arquivo onde o PDF será salvo
     * @throws IOException Se ocorrer erro ao salvar o arquivo
     */
    private static void salvarPdfEmArquivo(byte[] conteudoPdf, String nomeArquivo) throws IOException {
        Path path = Paths.get(nomeArquivo);
        
        // Cria os diretórios pai se necessário
        Files.createDirectories(path.getParent());
        
        // Escreve o conteúdo no arquivo
        try (FileOutputStream fos = new FileOutputStream(nomeArquivo)) {
            fos.write(conteudoPdf);
            fos.flush();
        }
        
        System.out.println("PDF salvo em: " + path.toAbsolutePath());
    }
}