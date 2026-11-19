package com.facturacion.notificacion.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.infrastructure.persistence.entity.PlantillaCorreoEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PlantillaCorreoMapper {

    PlantillaCorreoEntity toEntity(PlantillaCorreoEmpresa domain);

    PlantillaCorreoEmpresa toDomain(PlantillaCorreoEntity entity);
}
