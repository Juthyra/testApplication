package testapp.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class ProductSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("book_title", table, columnPrefix + "_book_title"));
        columns.add(Column.aliased("book_price", table, columnPrefix + "_book_price"));
        columns.add(Column.aliased("book_quantity", table, columnPrefix + "_book_quantity"));

        return columns;
    }
}
