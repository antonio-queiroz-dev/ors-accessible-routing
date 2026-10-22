package br.labai.rotas.obstaculo;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/obstaculos")
public class ObstaculoController {

    private final ObstaculoRepository repositorio;

    public ObstaculoController(ObstaculoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Operation(summary = "Registra um obstáculo detectado pela câmera")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Obstaculo registrar(@Valid @RequestBody NovoObstaculo novo) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime detectadoEm = novo.detectadoEm() != null ? novo.detectadoEm() : agora;
        return repositorio.save(new Obstaculo(novo.latitude(), novo.longitude(), novo.tipo().trim(),
                novo.confianca(), Passagem.de(novo.passagem()), detectadoEm, agora));
    }

    @Operation(summary = "Lista todos os obstáculos registrados")
    @GetMapping
    public List<Obstaculo> listar() {
        return repositorio.findAll();
    }
}
