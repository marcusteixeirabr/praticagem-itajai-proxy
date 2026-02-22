package br.dev.marcus.praticagem.fetcher;

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

        int tentativas = 3;
        int tentativaAtual = 0;

        while (tentativaAtual < tentativas) {

            try {
                tentativaAtual++;

                logger.info("Tentativa {} de buscar HTML da URL: {}", tentativaAtual, url);

                return Jsoup.connect(url)
                    .timeout(timeoutMillis)
                    .userAgent("Mozilla/5.0 ")
                    .get();

            } catch (IOException e) {

                logger.warn("Falha na tentativa {}", tentativaAtual);

                if (tentativaAtual >= tentativas) {
                    logger.error("Todas as tentativas falharam para URL: {}", url);
                    throw new IllegalStateException(
                        "Falha após múltiplas tentativas", e);
                }

                try {
                    Thread.sleep(2000); // espera 2 segundos antes de tentar novamente
                } catch (InterruptedException ignored) {}
            }
        }

                logger.error("Erro ao buscar HTML da URL: {}", url);
                throw new IllegalStateException("Erro inesperado no fetch");
    }
}

