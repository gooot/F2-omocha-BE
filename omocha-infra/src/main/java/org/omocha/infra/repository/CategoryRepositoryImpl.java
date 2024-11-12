package org.omocha.infra.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.omocha.domain.auction.QCategory;
import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;

@Repository
public class CategoryRepositoryImpl implements CategoryRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	public CategoryRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	@Override
	public List<Long> getSubCategoryIds(Long categoryId) {
		if (categoryId == null) {
			return Collections.emptyList();
		}

		QCategory category = QCategory.category;
		List<Long> categoryIds = new ArrayList<>();
		collectSubCategoryIds(categoryId, categoryIds, category);
		return categoryIds;
	}

	private void collectSubCategoryIds(Long categoryId, List<Long> categoryIds, QCategory category) {
		categoryIds.add(categoryId);

		List<Long> subCategoryIds = queryFactory
			.select(category.categoryId)
			.from(category)
			.where(category.parent.categoryId.eq(categoryId))
			.fetch();

		for (Long subId : subCategoryIds) {
			categoryIds.add(subId);
			collectSubCategoryIds(subId, categoryIds, category); // 재귀 호출
		}
	}

/*	private void collectSubCategoryIds(Long categoryId, List<Long> categoryIds, QCategory category) {
		List<Long> subCategoryIds = queryFactory
			.select(category.categoryId)
			.from(category)
			.where(category.parent.categoryId.eq(categoryId))
			.fetch();

		for (Long subId : subCategoryIds) {
			categoryIds.add(subId);
			collectSubCategoryIds(subId, categoryIds, category);
		}
	}*/
}
