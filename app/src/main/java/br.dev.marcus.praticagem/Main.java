package br.dev.marcus.praticagem;

import io.javalin.Javalin;
import org.jsoup.nodes.Document;

import br.dev.marcus.praticagem.parser.HtmlParser;
import br.dev.marcus.praticagem.model.MovimentacaoService;

import java.utils.List;

public class Main {

    public static void main(String[] args) {
        
        // URL pode vir de variável de ambiente
        String url = System.getenv().getOrDefault(
            "PRATICAGEM_URL",
            "https://praticoszp21.com.br/movimentacao-de-navios/"
        );

        HtmlFetcher fetcher = new HtmlFetcher(url, 10000);
        HtmlParser parser = new HtmlParser();
        MovimentacaoService service = new MovimentacaoService(fetcher, parser);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(7000);

        app.get("/movimentacoes", ctx -> {

            List<NavioMovimentacao> dados =
                service.buscarMovimentacoes();
            
                ctx.json(dados);
        });
    }
}
