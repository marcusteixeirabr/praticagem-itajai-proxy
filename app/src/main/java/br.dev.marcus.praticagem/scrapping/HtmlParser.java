package br.dev.marcus.praticagem.scrapping;

import br.dev.marcus.praticagem.scrapping.NavioMovimentacao;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Encontrar a tabela de movimentação
        Element tabela = encontrarTabelaMovimentacao(document);

        // Se não encontramos a tabela de movimentação, retornamos lista vazia
        if (tabela == null) {
            throw new IllegalStateException("Tabela de movimentação não encontrada");
        }

        // Seleciona todas as linhas da tabela
        Elements linhas = tabela.select("tr");

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
        Integer idxData = encontrarIndicePorPalavra(indiceColunas, "data");
        Integer idxHorario = encontrarIndicePorPalavra(indiceColunas, "horario");
        Integer idxManobra = encontrarIndicePorPalavra(indiceColunas, "manobra");
        Integer idxBerco = encontrarIndicePorPalavra(indiceColunas, "berco");
        Integer idxNavio = encontrarIndicePorPalavra(indiceColunas, "navio");
        Integer idxSituacao = encontrarIndicePorPalavra(indiceColunas, "situacao");

        if (idxData == null || idxHorario == null || idxManobra == null ||
            idxBerco == null || idxNavio == null || idxSituacao == null) {

            throw new IllegalStateException("Estrutura da tabela mudou.");
        }
            
        /*
         * ===== PASSO 2: Ler linhas de dados =====
         */
        for (int i = 1; i < linhas.size(); i++) {

            Elements colunas = linhas.get(i).select("td");

            if (colunas.isEmpty()) {
                continue;
            }

            String data = pegarPorIndice(colunas, idxData);
            String horario = pegarPorIndice(colunas, idxHorario);
            String manobra = pegarPorIndice(colunas, idxManobra);
            String berco = pegarPorIndice(colunas, idxBerco);
            String navio = pegarPorIndice(colunas, idxNavio);
            String situacao = pegarPorIndice(colunas, idxSituacao);

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
     * Busca a tabela de movimentação dentro do documento HTML.
     * A estratégia é procurar por uma tabela que contenha as colunas essenciais.
     * @param document
     * @return
     */
    private Element encontrarTabelaMovimentacao(Document document) {

        // Seleciona todas as tabelas da página
        Elements tabelas = document.select("table");

        for (Element tabela : tabelas) {

            Elements headers = tabela.select("th");
            
            Set<String> nomes = new HashSet<>();

            for (Element th : headers) {
                nomes.add(normalizar(th.text()));
            }

            // Verificamos de contém todas as colunas essenciais
            if (contemColuna(nomes, "data") &&
                contemColuna(nomes, "horario") &&
                contemColuna(nomes, "manobra") &&
                contemColuna(nomes, "berco") &&
                contemColuna(nomes, "navio") &&
                contemColuna(nomes, "situacao")) {

            return tabela;
}
        }

        return null;
    }

    /**
     * Normaliza texto para evitar problemas com:
     * - Acentuação
     * - Maiúsculas/minúsculas
     * - Espaços extras
     */
    private String normalizar(String texto) {
        
        if (texto == null) {
            return "";
        }

        // Remove acentos
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Remove espaços extras e converte para minúsculas
        return semAcento.trim().toLowerCase();
    }

    /**
     * Procura o índice de uma coluna que contenha a palavra-chave.
     * Exemplo: "situacao" vai casar com "situacao da manobra"
     */
    private Integer encontrarIndicePorPalavra(Map<String, Integer> mapa,
                                            String palavraChave) {

        for (Map.Entry<String, Integer> entry : mapa.entrySet()) {

            String nomeColuna = entry.getKey();

            if (nomeColuna.contains(palavraChave)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Extrai valor de coluna dinamicamente pelo nome, usando índice já validado.
     */
    private String pegarPorIndice(Elements colunas, Integer indice) {

        if (indice == null || indice >= colunas.size()) {
            return "";
        }

        return colunas.get(indice).text().trim();
    }

    private boolean contemColuna(Set<String> nomes, String palavraChave) {
    for (String nome : nomes) {
        if (nome.contains(palavraChave)) {
            return true;
        }
    }
    return false;
}

}
