package br.dev.marcus.praticagem.scrapping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HtmlFetcher {

    private static final Logger logger =
        LoggerFactory.getLogger(HtmlFetcher.class);

    private final String url;
    private final int timeoutMillis;

    public HtmlFetcher(String url, int timeoutMillis) {
        this.url = url;
        this.timeoutMillis = timeoutMillis;
    }

    public Document fetch() {

        try {
            logger.info("Buscando HTML da URL: {}", url);

            return Jsoup.connect(url)
                .timeout(timeoutMillis)
                .userAgent("Mozilla/5.0 ")
                .get();

        } catch (IOException e) {
            logger.error("Erro ao buscar HTML da URL: {}", url, e);
            throw new IllegalStateException("Falha ao buscar HTML remoto", e);
        }
    }

}
