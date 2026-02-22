package br.dev.marcus.praticagem.service;

import org.jsoup.nodes.Document;
import java.util.List;

public class MovimentacaoService {
    
    private final HtmlFetcher fetcher;
    private final HtmlParser parser;

    public MovimentacaoService(HtmlFetcher fetcher, HtmlParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    public List<NavioMovimentacao> buscarMovimentacoes() {
        
        // 1) Buscar o HTML
        Document document = fetcher.fetch();

        // 2) Parsear o HTML e extrair as movimentações
        return parser.parse(document);
    }
}
