package br.com.pdv.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import javafx.scene.control.TextField;

/**
 * Classe utilitária para formatação de valores.
 */
public class FormatUtil {

    private static final DecimalFormat DECIMAL_FORMAT;
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final SimpleDateFormat DATE_FORMAT;
    private static final SimpleDateFormat DATETIME_FORMAT;
    private static final DateTimeFormatter LOCAL_DATE_FORMATTER;
    private static final DateTimeFormatter LOCAL_DATETIME_FORMATTER;
    
    static {
        // Configuração para o locale brasileiro
        Locale localeBR = new Locale("pt", "BR");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(localeBR);
        
        // Formatadores de número
        DECIMAL_FORMAT = new DecimalFormat("#,##0.00", symbols);
        CURRENCY_FORMAT = new DecimalFormat("¤ #,##0.00", symbols);
        
        // Formatadores de data
        DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", localeBR);
        DATETIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeBR);
        
        // Formatadores para as classes do Java 8+
        LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LOCAL_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    }
    
    /**
     * Formata um valor BigDecimal para exibição com duas casas decimais.
     * 
     * @param valor O valor a ser formatado
     * @return O valor formatado como String
     * 
     */
    /*
    public static String formatarValor(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        return DECIMAL_FORMAT.format(valor);
    }
    */
    /**
     * Formata um valor BigDecimal para exibição como moeda.
     * 
     * @param valor O valor a ser formatado
     * @return O valor formatado como String com símbolo de moeda
     */
    
    
    public static String formatarMoeda(BigDecimal valor) {
        if (valor == null) {
            return "R$ 0,00";
        }
        return CURRENCY_FORMAT.format(valor);
    }
    
    /**
     * Converte uma String formatada para BigDecimal.
     * 
     * @param valor A String a ser convertida
     * @return O valor como BigDecimal
     * @throws ParseException Se ocorrer erro na conversão
     */
    public static BigDecimal parseBigDecimal(String valor) throws ParseException {
        if (valor == null || valor.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Remove símbolos de moeda e espaços
        valor = valor.replace("R$", "").replace(" ", "");
        
        // Substitui vírgula por ponto para conversão correta
        valor = valor.replace(".", "").replace(",", ".");
        
        return new BigDecimal(valor);
    }
    
    /**
     * Formata um Date para exibição no formato dd/MM/yyyy.
     * 
     * @param data A data a ser formatada
     * @return A data formatada como String
     */
    public static String formatarData(Date data) {
        if (data == null) {
            return "";
        }
        return DATE_FORMAT.format(data);
    }
    
    /**
     * Formata um Date para exibição no formato dd/MM/yyyy HH:mm:ss.
     * 
     * @param data A data e hora a ser formatada
     * @return A data e hora formatada como String
     */
    public static String formatarDataHora(Date data) {
        if (data == null) {
            return "";
        }
        return DATETIME_FORMAT.format(data);
    }
    
    /**
     * Formata um LocalDate para exibição no formato dd/MM/yyyy.
     * 
     * @param data A data a ser formatada
     * @return A data formatada como String
     */
    public static String formatarData(LocalDate data) {
        if (data == null) {
            return "";
        }
        return data.format(LOCAL_DATE_FORMATTER);
    }
    
    /**
     * Formata um LocalDateTime para exibição no formato dd/MM/yyyy HH:mm:ss.
     * 
     * @param dataHora A data e hora a ser formatada
     * @return A data e hora formatada como String
     */
    public static String formatarDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            return "";
        }
        return dataHora.format(LOCAL_DATETIME_FORMATTER);
    }
    
    /**
     * Formata um número inteiro como CPF (XXX.XXX.XXX-XX).
     * 
     * @param cpf O número do CPF (apenas dígitos)
     * @return O CPF formatado
     */
    public static String formatarCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        
        return cpf.substring(0, 3) + "." + 
               cpf.substring(3, 6) + "." + 
               cpf.substring(6, 9) + "-" + 
               cpf.substring(9);
    }
    
    /**
     * Formata um número inteiro como CNPJ (XX.XXX.XXX/XXXX-XX).
     * 
     * @param cnpj O número do CNPJ (apenas dígitos)
     * @return O CNPJ formatado
     */
    public static String formatarCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        
        return cnpj.substring(0, 2) + "." + 
               cnpj.substring(2, 5) + "." + 
               cnpj.substring(5, 8) + "/" + 
               cnpj.substring(8, 12) + "-" + 
               cnpj.substring(12);
    }
    
    /**
     * Formata um número como CPF ou CNPJ, dependendo do tamanho.
     * 
     * @param cpfCnpj O número do CPF ou CNPJ (apenas dígitos)
     * @return O CPF ou CNPJ formatado
     */
    public static String formatarCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null) {
            return "";
        }
        
        // Remove todos os caracteres não numéricos
        String numeros = cpfCnpj.replaceAll("\\D", "");
        
        if (numeros.length() == 11) {
            return formatarCPF(numeros);
        } else if (numeros.length() == 14) {
            return formatarCNPJ(numeros);
        }
        
        return cpfCnpj;
    }
    
    /**
     * Formata um número como telefone (XX) XXXXX-XXXX.
     * 
     * @param telefone O número de telefone (apenas dígitos)
     * @return O telefone formatado
     */
    public static String formatarTelefone(String telefone) {
        if (telefone == null) {
            return "";
        }
        
        // Remove todos os caracteres não numéricos
        String numeros = telefone.replaceAll("\\D", "");
        
        if (numeros.length() == 10) {
            // Telefone fixo: (XX) XXXX-XXXX
            return "(" + numeros.substring(0, 2) + ") " + 
                   numeros.substring(2, 6) + "-" + 
                   numeros.substring(6);
        } else if (numeros.length() == 11) {
            // Celular: (XX) XXXXX-XXXX
            return "(" + numeros.substring(0, 2) + ") " + 
                   numeros.substring(2, 7) + "-" + 
                   numeros.substring(7);
        }
        
        return telefone;
    }
    
    /**
     * Formata um número como CEP (XXXXX-XXX).
     * 
     * @param cep O número do CEP (apenas dígitos)
     * @return O CEP formatado
     */
    public static String formatarCEP(String cep) {
        if (cep == null || cep.length() != 8) {
            return cep;
        }
        
        return cep.substring(0, 5) + "-" + cep.substring(5);
    }
    
    
    /**
     * Formata um valor monetário para exibição
     * @param valor O valor a ser formatado
     * @return String formatada (ex: "R$ 1.234,56")
     */
    public static String formatarValor(BigDecimal valor) {
        if (valor == null) {
            return "0,00";
        }
        
        DecimalFormat df = new DecimalFormat("0.00");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(new Locale("pt", "BR")));
        return df.format(valor);
    }
    
    /**
     * Formata um valor percentual para exibição
     * @param valor O valor a ser formatado
     * @return String formatada (ex: "12,34%")
     */
    public static String formatarPercentual(BigDecimal valor) {
        if (valor == null) {
            return "0,00%";
        }
        
        DecimalFormat df = new DecimalFormat("0.00'%'");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(new Locale("pt", "BR")));
        return df.format(valor);
    }
    
    /**
     * Configura um TextField para aceitar apenas valores monetários
     * @param textField O campo a ser configurado
     */
    public static void formatTextFieldAsCurrency(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                return;
            }
            
            // Remover caracteres não numéricos, exceto vírgula
            String value = newValue.replaceAll("[^0-9,]", "");
            
            // Garantir que há apenas uma vírgula
            int commaCount = value.length() - value.replace(",", "").length();
            if (commaCount > 1) {
                int firstCommaIndex = value.indexOf(",");
                value = value.substring(0, firstCommaIndex + 1) + value.substring(firstCommaIndex + 1).replace(",", "");
            }
            
            // Limitar a 2 casas decimais após a vírgula
            if (value.contains(",")) {
                String[] parts = value.split(",");
                if (parts.length > 1 && parts[1].length() > 2) {
                    value = parts[0] + "," + parts[1].substring(0, 2);
                }
            }
            
            // Atualizar o valor se foi modificado
            if (!value.equals(newValue)) {
                textField.setText(value);
            }
        });
    }
    
    /**
     * Configura um TextField para aceitar apenas valores percentuais
     * @param textField O campo a ser configurado
     */
    public static void formatTextFieldAsPercentage(TextField textField) {
        formatTextFieldAsCurrency(textField); // Mesma lógica para valores decimais
    }
    
    /**
     * Configura um TextField para aceitar apenas valores inteiros
     * @param textField O campo a ser configurado
     */
    public static void formatTextFieldAsInteger(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}