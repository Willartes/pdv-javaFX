package br.com.pdv.exemplos;

import com.itextpdf.text.DocumentException;
import br.com.pdv.relatorio.*;
import br.com.pdv.relatorio.RelatorioManager.TipoRelatorio;
import br.com.pdv.relatorio.RelatorioManager.RelatorioPDVException;
import br.com.pdv.util.LogUtil;
import br.com.pdv.util.PDFUtil;

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
    
    // Verifica se a biblioteca iText está disponível
    private static boolean iTextDisponivel = false;
    
    static {
        try {
            // Tentar carregar algumas classes do iText
            Class.forName("com.itextpdf.text.Document");
            Class.forName("com.itextpdf.text.pdf.PdfWriter");
            iTextDisponivel = true;
        } catch (ClassNotFoundException e) {
            System.err.println("ATENÇÃO: Biblioteca iText não encontrada!");
            System.err.println("Para gerar PDFs, você precisa adicionar a biblioteca iText ao seu projeto.");
            System.err.println("Para Maven, adicione a seguinte dependência ao seu pom.xml:");
            System.err.println("<dependency>");
            System.err.println("    <groupId>com.itextpdf</groupId>");
            System.err.println("    <artifactId>itextpdf</artifactId>");
            System.err.println("    <version>5.5.13.3</version>");
            System.err.println("</dependency>");
            System.err.println("\nAlternativamente, baixe o JAR em https://repo1.maven.org/maven2/com/itextpdf/itextpdf/5.5.13.3/itextpdf-5.5.13.3.jar");
            System.err.println("e adicione-o ao classpath do seu projeto.");
            
            // Se a biblioteca não está disponível, usaremos a versão básica
            System.err.println("\nUsando versão básica de geração de relatórios (arquivo de texto formatado)...");
            iTextDisponivel = false;
        }
    }
    
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
     * @throws RelatorioPDVException Se ocorrer erro ao criar o relatório
     */
    private static void gerarRelatorioPdf(RelatorioManager relatorioManager, 
                                         TipoRelatorio tipoRelatorio,
                                         LocalDateTime dataInicio,
                                         LocalDateTime dataFim) throws RelatorioPDVException {
        try {
            System.out.println("Gerando relatório PDF: " + tipoRelatorio.getDescricao());
            
            // Cria o relatório
            RelatorioBase relatorio = relatorioManager.criarRelatorio(tipoRelatorio, dataInicio, dataFim);
            
            try {
                // Tenta gerar o relatório em PDF
                byte[] pdfBytes = relatorio.gerarRelatorioPDF();
                
                // Define o nome do arquivo com base no tipo de relatório e data atual
                String nomeArquivo = gerarNomeArquivo(tipoRelatorio, "pdf");
                
                try {
                    // Salva o PDF em arquivo
                    salvarArquivo(pdfBytes, nomeArquivo);
                    System.out.println("Relatório PDF gerado com sucesso: " + nomeArquivo);
                } catch (IOException e) {
                    System.err.println("Erro ao salvar arquivo PDF: " + e.getMessage());
                    LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao salvar arquivo PDF", e);
                }
            } catch (UnsupportedOperationException e) {
                System.out.println("Método PDF não implementado para " + tipoRelatorio.getDescricao());
                gerarRelatorioTexto(relatorio, tipoRelatorio);
            } catch (SQLException e) {
                System.err.println("Erro ao gerar relatório PDF: " + e.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar relatório PDF", e);
                gerarRelatorioTexto(relatorio, tipoRelatorio);
            } catch (DocumentException e) {
                System.err.println("Erro ao gerar documento PDF: " + e.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar documento PDF", e);
                gerarRelatorioTexto(relatorio, tipoRelatorio);
            } catch (IOException e) {
                System.err.println("Erro de I/O ao gerar PDF: " + e.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro de I/O ao gerar PDF", e);
                gerarRelatorioTexto(relatorio, tipoRelatorio);
            }
        } catch (RelatorioPDVException e) {
            System.err.println("Erro ao criar relatório: " + e.getMessage());
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao criar relatório", e);
            throw e;
        } catch (Exception e) {
            System.err.println("Erro inesperado ao gerar relatório: " + e.getMessage());
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro inesperado ao gerar relatório", e);
        }
    }
    
    /**
     * Método auxiliar para gerar relatório em formato texto
     */
    private static void gerarRelatorioTexto(RelatorioBase relatorio, TipoRelatorio tipoRelatorio) {
        try {
            System.out.println("Tentando gerar relatório em formato texto...");
            String conteudoTexto = relatorio.gerarRelatorio();
            byte[] textoBytes = gerarTextoFormatado(conteudoTexto, tipoRelatorio.getDescricao());
            
            // Define o nome do arquivo com base no tipo de relatório e data atual
            String nomeArquivo = gerarNomeArquivo(tipoRelatorio, "txt");
            
            try {
                // Salva o texto em arquivo
                salvarArquivo(textoBytes, nomeArquivo);
                System.out.println("Relatório em texto gerado com sucesso: " + nomeArquivo);
            } catch (IOException e) {
                System.err.println("Erro ao salvar arquivo de texto: " + e.getMessage());
                LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao salvar arquivo de texto", e);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao gerar relatório em texto: " + e.getMessage());
            LogUtil.error(ExemploRelatoriosPdf.class, "Erro ao gerar relatório em texto", e);
        }
    }
    
    /**
     * Gera um documento de texto formatado (fallback quando iText não está disponível)
     */
    private static byte[] gerarTextoFormatado(String texto, String titulo) {
        StringBuilder sb = new StringBuilder();
        
        // Adicionar cabeçalho formatado
        sb.append("=====================================================\n");
        sb.append("                 ").append(titulo).append("\n");
        sb.append("=====================================================\n\n");
        
        // Adicionar data/hora
        sb.append("Gerado em: ").append(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .append("\n\n");
        
        // Adicionar o conteúdo do relatório
        sb.append(texto);
        
        // Adicionar rodapé
        sb.append("\n\n");
        sb.append("=====================================================\n");
        sb.append("            SISTEMA PDV - Relatório Gerado           \n");
        sb.append("=====================================================\n");
        
        return sb.toString().getBytes();
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
     * @param extensao A extensão do arquivo
     * @return O nome do arquivo gerado
     */
    private static String gerarNomeArquivo(TipoRelatorio tipoRelatorio, String extensao) {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nomeRelatorio = tipoRelatorio.name().toLowerCase();
        
        return DIRETORIO_RELATORIOS + nomeRelatorio + "_" + dataHora + "." + extensao;
    }
    
    /**
     * Salva o conteúdo do documento em um arquivo
     * 
     * @param conteudo O conteúdo do documento em array de bytes
     * @param nomeArquivo O nome do arquivo onde será salvo
     * @throws IOException Se ocorrer erro ao salvar o arquivo
     */
    private static void salvarArquivo(byte[] conteudo, String nomeArquivo) throws IOException {
        Path path = Paths.get(nomeArquivo);
        
        // Cria os diretórios pai se necessário
        Files.createDirectories(path.getParent());
        
        // Salvar o arquivo
        try (FileOutputStream fos = new FileOutputStream(nomeArquivo)) {
            fos.write(conteudo);
            fos.flush();
        }
        
        System.out.println("Arquivo salvo em: " + path.toAbsolutePath());
    }
}