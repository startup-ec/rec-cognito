package com.cognito.virtual.repository;

import com.cognito.virtual.entity.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;


@Repository
public interface OpcionRepository extends JpaRepository<Opcion, Long> {

    @Query("SELECT ro.opcion FROM RoleOpcion ro WHERE ro.role_id = :roleId AND ro.activo = true")
    List<Opcion> findByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT ro.opcion FROM RoleOpcion ro WHERE ro.role_id = :roleId AND ro.activo = true AND ro.opcion.proyecto = :proyecto")
    List<Opcion> findByRoleIdAndProyecto(@Param("roleId") Long roleId, @Param("proyecto") String proyecto);

    List<Opcion> findAllByOrderByNombreAsc();

}