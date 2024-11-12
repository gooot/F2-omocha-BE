package org.omocha.api.interfaces.mapper;

import java.util.List;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.omocha.api.interfaces.dto.CategoryDto;
import org.omocha.domain.auction.Category;
import org.omocha.domain.auction.CategoryCommand;

@Mapper(
	componentModel = "spring",
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
	unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CategoryMapper {

	CategoryCommand.AddCategory toCommand(CategoryDto.CategoryAddRequest request);

	CategoryDto.CategoryAddResponse toResponse(Long categoryId);

	List<CategoryDto.CategoryResponse> toResponseList(List<Category> categories);

	@Mapping(target = "categoryId", source = "categoryId")
	@Mapping(target = "name", source = "name")
	@Mapping(target = "parentId", source = "parent.categoryId")
	@Mapping(target = "subCategories", source = "subCategories")
	CategoryDto.CategoryResponse toResponse(Category category);
}
