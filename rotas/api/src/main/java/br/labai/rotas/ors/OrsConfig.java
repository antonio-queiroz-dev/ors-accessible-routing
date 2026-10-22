package br.labai.rotas.ors;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OrsConfig {

    @Bean
    RestClient orsRestClient(@Value("${ors.url}") String orsUrl, @Value("${ors.chave:}") String chave) {
        var tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(Duration.ofSeconds(3));
        tempos.setReadTimeout(Duration.ofSeconds(15));
        var builder = RestClient.builder().baseUrl(orsUrl).requestFactory(tempos);
        // A API pública exige a chave; um ORS próprio não precisa dela
        if (!chave.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, chave.trim());
        }
        return builder.build();
    }
}
