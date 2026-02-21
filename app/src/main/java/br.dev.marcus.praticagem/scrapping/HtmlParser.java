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
        validarColuna(indiceColunas, "data");
        validarColuna(indiceColunas, "horario");
        validarColuna(indiceColunas, "manobra");
        validarColuna(indiceColunas, "berco");
        validarColuna(indiceColunas, "navio");
        validarColuna(indiceColunas, "situacao");
            
        /*
         * ===== PASSO 2: Ler linhas de dados =====
         */
        for (int i = 1; i < linhas.size(); i++) {

            Elements colunas = linhas.get(i).select("td");

            if (colunas.isEmpty()) {
                continue;
            }

            String data = pegar(colunas, indiceColunas, "data");
            String horario = pegar(colunas, indiceColunas, "horario");
            String manobra = pegar(colunas, indiceColunas, "manobra");
            String berco = pegar(colunas, indiceColunas, "berco");
            String navio = pegar(colunas, indiceColunas, "navio");
            String situacao = pegar(colunas, indiceColunas, "situacao");

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
            if (nomes.contains("data") &&
                nomes.contains("horario") &&
                nomes.contains("manobra") &&
                nomes.contains("berco") &&
                nomes.contains("navio") &&
                nomes.contains("situacao")) {
                
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

}
