package com.challenge.mapper;

import com.challenge.dto.AttributeGroupDTO;
import com.challenge.dto.CategoryDetailDTO;
import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.model.Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    // ── toTreeDTO ─────────────────────────────────────────────────────────────

    @Test
    void toTreeDTO_returnsNull_whenCategoryIsNull() {
        assertThat(CategoryMapper.toTreeDTO(null)).isNull();
    }

    @Test
    void toTreeDTO_mapsIdAndName_withEmptyChildrenWhenNull() {
        Category cat = Category.builder().id(1L).name("Tecnología").build();

        CategoryTreeDTO dto = CategoryMapper.toTreeDTO(cat);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Tecnología");
        assertThat(dto.children()).isEmpty();
    }

    @Test
    void toTreeDTO_mapsChildrenRecursively() {
        Category child = Category.builder().id(2L).name("Smartphones").build();
        Category root = Category.builder().id(1L).name("Tecnología")
                .children(List.of(child))
                .build();

        CategoryTreeDTO dto = CategoryMapper.toTreeDTO(root);

        assertThat(dto.children()).hasSize(1);
        assertThat(dto.children().get(0).id()).isEqualTo(2L);
        assertThat(dto.children().get(0).name()).isEqualTo("Smartphones");
    }

    // ── toSummaryDTO ──────────────────────────────────────────────────────────

    @Test
    void toSummaryDTO_returnsNull_whenCategoryIsNull() {
        assertThat(CategoryMapper.toSummaryDTO(null)).isNull();
    }

    @Test
    void toSummaryDTO_mapsIdAndName() {
        Category cat = Category.builder().id(3L).name("Cafeteras").build();

        CategorySummaryDTO dto = CategoryMapper.toSummaryDTO(cat);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.name()).isEqualTo("Cafeteras");
    }

    // ── toDetailDTO ───────────────────────────────────────────────────────────

    @Test
    void toDetailDTO_returnsNull_whenCategoryIsNull() {
        assertThat(CategoryMapper.toDetailDTO(null, List.of(), List.of())).isNull();
    }

    @Test
    void toDetailDTO_mapsAllFields_withParentAndComparables() {
        Category parent = Category.builder().id(1L).name("Tecnología").build();
        Category cat = Category.builder().id(2L).name("Smartphones").parent(parent).build();

        CategorySummaryDTO comparable = new CategorySummaryDTO(7L, "Tablets");
        List<CategorySummaryDTO> comparableWith = List.of(comparable);
        List<AttributeGroupDTO> attributeGroups = List.of();

        CategoryDetailDTO dto = CategoryMapper.toDetailDTO(cat, comparableWith, attributeGroups);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.name()).isEqualTo("Smartphones");
        assertThat(dto.parent().id()).isEqualTo(1L);
        assertThat(dto.parent().name()).isEqualTo("Tecnología");
        assertThat(dto.comparableWith()).hasSize(1);
        assertThat(dto.comparableWith().get(0).id()).isEqualTo(7L);
        assertThat(dto.attributeGroups()).isEmpty();
    }

    @Test
    void toDetailDTO_returnsEmptyLists_whenNullsPassedIn() {
        Category cat = Category.builder().id(2L).name("Smartphones").build();

        CategoryDetailDTO dto = CategoryMapper.toDetailDTO(cat, null, null);

        assertThat(dto.comparableWith()).isEmpty();
        assertThat(dto.attributeGroups()).isEmpty();
    }
}
