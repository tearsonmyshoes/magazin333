package com.ecommerce.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Brings databases created by older versions of the application up to date.
 *
 * <p>Hibernate's {@code ddl-auto=update} can add new columns and tables, but it
 * cannot drop the old {@code users.role} column or the old global unique
 * constraint on {@code products.name}. Both are fixed here, after Hibernate has
 * finished its schema update and before the application starts serving requests.</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private static final String USERS_TABLE = "users";
    private static final String PRODUCTS_TABLE = "products";
    private static final String USERS_OLD_TABLE = "users_migration_old";
    private static final String PRODUCTS_NEW_TABLE = "products_migration_new";
    private static final String PRODUCTS_OLD_TABLE = "products_migration_old";

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!isSqlite(connection)) {
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            execute(connection, "PRAGMA foreign_keys = OFF");

            try {
                connection.setAutoCommit(false);

                // Rebuild products first so no foreign key can keep pointing at
                // the old users table after it is renamed below.
                boolean hasLegacyUsers = columns(connection, USERS_TABLE).contains("role");
                migrateProducts(connection, hasLegacyUsers);
                migrateUsers(connection);

                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
                execute(connection, "PRAGMA foreign_keys = ON");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not migrate the database schema", exception);
        }
    }

    private void migrateUsers(Connection connection) throws SQLException {
        if (!columns(connection, USERS_TABLE).contains("role")) {
            return;
        }

        log.info("Removing the obsolete users.role column");

        execute(connection, "DROP TABLE IF EXISTS \"" + USERS_OLD_TABLE + "\"");
        execute(connection, "ALTER TABLE \"" + USERS_TABLE + "\" RENAME TO \"" + USERS_OLD_TABLE + "\"");
        execute(connection, """
                CREATE TABLE "users" (
                    "id" integer,
                    "created_at" timestamp not null,
                    "email" varchar(255) not null unique,
                    "password_hash" varchar(100) not null,
                    primary key ("id")
                )
                """);
        execute(connection,
                "INSERT INTO \"users\" (\"id\", \"created_at\", \"email\", \"password_hash\")"
                        + " SELECT \"id\", \"created_at\", \"email\", \"password_hash\""
                        + " FROM \"" + USERS_OLD_TABLE + "\"");
        execute(connection, "DROP TABLE \"" + USERS_OLD_TABLE + "\"");
    }

    private void migrateProducts(Connection connection, boolean forceRebuild) throws SQLException {
        List<String> columns = columns(connection, PRODUCTS_TABLE);

        if (columns.isEmpty()) {
            return;
        }

        boolean hasSeller = columns.contains("seller_id");
        boolean nameIsGloballyUnique = hasUniqueIndexOn(connection, PRODUCTS_TABLE, "name");

        if (!forceRebuild && hasSeller && !nameIsGloballyUnique) {
            ensureSellerNameIndex(connection);
            return;
        }

        log.info(
                "Rebuilding products table (seller column present: {}, global name constraint: {})",
                hasSeller,
                nameIsGloballyUnique
        );

        execute(connection, "DROP TABLE IF EXISTS \"" + PRODUCTS_NEW_TABLE + "\"");
        execute(connection, "DROP TABLE IF EXISTS \"" + PRODUCTS_OLD_TABLE + "\"");
        execute(connection, """
                CREATE TABLE "products_migration_new" (
                    "id" integer,
                    "created_at" timestamp not null,
                    "description" TEXT,
                    "name" varchar(150) not null,
                    "price" numeric(10,2) not null,
                    "stock" integer not null,
                    "category_id" bigint,
                    "seller_id" bigint,
                    primary key ("id")
                )
                """);

        List<String> newColumns = List.of(
                "id",
                "created_at",
                "description",
                "name",
                "price",
                "stock",
                "category_id",
                "seller_id"
        );

        List<String> copiedColumns = newColumns
                .stream()
                .filter(columns::contains)
                .toList();

        String columnList = String.join(", ", copiedColumns.stream()
                .map(column -> "\"" + column + "\"")
                .toList());

        execute(connection,
                "INSERT INTO \"" + PRODUCTS_NEW_TABLE + "\" (" + columnList + ")"
                        + " SELECT " + columnList + " FROM \"" + PRODUCTS_TABLE + "\"");

        execute(connection, "ALTER TABLE \"" + PRODUCTS_TABLE + "\" RENAME TO \"" + PRODUCTS_OLD_TABLE + "\"");
        execute(connection, "ALTER TABLE \"" + PRODUCTS_NEW_TABLE + "\" RENAME TO \"" + PRODUCTS_TABLE + "\"");
        execute(connection, "DROP TABLE \"" + PRODUCTS_OLD_TABLE + "\"");

        ensureSellerNameIndex(connection);
    }

    private void ensureSellerNameIndex(Connection connection) throws SQLException {
        try {
            execute(connection,
                    "CREATE UNIQUE INDEX IF NOT EXISTS \"ux_products_seller_name\""
                            + " ON \"" + PRODUCTS_TABLE + "\" (\"seller_id\", \"name\" COLLATE NOCASE)");
        } catch (SQLException exception) {
            // Older data may already contain duplicates for one seller. The
            // application still validates this, so do not block startup.
            log.warn("Could not create the per-seller product name index", exception);
        }
    }

    private List<String> columns(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(\"" + table + "\")")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }

        return columns;
    }

    private boolean hasUniqueIndexOn(
            Connection connection,
            String table,
            String column
    ) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA index_list(\"" + table + "\")")) {
            while (resultSet.next()) {
                if (resultSet.getInt("unique") != 1) {
                    continue;
                }

                String indexName = resultSet.getString("name");
                List<String> indexColumns = indexColumns(connection, indexName);

                if (indexColumns.size() == 1 && indexColumns.get(0).equalsIgnoreCase(column)) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<String> indexColumns(Connection connection, String indexName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String escapedName = indexName.replace("\"", "\"\"");

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA index_info(\"" + escapedName + "\")")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }

        return columns;
    }

    private boolean isSqlite(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toLowerCase().contains("sqlite");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
