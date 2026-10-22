package br.labai.rotas.ors;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OrsConfig {

    @Bean
    RestClient orsRestClient(@Value("${ors.url}") String orsUrl) {
        var tempos = new SimpleClientHttpRequestFactory();
        tempos.setConnectTimeout(Duration.ofSeconds(3));
        tempos.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl(orsUrl).requestFactory(tempos).build();
    }
}
