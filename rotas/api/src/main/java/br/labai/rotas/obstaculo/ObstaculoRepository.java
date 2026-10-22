package br.labai.rotas.obstaculo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObstaculoRepository extends JpaRepository<Obstaculo, Long> {

    List<Obstaculo> findByConfiancaGreaterThanEqualAndLatitudeBetweenAndLongitudeBetween(
            double confiancaMinima, double minLat, double maxLat, double minLon, double maxLon);
}
