package testapp.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import testapp.IntegrationTest;
import testapp.domain.Product;
import testapp.repository.EntityManager;
import testapp.repository.ProductRepository;

/**
 * Integration tests for the {@link ProductResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class ProductResourceIT {

    private static final String DEFAULT_BOOK_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_BOOK_TITLE = "BBBBBBBBBB";

    private static final Double DEFAULT_BOOK_PRICE = 1D;
    private static final Double UPDATED_BOOK_PRICE = 2D;

    private static final Integer DEFAULT_BOOK_QUANTITY = 1;
    private static final Integer UPDATED_BOOK_QUANTITY = 2;

    private static final String ENTITY_API_URL = "/api/products";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Product product;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createEntity(EntityManager em) {
        Product product = new Product().bookTitle(DEFAULT_BOOK_TITLE).bookPrice(DEFAULT_BOOK_PRICE).bookQuantity(DEFAULT_BOOK_QUANTITY);
        return product;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Product createUpdatedEntity(EntityManager em) {
        Product product = new Product().bookTitle(UPDATED_BOOK_TITLE).bookPrice(UPDATED_BOOK_PRICE).bookQuantity(UPDATED_BOOK_QUANTITY);
        return product;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Product.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        product = createEntity(em);
    }

    @Test
    void createProduct() throws Exception {
        int databaseSizeBeforeCreate = productRepository.findAll().collectList().block().size();
        // Create the Product
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isCreated();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeCreate + 1);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getBookTitle()).isEqualTo(DEFAULT_BOOK_TITLE);
        assertThat(testProduct.getBookPrice()).isEqualTo(DEFAULT_BOOK_PRICE);
        assertThat(testProduct.getBookQuantity()).isEqualTo(DEFAULT_BOOK_QUANTITY);
    }

    @Test
    void createProductWithExistingId() throws Exception {
        // Create the Product with an existing ID
        product.setId(1L);

        int databaseSizeBeforeCreate = productRepository.findAll().collectList().block().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    void getAllProducts() {
        // Initialize the database
        productRepository.save(product).block();

        // Get all the productList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(product.getId().intValue()))
            .jsonPath("$.[*].bookTitle")
            .value(hasItem(DEFAULT_BOOK_TITLE))
            .jsonPath("$.[*].bookPrice")
            .value(hasItem(DEFAULT_BOOK_PRICE.doubleValue()))
            .jsonPath("$.[*].bookQuantity")
            .value(hasItem(DEFAULT_BOOK_QUANTITY));
    }

    @Test
    void getProduct() {
        // Initialize the database
        productRepository.save(product).block();

        // Get the product
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, product.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(product.getId().intValue()))
            .jsonPath("$.bookTitle")
            .value(is(DEFAULT_BOOK_TITLE))
            .jsonPath("$.bookPrice")
            .value(is(DEFAULT_BOOK_PRICE.doubleValue()))
            .jsonPath("$.bookQuantity")
            .value(is(DEFAULT_BOOK_QUANTITY));
    }

    @Test
    void getNonExistingProduct() {
        // Get the product
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingProduct() throws Exception {
        // Initialize the database
        productRepository.save(product).block();

        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();

        // Update the product
        Product updatedProduct = productRepository.findById(product.getId()).block();
        updatedProduct.bookTitle(UPDATED_BOOK_TITLE).bookPrice(UPDATED_BOOK_PRICE).bookQuantity(UPDATED_BOOK_QUANTITY);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedProduct.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(updatedProduct))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getBookTitle()).isEqualTo(UPDATED_BOOK_TITLE);
        assertThat(testProduct.getBookPrice()).isEqualTo(UPDATED_BOOK_PRICE);
        assertThat(testProduct.getBookQuantity()).isEqualTo(UPDATED_BOOK_QUANTITY);
    }

    @Test
    void putNonExistingProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, product.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateProductWithPatch() throws Exception {
        // Initialize the database
        productRepository.save(product).block();

        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();

        // Update the product using partial update
        Product partialUpdatedProduct = new Product();
        partialUpdatedProduct.setId(product.getId());

        partialUpdatedProduct.bookPrice(UPDATED_BOOK_PRICE).bookQuantity(UPDATED_BOOK_QUANTITY);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedProduct.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedProduct))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getBookTitle()).isEqualTo(DEFAULT_BOOK_TITLE);
        assertThat(testProduct.getBookPrice()).isEqualTo(UPDATED_BOOK_PRICE);
        assertThat(testProduct.getBookQuantity()).isEqualTo(UPDATED_BOOK_QUANTITY);
    }

    @Test
    void fullUpdateProductWithPatch() throws Exception {
        // Initialize the database
        productRepository.save(product).block();

        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();

        // Update the product using partial update
        Product partialUpdatedProduct = new Product();
        partialUpdatedProduct.setId(product.getId());

        partialUpdatedProduct.bookTitle(UPDATED_BOOK_TITLE).bookPrice(UPDATED_BOOK_PRICE).bookQuantity(UPDATED_BOOK_QUANTITY);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedProduct.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(partialUpdatedProduct))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
        Product testProduct = productList.get(productList.size() - 1);
        assertThat(testProduct.getBookTitle()).isEqualTo(UPDATED_BOOK_TITLE);
        assertThat(testProduct.getBookPrice()).isEqualTo(UPDATED_BOOK_PRICE);
        assertThat(testProduct.getBookQuantity()).isEqualTo(UPDATED_BOOK_QUANTITY);
    }

    @Test
    void patchNonExistingProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, product.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamProduct() throws Exception {
        int databaseSizeBeforeUpdate = productRepository.findAll().collectList().block().size();
        product.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(TestUtil.convertObjectToJsonBytes(product))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Product in the database
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteProduct() {
        // Initialize the database
        productRepository.save(product).block();

        int databaseSizeBeforeDelete = productRepository.findAll().collectList().block().size();

        // Delete the product
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, product.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        List<Product> productList = productRepository.findAll().collectList().block();
        assertThat(productList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
