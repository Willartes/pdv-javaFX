package br.com.pdv.dao;

import br.com.pdv.model.Produto;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface DAO para manipulação de dados de produtos no banco de dados.
 */
public interface IProdutoDAO {

    Produto create(Produto produto) throws SQLException;

    Produto findById(Integer id) throws SQLException;

    Produto findByCodigo(String codigo) throws SQLException;

    List<Produto> findByNome(String nome) throws SQLException;

    boolean update(Produto produto) throws SQLException;

    boolean delete(Integer id) throws SQLException;

    List<Produto> findAll() throws SQLException;

    List<Produto> findEstoqueBaixo() throws SQLException;

    long count() throws SQLException;
}