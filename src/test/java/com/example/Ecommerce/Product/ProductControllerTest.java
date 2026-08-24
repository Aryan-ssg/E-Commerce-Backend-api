package com.example.Ecommerce.Product;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Common.DTOs.PagedResponse;
import com.example.Ecommerce.Common.Exceptions.ResourceNotFoundException;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.JwtAuthenticationEntryPoint;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Common.SecurityConfig;
import com.example.Ecommerce.Product.DTOs.request.ProductRequest;
import com.example.Ecommerce.Product.DTOs.request.UpdateProductRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;
import com.example.Ecommerce.Product.DTOs.response.ProductResponse;

import static org.assertj.core.api.Assertions.assertThat;
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

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductServiceImpl productService;

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

    private ProductResponse productResponse() {
        return new ProductResponse(100L, "Headphones", 2000, "Electronics", null);
    }

    private ProductAdminResponse productAdminResponse() {
        return new ProductAdminResponse(100L, "Headphones", 2000, "Electronics", 5, null);
    }

    @Test
    void getAllProducts_public_returns200WithPagedContent() throws Exception {
        PagedResponse<ProductResponse> paged = new PagedResponse<>(
                List.of(productResponse()), 0, 20, 1, 1, true);
        when(productService.getAllProducts(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(paged);

        mockMvc.perform(get("/api/public/products").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].productId").value(100))
                .andExpect(jsonPath("$.content[0].productName").value("Headphones"))
                .andExpect(jsonPath("$.content[0].categoryName").value("Electronics"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getProductById_public_returns200() throws Exception {
        when(productService.getProductById(100L)).thenReturn(productResponse());

        mockMvc.perform(get("/api/public/products/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Headphones"))
                .andExpect(jsonPath("$.productPrice").value(2000));

        verify(productService).getProductById(100L);
    }

    @Test
    void getProductById_notFound_returns404WithErrorBody() throws Exception {
        when(productService.getProductById(99L))
                .thenThrow(new ResourceNotFoundException("Product with productid : 99 not found"));

        mockMvc.perform(get("/api/public/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product with productid : 99 not found"));
    }

    @Test
    void createProduct_admin_returns201WithCreatedProduct() throws Exception {
        when(productService.createProduct(eq(1L), any(ProductRequest.class))).thenReturn(productAdminResponse());

        mockMvc.perform(post("/api/admin/categories/1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Headphones\",\"productPrice\":2000,\"stock\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(100))
                .andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.stock").value(5));

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(eq(1L), captor.capture());
        assertThat(captor.getValue().getProductName()).isEqualTo("Headphones");
        assertThat(captor.getValue().getStock()).isEqualTo(5);
    }

    @Test
    void createProduct_asUserRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/categories/1/products")
                        .with(user("shopper").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Headphones\",\"productPrice\":2000,\"stock\":5}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_negativePrice_returns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/api/admin/categories/1/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Headphones\",\"productPrice\":-5,\"stock\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", Matchers.containsString("productPrice")));

        verifyNoInteractions(productService);
    }

    @Test
    void updateProduct_admin_returns200WithUpdatedProduct() throws Exception {
        when(productService.updateProduct(eq(100L), any(UpdateProductRequest.class)))
                .thenReturn(new ProductAdminResponse(100L, "Wireless Headphones", 2500, "Electronics", 5, null));

        mockMvc.perform(put("/api/admin/products/100")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Wireless Headphones\",\"productPrice\":2500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Wireless Headphones"));

        verify(productService).updateProduct(eq(100L), any());
    }

    @Test
    void deleteProduct_admin_returns200WithMessage() throws Exception {
        mockMvc.perform(delete("/api/admin/products/100").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("Product with ProductId : 100 successfully deleted"));

        verify(productService).deleteProduct(100L);
    }
}
