package org.omocha.api.interfaces;

import static org.omocha.domain.exception.code.SuccessCode.*;

import java.util.List;

import org.omocha.api.application.CategoryFacade;
import org.omocha.api.common.response.ResultDto;
import org.omocha.api.interfaces.dto.CategoryDto;
import org.omocha.api.interfaces.mapper.CategoryDtoMapper;
import org.omocha.domain.auction.CategoryCommand;
import org.omocha.domain.auction.CategoryInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/categories")
public class CategoryController implements CategoryApi {

	private final CategoryFacade categoryFacade;
	private final CategoryDtoMapper categoryMapper;

	@Override
	@PostMapping
	public ResponseEntity<ResultDto<CategoryDto.CategoryAddResponse>> categoryAdd(
		@RequestBody CategoryDto.CategoryAddRequest request
	) {
		CategoryCommand.AddCategory addCommand = categoryMapper.toCommand(request);

		Long categoryId = categoryFacade.addCategory(addCommand);

		CategoryDto.CategoryAddResponse response = categoryMapper.toResponse(categoryId);

		ResultDto<CategoryDto.CategoryAddResponse> result = ResultDto.res(
			CATEGORY_CREATE_SUCCESS.getStatusCode(),
			CATEGORY_CREATE_SUCCESS.getDescription(),
			response
		);

		return ResponseEntity
			.status(CATEGORY_CREATE_SUCCESS.getHttpStatus())
			.body(result);
	}

	@Override
	@GetMapping("/{category_id}")
	public ResponseEntity<ResultDto<List<CategoryInfo.CategoryResponse>>> categoryHierarchy(
		@PathVariable("category_id") Long categoryId
	) {

		List<CategoryInfo.CategoryResponse> response = categoryFacade.retrieveCategoryHierarchy(categoryId);
		ResultDto<List<CategoryInfo.CategoryResponse>> result = ResultDto.res(
			CATEGORY_HIERARCHY_SUCCESS.getStatusCode(),
			CATEGORY_HIERARCHY_SUCCESS.getDescription(),
			response
		);

		return ResponseEntity
			.status(CATEGORY_HIERARCHY_SUCCESS.getHttpStatus())
			.body(result);
	}

	@Override
	@GetMapping
	public ResponseEntity<ResultDto<List<CategoryInfo.CategoryResponse>>> categoryDetails() {

		List<CategoryInfo.CategoryResponse> response = categoryFacade.retrieveCategories();

		ResultDto<List<CategoryInfo.CategoryResponse>> result = ResultDto.res(
			CATEGORY_DETAIL_SUCCESS.getStatusCode(),
			CATEGORY_DETAIL_SUCCESS.getDescription(),
			response
		);
		return ResponseEntity
			.status(CATEGORY_DETAIL_SUCCESS.getHttpStatus())
			.body(result);
	}

}
