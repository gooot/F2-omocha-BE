package org.omocha.domain.auction;

public class CategoryCommand {
	public record AddCategory(
		String name,
		Long parentId
	) {
		public Category toEntity(Category parent) {
			return Category.builder()
				.name(name)
				.parent(parent)
				.build();
		}

	}
}
