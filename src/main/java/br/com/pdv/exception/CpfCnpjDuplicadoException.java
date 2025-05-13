package br.com.pdv.exception;

public class CpfCnpjDuplicadoException extends Exception {
    public CpfCnpjDuplicadoException(String cpfCnpj) {
        super("CPF/CNPJ já cadastrado no sistema: " + cpfCnpj);
    }
    
    
}