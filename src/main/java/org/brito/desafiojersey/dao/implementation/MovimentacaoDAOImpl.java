package org.brito.desafiojersey.dao.implementation;

import jakarta.inject.Inject;
import org.brito.desafiojersey.dao.ConteinerDAO;
import org.brito.desafiojersey.dao.ConteinerMovimentacaoDAO;
import org.brito.desafiojersey.dao.MovimentacaoDAO;
import org.brito.desafiojersey.dao.utils.SqlLoaderUtils;
import org.brito.desafiojersey.db.DatabaseConnection;
import org.brito.desafiojersey.domain.Conteiner;
import org.brito.desafiojersey.domain.ConteinerMovimentacao;
import org.brito.desafiojersey.domain.Movimentacao;
import org.brito.desafiojersey.enums.ETipoMovimentacao;
import org.brito.desafiojersey.exceptions.MovimentacaoException;
import org.brito.desafiojersey.exceptions.NaoEncontradoException;
import org.brito.desafiojersey.utils.MessageUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.brito.desafiojersey.dao.utils.DaoUtils.buscarKeyGerada;

/**
 * Implementação da interface {@link MovimentacaoDAO} utilizando JDBC e injeção de dependências para gerenciar movimentações.
 */
public class MovimentacaoDAOImpl implements MovimentacaoDAO {
    private final ConteinerMovimentacaoDAO conteinerMovimentacaoDAO;
    private final ConteinerDAO conteinerDAO;

    @Inject
    public MovimentacaoDAOImpl(ConteinerMovimentacaoDAO conteinerMovimentacaoDAO, ConteinerDAO conteinerDAO) {
        this.conteinerMovimentacaoDAO = conteinerMovimentacaoDAO;
        this.conteinerDAO = conteinerDAO;
    }

    @Override
    public long criarMovimentacao(Movimentacao movimentacao) throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.criar");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherStatementCriar(stmt, movimentacao);
            stmt.executeUpdate();
            return relacionarContainerMovimentacao(stmt, movimentacao);
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao("movimentacao.erro.criacao", e.getMessage()));
        }
    }

    @Override
    public Movimentacao buscarMovimentacaoPorId(long id) throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.buscar.por.id");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return executarQueryMovimentacao(stmt, id);
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao(e.getMessage()));
        }
    }


    @Override
    public Movimentacao fecharMovimentacao(long id) throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.fecha");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                return buscarMovimentacaoPorId(id);
            } else {
                throw new MovimentacaoException(
                        MessageUtils.buscaValidacao("movimentacao.nao.encontrada", id));
            }
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao("movimentacao.erro.buscar", e.getMessage()));
        }
    }

    @Override
    public List<Movimentacao> listaMovimentacoes() throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.todos");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Movimentacao> movimentacoes = new ArrayList<>();
            while (rs.next()) {
                movimentacoes.add(gerarMovimentacao(rs));
            }
            return movimentacoes;
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao("movimentacao.erro.listar", e.getMessage()));
        }
    }

    @Override
    public List<Movimentacao> listaMovimentacoesPorCliente(long idCliente) throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.todos.por.cliente");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idCliente);
            List<Movimentacao> movimentacoes = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimentacoes.add(gerarMovimentacao(rs));
                }
                return movimentacoes;
            }
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao("movimentacao.erro.listar", e.getMessage()));
        }
    }

    @Override
    public List<Movimentacao> listaMovimentacoesPorContainer(long idConteiner) throws MovimentacaoException {
        String sql = SqlLoaderUtils.getSql("movimentacao.todos.por.container");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idConteiner);
            List<Movimentacao> movimentacoes = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    movimentacoes.add(gerarMovimentacao(rs));
                }
                return movimentacoes;
            }
        } catch (SQLException e) {
            throw new MovimentacaoException(
                    MessageUtils.buscaValidacao("movimentacao.erro.listar", e.getMessage()));
        }
    }

    private void preencherStatementCriar(PreparedStatement stmt, Movimentacao movimentacao) throws SQLException {
        stmt.setString(1, movimentacao.getTipo().name());
        stmt.setTimestamp(2, java.sql.Timestamp.valueOf(movimentacao.getHoraInicio()));
        stmt.setTimestamp(3, movimentacao.getHoraFim() != null ? java.sql.Timestamp.valueOf(movimentacao.getHoraFim()) : null);
        stmt.setLong(4, movimentacao.getConteiner().getId());
    }

    private Movimentacao executarQueryMovimentacao(PreparedStatement stmt, long id) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return gerarMovimentacao(rs);
            } else {
                throw new NaoEncontradoException(
                        MessageUtils.buscaValidacao("movimentacao.nao.encontrada", id));
            }
        }
    }

    private long relacionarContainerMovimentacao(PreparedStatement stmt, Movimentacao movimentacao) throws SQLException {
        long id = buscarKeyGerada(stmt);
        conteinerMovimentacaoDAO.insereConteineresMovimentacoes(
                new ConteinerMovimentacao(movimentacao.getConteiner().getId(), id));

        return id;
    }

    private Movimentacao gerarMovimentacao(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        LocalDateTime horaInicio = rs.getTimestamp("hora_inicio").toLocalDateTime();
        LocalDateTime horaFim = rs.getTimestamp("hora_fim") != null ? rs.getTimestamp("hora_fim").toLocalDateTime() : null;
        Conteiner conteiner = conteinerDAO.buscarContainerPorId(rs.getLong("conteiner_id"));
        return new Movimentacao(id, ETipoMovimentacao.valueOf(rs.getString("tipo")), horaInicio, horaFim, conteiner);
    }

}
