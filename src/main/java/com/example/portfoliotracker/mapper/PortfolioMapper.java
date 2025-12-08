package com.example.portfoliotracker.mapper;

import com.example.portfoliotracker.dto.FundDto;
import com.example.portfoliotracker.entity.FundEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {
    PortfolioMapper INSTANCE = Mappers.getMapper(PortfolioMapper.class);


    //@Mapping(source = "amfiCode", target = "amfiCode")
    FundDto entityToDto(FundEntity entity);

    FundEntity dtoToEntity(FundDto dto);
}
