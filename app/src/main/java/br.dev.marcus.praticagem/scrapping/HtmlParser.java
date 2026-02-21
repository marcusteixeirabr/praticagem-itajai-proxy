package br.dev.marcus.praticagem.scrapping;

import br.dev.marcus.praticagem.scrapping.NavioMovimentacao;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser resiliente da tabela de movimentação.
 *
 * Estratégia:
 * 1) Localiza a tabela
 * 2) Lê o cabeçalho (th)
 * 3) Mapeia nome da coluna -> índice
 * 4) Extrai dados usando o mapa
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

        // Se não houver linhas, retornamos lista vazia
        if (linhas.size() < 2) {
            return movimentacoes;
        }

        /*
         * ===== PASSO 1: Mapear cabeçalho =====
         */
        Element headerRow = linhas.get(0);
        Elements headers = headerRow.select("th");

        Map<String, Integer> indiceColunas = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String nomeColuna = normalizar(headers.get(i).text());
            indiceColunas.put(nomeColuna, i);
        }

        /*
         * Validamos se as colunas essenciais existem.
         * Se não existirem, falhamos de forma explícita.
         */
        validarColuna(indiceColunas, "data");
        validarColuna(indiceColunas, "horário");
        validarColuna(indiceColunas, "manobra");
        validarColuna(indiceColunas, "berço");
        validarColuna(indiceColunas, "navio");
        validarColuna(indiceColunas, "situação");
            
        /*
         * ===== PASSO 2: Ler linhas de dados =====
         */
        for (int i = 1; i < linhas.size(); i++) {

            Elements colunas = linhas.get(i).select("td");

            if (colunas.isEmpty()) {
                continue;
            }

            String data = pegar(colunas, indiceColunas, "data");
            String horario = pegar(colunas, indiceColunas, "horário");
            String manobra = pegar(colunas, indiceColunas, "manobra");
            String berco = pegar(colunas, indiceColunas, "berço");
            String navio = pegar(colunas, indiceColunas, "navio");
            String situacao = pegar(colunas, indiceColunas, "situação");

            movimentacoes.add(
                new NavioMovimentacao(
                    data,
                    horario,
                    manobra,
                    berco,
                    navio,
                    situacao
                )
            );
        }

        return movimentacoes;
    }

    /**
     * Normaliza texto para evitar problemas com maiúsculas/minúsculas.
     */
    private String normalizar(String texto) {
        return texto.trim().toLowerCase();
    }

    /**
     * Garante que a coluna obrigatória existe.
     * Se não existir, falha explicitamente.
     */
    private void validarColuna(Map<String, Integer> mapa, String coluna) {
        if (!mapa.containsKey(coluna)) {
            throw new IllegalStateException(
                    "Coluna obrigatória não encontrada: " + coluna
            );
        }
    }

        /**
     * Extrai valor da coluna dinamicamente pelo nome.
     */
    private String pegar(Elements colunas,
                         Map<String, Integer> mapa,
                         String nomeColuna) {

        Integer indice = mapa.get(nomeColuna);

        if (indice == null || indice >= colunas.size()) {
            return "";
        }

        return colunas.get(indice).text().trim();
    }
    
    /**
     * Extrai valor da coluna dinamicamente pelo nome.
     */
    private String pegar(Elements colunas,
                         Map<String, Integer> mapa,
                         String nomeColuna) {

        Integer indice = mapa.get(nomeColuna);

        if (indice == null || indice >= colunas.size()) {
            return "";
        }

        return colunas.get(indice).text().trim();
    }

}
