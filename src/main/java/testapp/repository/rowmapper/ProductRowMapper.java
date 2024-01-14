package testapp.repository.rowmapper;

import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import testapp.domain.Product;

/**
 * Converter between {@link Row} to {@link Product}, with proper type conversions.
 */
@Service
public class ProductRowMapper implements BiFunction<Row, String, Product> {

    private final ColumnConverter converter;

    public ProductRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Product} stored in the database.
     */
    @Override
    public Product apply(Row row, String prefix) {
        Product entity = new Product();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setBookTitle(converter.fromRow(row, prefix + "_book_title", String.class));
        entity.setBookPrice(converter.fromRow(row, prefix + "_book_price", Double.class));
        entity.setBookQuantity(converter.fromRow(row, prefix + "_book_quantity", Integer.class));
        return entity;
    }
}
