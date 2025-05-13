package br.com.pdv.model;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;

import br.com.pdv.dao.ClienteDAO;

import java.time.ZoneId;
import java.util.Date;

public class Cliente {
    private Long id;
    private String nome;
    private String cpfCnpj;
    private String endereco;
    private String telefone;
    private String email;
    private boolean ativo;
    private Date dataCadastro;	
    private LocalDateTime data;
    private Date dataNascimento;
    private String observacao;
    
    public Cliente() {
        this.ativo = true;  // por padrão, cliente é criado como ativo
    }
    
    public Cliente(String nome, String cpfCnpj, String endereco, String telefone, String email) {
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.endereco = endereco;
        this.telefone = telefone;
        this.email = email;
        this.ativo = true;  // por padrão, cliente é criado como ativo
    }
    
    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(int i) {
        this.id = (long) i;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getCpfCnpj() {
        return cpfCnpj;
    }
    
    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }
    
    public String getEndereco() {
        return endereco;
    }
    
    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }
    
    public String getTelefone() {
        return telefone;
    }
    
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public Date getDataCadastro() {
        return dataCadastro;
    }
    
    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
    
    public LocalDateTime getData() {
        return data;
    }
    
    public void setData(LocalDateTime data) {
        this.data = data;
    }
    
    
    
    @Override
    public String toString() {
        return "Cliente{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpfCnpj='" + cpfCnpj + '\'' +
                ", endereco='" + endereco + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                ", ativo=" + ativo +
                ", dataCadastro=" + dataCadastro +
                '}';
    }
    
    private Cliente buscarOuCriarCliente(String nomeCliente) throws SQLException {
        // Buscar cliente pelo nome
        ClienteDAO clienteDAO = ClienteDAO.getInstance();
        
        // Opção 1: Se você implementou findByNome no ClienteDAO
        Cliente cliente = clienteDAO.findByNome(nomeCliente);
        
        /* Opção 2: Se você está usando findAll() e filtrando manualmente
        List<Cliente> clientes = clienteDAO.findAll();
        Cliente cliente = null;
        for (Cliente c : clientes) {
            if (nomeCliente.equals(c.getNome())) {
                cliente = c;
                break;
            }
        }
        */
        
        // Se não existir, criar um novo
        if (cliente == null) {
            cliente = new Cliente();
            cliente.setNome(nomeCliente);
            cliente.setData(LocalDateTime.now());
            cliente.setAtivo(true);
            
            // Salvar o novo cliente
            clienteDAO.save(cliente);
        }
        
        return cliente;
    }
}