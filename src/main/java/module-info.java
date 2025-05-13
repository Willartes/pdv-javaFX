module pdv {
	requires javafx.controls;
	requires log4j;
	requires java.sql;
	requires java.base;
	requires itextpdf;

    requires javafx.fxml;
    requires java.desktop;
    
    
    
    
    
    
    exports br.com.pdv.application;
	exports br.com.pdv.controller;
	exports br.com.pdv.util;
	exports br.com.pdv.relatorio;
	exports br.com.pdv.dao;
	opens br.com.pdv.application to javafx.graphics, javafx.fxml;
	opens br.com.pdv.model to javafx.base;
	opens br.com.pdv.controller to javafx.fxml;
	
}
