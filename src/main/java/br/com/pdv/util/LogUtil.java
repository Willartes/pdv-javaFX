package br.com.pdv.util;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Classe utilitária para gerenciamento de logs da aplicação
 */
public class LogUtil {
    private static final String DEFAULT_LOG_PROPERTIES = "src/main/resources/log4j.properties";
    private static Properties logProperties;
    private static boolean configured = false;

    /**
     * Configura o log4j usando arquivo de propriedades
     */
    public static void configureLog() {
        if (!configured) {
            try {
                logProperties = new Properties();
                File propertiesFile = new File(DEFAULT_LOG_PROPERTIES);
                
                if (propertiesFile.exists()) {
                    logProperties.load(new FileInputStream(propertiesFile));
                } else {
                    // Configuração padrão caso não encontre o arquivo
                    setDefaultConfiguration();
                }
                
                PropertyConfigurator.configure(logProperties);
                configured = true;
                
                Logger logger = Logger.getLogger(LogUtil.class);
                logger.info("Log4j configurado com sucesso");
                
            } catch (IOException e) {
                System.err.println("Erro ao configurar log4j: " + e.getMessage());
                e.printStackTrace();
                // Em caso de erro, usa configuração padrão
                setDefaultConfiguration();
            }
        }
    }

    /**
     * Define configurações padrão para o log4j
     */
    private static void setDefaultConfiguration() {
        logProperties = new Properties();
        
        // Configuração do root logger
        logProperties.setProperty("log4j.rootLogger", "DEBUG, stdout, file");
        
        // Configuração para console
        logProperties.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        logProperties.setProperty("log4j.appender.stdout.Target", "System.out");
        logProperties.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        logProperties.setProperty("log4j.appender.stdout.layout.ConversionPattern", 
            "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
        
        // Configuração para arquivo
        logProperties.setProperty("log4j.appender.file", "org.apache.log4j.RollingFileAppender");
        logProperties.setProperty("log4j.appender.file.File", "logs/pdv.log");
        logProperties.setProperty("log4j.appender.file.MaxFileSize", "10MB");
        logProperties.setProperty("log4j.appender.file.MaxBackupIndex", "10");
        logProperties.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
        logProperties.setProperty("log4j.appender.file.layout.ConversionPattern",
            "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n");
            
        PropertyConfigurator.configure(logProperties);
    }

    /**
     * Obtém uma instância do logger para a classe especificada
     * @param clazz Classe que solicita o logger
     * @return Logger configurado
     */
    public static Logger getLogger(Class<?> clazz) {
        if (!configured) {
            configureLog();
        }
        return Logger.getLogger(clazz);
    }

    /**
     * Registra uma mensagem de debug
     * @param clazz Classe que gerou o log
     * @param message Mensagem a ser registrada
     */
    public static void debug(Class<?> clazz, String message) {
        getLogger(clazz).debug(message);
    }

    /**
     * Registra uma mensagem de info
     * @param clazz Classe que gerou o log
     * @param message Mensagem a ser registrada
     */
    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }

    /**
     * Registra uma mensagem de warning
     * @param clazz Classe que gerou o log
     * @param message Mensagem a ser registrada
     */
    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warn(message);
    }

    /**
     * Registra uma mensagem de erro
     * @param clazz Classe que gerou o log
     * @param message Mensagem a ser registrada
     * @param throwable Exceção associada (opcional)
     */
    public static void error(Class<?> clazz, String message, Throwable throwable) {
        if (throwable != null) {
            getLogger(clazz).error(message, throwable);
        } else {
            getLogger(clazz).error(message);
        }
    }
}