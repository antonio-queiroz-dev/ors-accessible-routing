package br.labai.rotas.obstaculo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Enquanto o contrato com a câmera não é confirmado, carrega obstáculos fixos
 * no caminho da rota do Pelourinho. Só roda com o banco vazio.
 */
@Component
@ConditionalOnProperty(name = "obstaculos.teste", havingValue = "true")
public class CargaObstaculosTeste implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaObstaculosTeste.class);

    private final ObstaculoRepository repositorio;
    private final ObjectMapper json;

    public CargaObstaculosTeste(ObstaculoRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repositorio.count() > 0) {
            return;
        }
        List<NovoObstaculo> obstaculos;
        try (InputStream arquivo = new ClassPathResource("obstaculos-teste.json").getInputStream()) {
            obstaculos = json.readValue(arquivo, new TypeReference<>() {});
        }
        OffsetDateTime agora = OffsetDateTime.now();
        obstaculos.forEach(o -> repositorio.save(new Obstaculo(o.latitude(), o.longitude(), o.tipo(),
                o.confianca(), Passagem.de(o.passagem()), o.detectadoEm(), agora)));
        log.info("{} obstáculos de teste carregados", obstaculos.size());
    }
}
