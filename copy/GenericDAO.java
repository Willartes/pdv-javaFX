package br.com.pdv.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface genérica para operações de CRUD
 * @param <T> Tipo da entidade
 * @param <K> Tipo da chave primária
 */
public interface GenericDAO<T, K> {
    
    /**
     * Salva uma nova entidade no banco de dados
     * @param entity Entidade a ser salva
     * @return Entidade salva com ID gerado
     * @throws SQLException em caso de erro no banco de dados
     */
    T create(T entity) throws SQLException;
    
    /**
     * Atualiza uma entidade existente
     * @param entity Entidade a ser atualizada
     * @return true se atualizado com sucesso
     * @throws SQLException em caso de erro no banco de dados
     */
    boolean update(T entity) throws SQLException;
    
    /**
     * Exclui uma entidade pelo ID
     * @param id ID da entidade
     * @return true se excluído com sucesso
     * @throws SQLException em caso de erro no banco de dados
     */
    boolean delete(K id) throws SQLException;
    
    /**
     * Busca uma entidade pelo ID
     * @param id ID da entidade
     * @return Entidade encontrada ou null
     * @throws SQLException em caso de erro no banco de dados
     */
    T findById(K id) throws SQLException;
    
    /**
     * Lista todas as entidades
     * @return Lista de entidades
     * @throws SQLException em caso de erro no banco de dados
     */
    List<T> findAll() throws SQLException;
    
    /**
     * Verifica se uma entidade existe pelo ID
     * @param id ID da entidade
     * @return true se existe
     * @throws SQLException em caso de erro no banco de dados
     */
    boolean exists(K id) throws SQLException;
    
    /**
     * Conta total de registros
     * @return número de registros
     * @throws SQLException em caso de erro no banco de dados
     */
    long count() throws SQLException;
}