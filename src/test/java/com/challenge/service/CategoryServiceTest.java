package com.challenge.service;

import com.challenge.dto.CategorySummaryDTO;
import com.challenge.dto.CategoryTreeDTO;
import com.challenge.exception.ItemNotFoundException;
import com.challenge.model.Category;
import com.challenge.model.ComparableCategory;
import com.challenge.repository.CategoryAttributeRuleRepository;
import com.challenge.repository.CategoryRepository;
import com.challenge.repository.ComparableCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryAttributeRuleRepository ruleRepository;

    @Mock
    private ComparableCategoryRepository comparableCategoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getTree_returnsOnlyRootCategories() {
        Category root = Category.builder().id(1L).name("Tecnología").build();
        Category child = Category.builder().id(2L).name("Smartphones").parent(root).build();

        when(categoryRepository.findAllWithChildren()).thenReturn(List.of(root, child));

        List<CategoryTreeDTO> result = categoryService.getTree();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Tecnología");
    }

    @Test
    void getDetail_throwsItemNotFoundException_whenCategoryDoesNotExist() {
        when(categoryRepository.findByIdWithParent(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getDetail(99L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getComparableCategories_returnsTheOtherCategoryInEachPair() {
        Category smartphones = Category.builder().id(2L).name("Smartphones").build();
        Category tablets = Category.builder().id(7L).name("Tablets").build();

        ComparableCategory pair = new ComparableCategory(1L, smartphones, tablets);

        when(comparableCategoryRepository.findAllByCategoryId(2L)).thenReturn(List.of(pair));

        List<CategorySummaryDTO> result = categoryService.getComparableCategories(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(7L);
        assertThat(result.get(0).name()).isEqualTo("Tablets");
    }
}
