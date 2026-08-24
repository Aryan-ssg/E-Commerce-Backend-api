package com.example.Ecommerce.Category;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Category.DTOs.response.CategoryResponse;
import com.example.Ecommerce.Category.DTOs.response.GetAllCategoriesResponse;
import com.example.Ecommerce.Category.DTOs.response.Productsresponse;
import com.example.Ecommerce.Common.Exceptions.CategoryAlreadyExistsException;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.JwtAuthenticationEntryPoint;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Common.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private AppUserRepository appUserRepository;

    @Test
    void getAllCategories_public_returns200WithCategoriesAndProducts() throws Exception {
        GetAllCategoriesResponse category = new GetAllCategoriesResponse(1L, "Electronics",
                List.of(new Productsresponse(100L, "Headphones", 2000)));
        when(categoryService.getAllCategories()).thenReturn(List.of(category));

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].products", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].products[0].productName").value("Headphones"));
    }

    @Test
    void createCategories_admin_returns201() throws Exception {
        when(categoryService.createCategories(any()))
                .thenReturn(new CategoryResponse(1L, "Electronics"));

        mockMvc.perform(post("/api/admin/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Electronics\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));

        verify(categoryService).createCategories(any());
    }

    @Test
    void createCategories_duplicateName_returns409WithError() throws Exception {
        when(categoryService.createCategories(any()))
                .thenThrow(new CategoryAlreadyExistsException("Electronics"));

        mockMvc.perform(post("/api/admin/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Electronics\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createCategories_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategories_asUserRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Electronics\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void updateCategories_admin_returns200WithUpdatedCategory() throws Exception {
        when(categoryService.updateCategories(any(), eq(1L)))
                .thenReturn(new CategoryResponse(1L, "Home Appliances"));

        mockMvc.perform(put("/api/admin/categories/1")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Home Appliances\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Home Appliances"));

        verify(categoryService).updateCategories(any(), eq(1L));
    }

    @Test
    void updateCategories_notFound_returns404() throws Exception {
        when(categoryService.updateCategories(any(), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Category with category id : 99 not found"));

        mockMvc.perform(put("/api/admin/categories/99")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryName\":\"Home Appliances\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteCategories_admin_returns200WithMessage() throws Exception {
        when(categoryService.deleteCategories(1L)).thenReturn("Category removed Successfully");

        mockMvc.perform(delete("/api/admin/categories/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("Category removed Successfully"));

        verify(categoryService).deleteCategories(1L);
    }
}
