package org.omocha.domain.auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long categoryId;

	private String name;

	@ManyToOne
	@JoinColumn(name = "parent_id")
	private Category parent;

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Category> subCategories = new ArrayList<>();

	@OneToMany(mappedBy = "category")
	private List<AuctionCategory> auctionCategories = new ArrayList<>();

	@Builder
	public Category(String name, Category parent) {
		this.name = name;
		this.setParent(parent);
	}

	public void setParent(Category parent) {
		if (this.parent != null) {
			this.parent.getSubCategories().remove(this);
		}
		this.parent = parent;
		if (parent != null && !parent.getSubCategories().contains(this)) {
			parent.getSubCategories().add(this);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Category category = (Category)o;
		return Objects.equals(categoryId, category.categoryId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(categoryId);
	}
}
