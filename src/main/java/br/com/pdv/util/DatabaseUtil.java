package br.com.pdv.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtil {
	
	/**
     * Método utilitário para fechar recursos de banco de dados de forma segura
     */
	public static void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
	    if (rs != null) {
	        try {
	            rs.close();
	        } catch (SQLException e) {
	            LogUtil.warn(DatabaseConnection.class, "Erro ao fechar ResultSet: " + e.getMessage());
	        }
	    }

	    if (stmt != null) {
	        try {
	            stmt.close();
	        } catch (SQLException e) {
	            LogUtil.warn(DatabaseConnection.class, "Erro ao fechar Statement: " + e.getMessage());
	        }
	    }

	    if (conn != null) {
	        try {
	            conn.close();
	        } catch (SQLException e) {
	            LogUtil.warn(DatabaseConnection.class, "Erro ao fechar Connection: " + e.getMessage());
	        }
	    }
	}
}
