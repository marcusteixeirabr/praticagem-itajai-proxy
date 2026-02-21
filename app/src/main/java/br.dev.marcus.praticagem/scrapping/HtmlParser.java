package br.dev.marcus.praticagem.scrapping;

import br.dev.marcus.praticagem.scrapping.NavioMovimentacao;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável por interpretar o HTML da página
 * e extrair as informações da tabela de movimentação.
 *
 * Essa classe NÃO baixa o HTML.
 * Ela apenas recebe um Document já carregado.
 *
 * Separar responsabilidades é fundamental.
 */
public class HtmlParser {

    /**
     * Método principal do parser.
     *
     * @param document Documento HTML já parseado pelo Jsoup
     * @return Lista de movimentações encontradas
     */
    public List<NavioMovimentacao> parse(Document document) {

        // Lista que será retornada no final
        List<NavioMovimentacao> movimentacoes = new ArrayList<>();

        // Seleciona todas as tabelas da página
        Elements tabelas = document.select("table");

        // Se não houver tabela, retornamos lista vazia
        if (tabelas.isEmpty()) {
            return movimentacoes;
        }

        /*
         * IMPORTANTE:
         * Estamos assumindo que a primeira tabela é a que nos interessa.
         * Se o site mudar isso, precisaremos melhorar essa lógica.
         */
        Element primeiraTabela = tabelas.first();

        // Seleciona todas as linhas da tabela
        Elements linhas = primeiraTabela.select("tr");

        /*
         * A primeira linha geralmente é o cabeçalho (<th>).
         * Por isso começamos no índice 1.
         */
        for (int i = 1; i < linhas.size(); i++) {

            Element linha = linhas.get(i);

            // Seleciona todas as colunas da linha
            Elements colunas = linha.select("td");

            /*
             * A tabela completa tem 11 colunas:
             * 0 Data
             * 1 Horário
             * 2 Manobra
             * 3 Berço
             * 4 Bordo
             * 5 Navio
             * 6 Rota
             * 7 Loa
             * 8 Boca
             * 9 Calado
             * 10 Situação
             *
             * Se tiver menos que isso, ignoramos.
             */            
            if (colunas.size() < 11) {
                continue;
            }

            // Extraímos apenas as colunas que interessam
            String data = colunas.get(0).text().trim();
            String horario = colunas.get(1).text().trim();
            String manobra = colunas.get(2).text().trim();
            String berco = colunas.get(3).text().trim();
            String navio = colunas.get(5).text().trim();
            String situacao = colunas.get(10).text().trim();

            // Criamos o objeto representando essa linha
            NavioMovimentacao movimentacao = new NavioMovimentacao(
                data,
                horario,
                manobra,
                berco,
                navio,
                situacao
            );

            // Adicionamos na lista final
            movimentacoes.add(movimentacao);
        
        }

        return movimentacoes;
    }
}
