package com.facturacion.notificacion.application.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.facturacion.notificacion.application.dto.PlantillaCorreoResponse;
import com.facturacion.notificacion.application.dto.PlantillaPorDefectoResponse;
import com.facturacion.notificacion.application.dto.VariablePlantillaResponse;
import com.facturacion.notificacion.application.dto.VersionPlantillaResponse;
import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.VariableCorreo;
import com.facturacion.notificacion.domain.model.vo.PlantillaPorDefecto;
import com.facturacion.notificacion.domain.model.vo.VersionPlantillaCorreo;

/** Dominio → DTOs de respuesta de las plantillas de correo. Los DTO son solo datos: el mapeo vive aquí. */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PlantillaCorreoResponseMapper {

    PlantillaCorreoResponse toResponse(PlantillaCorreoEmpresa plantilla);

    List<PlantillaCorreoResponse> toResponses(List<PlantillaCorreoEmpresa> plantillas);

    VersionPlantillaResponse toResponse(VersionPlantillaCorreo version);

    List<VersionPlantillaResponse> toVersionResponses(List<VersionPlantillaCorreo> versiones);

    @Mapping(target = "tipoComprobante", source = "tipoComprobante")
    PlantillaPorDefectoResponse toResponse(String tipoComprobante, PlantillaPorDefecto plantilla);

    /** Es a mano porque {@link VariableCorreo} es un enum con accesores {@code nombre()}, no getters. */
    default VariablePlantillaResponse toResponse(VariableCorreo variable) {
        return new VariablePlantillaResponse(variable.nombre(), variable.marcador(), variable.descripcion());
    }
}
