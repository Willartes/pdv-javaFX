package br.com.pdv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe que representa um produto no sistema.
 */
public class Produto {

    private Integer id;
    private String nome;
    private String descricao;
    private String codigo;
    private String codigoBarra;
    private BigDecimal preco;
    private BigDecimal custo;
    private String unidade;
    private int estoqueAtual;
    private int estoqueMinimo;
    private String tipo;
    private String cor;
    private String tamanho;
    private String cfop;
    private BigDecimal icms;
    private BigDecimal icmsSub;
    private Categoria categoria;
    private Subcategoria subcategoria;
    private Marca marca;
    private boolean ativo;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataAtualizacao;
    private boolean atualizarEstoque;
    private LocalDate dataVencimento;
    private BigDecimal markup;
    // Construtores
    public Produto() {
        this.ativo = true;
        this.atualizarEstoque = true;
        this.dataCadastro = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
        this.preco = BigDecimal.ZERO;
        this.custo = BigDecimal.ZERO;
        this.icms = BigDecimal.ZERO;
        this.icmsSub = BigDecimal.ZERO;
        this.estoqueAtual = 0;
        this.estoqueMinimo = 0;
        this.unidade = "UN";
    }
    
 // Adicionar este construtor à classe Produto
    public Produto(String nome, String codigo, String descricao, BigDecimal preco, int estoque, boolean ativo) {
        this.nome = nome;
        this.codigo = codigo;
        this.descricao = descricao;
        this.preco = preco;
        this.estoqueAtual = estoque;
        this.ativo = ativo;
        this.dataCadastro = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    // Getters e Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
    
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getCusto() {
        return custo;
    }

    public void setCusto(BigDecimal custo) {
        this.custo = custo;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public int getEstoqueAtual() {
        return estoqueAtual;
    }

    public void setEstoqueAtual(int estoqueAtual) {
        this.estoqueAtual = estoqueAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getTamanho() {
        return tamanho;
    }

    public void setTamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getCfop() {
        return cfop;
    }

    public void setCfop(String cfop) {
        this.cfop = cfop;
    }

    public BigDecimal getIcms() {
        return icms;
    }

    public void setIcms(BigDecimal icms) {
        this.icms = icms;
    }

    public BigDecimal getIcmsSub() {
        return icmsSub;
    }

    public void setIcmsSub(BigDecimal icmsSub) {
        this.icmsSub = icmsSub;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Subcategoria getSubcategoria() {
        return subcategoria;
    }

    public void setSubcategoria(Subcategoria subcategoria) {
        this.subcategoria = subcategoria;
    }

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    // Equals e HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Produto produto = (Produto) o;
        return Objects.equals(id, produto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ToString
    @Override
    public String toString() {
        return nome;
    }

    /**
     * Atualiza o estoque do produto com base na quantidade e tipo de operação.
     * 
     * @param quantidade A quantidade a ser atualizada no estoque
     * @param tipoOperacao O tipo de operação: "ENTRADA" para adicionar ao estoque, 
     *                     "SAIDA" para remover do estoque
     * @return true se a operação foi realizada com sucesso, false caso contrário
     * @throws IllegalArgumentException se o tipo de operação for inválido ou se 
     *                                  tentar remover mais do que existe em estoque
     */
    public boolean atualizarEstoque(int quantidade, String tipoOperacao) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        
        if (tipoOperacao.equalsIgnoreCase("ENTRADA")) {
            // Adicionar ao estoque
            this.estoqueAtual += quantidade;
            this.dataAtualizacao = LocalDateTime.now();
            return true;
        } else if (tipoOperacao.equalsIgnoreCase("SAIDA")) {
            // Verificar se há estoque suficiente
            if (this.estoqueAtual < quantidade) {
                throw new IllegalArgumentException(
                    "Estoque insuficiente. Disponível: " + this.estoqueAtual + 
                    ", Solicitado: " + quantidade);
            }
            
            // Remover do estoque
            this.estoqueAtual -= quantidade;
            this.dataAtualizacao = LocalDateTime.now();
            return true;
        } else {
            throw new IllegalArgumentException(
                "Tipo de operação inválido. Use 'ENTRADA' ou 'SAIDA'");
        }
    }

	public BigDecimal getMarkup() {
		return markup;
	}

	public void setMarkup(BigDecimal markup) {
		this.markup = markup;
	}
}