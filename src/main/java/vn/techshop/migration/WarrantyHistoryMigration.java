package vn.techshop.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Shared V22 logic: database-generated V9 unique constraint names differ across H2 databases. */
public abstract class WarrantyHistoryMigration extends BaseJavaMigration {
    @Override public Integer getChecksum() {return 220001;}
    @Override public void migrate(Context context) throws SQLException {
        Connection connection=context.getConnection();
        boolean h2="H2".equals(connection.getMetaData().getDatabaseProductName());
        List<String> oldUnique=new ArrayList<>();
        String constraints="select tc.constraint_name from information_schema.table_constraints tc "
            +"join information_schema.key_column_usage k on k.constraint_schema=tc.constraint_schema and k.constraint_name=tc.constraint_name and k.table_name=tc.table_name "
            +"where tc.constraint_type='UNIQUE' and lower(tc.table_name)='warranty_cards' and lower(k.column_name)='imei_number' and tc.constraint_schema="
            +(h2?"current_schema()":"database()");
        try(var query=connection.createStatement();var rows=query.executeQuery(constraints)) {
            while(rows.next())oldUnique.add(rows.getString(1));
        }
        if(oldUnique.size()!=1)throw new SQLException("Expected exactly one V9 warranty serial unique constraint.");
        try(var ddl=connection.createStatement()) {
            ddl.execute("alter table warranty_cards add column revoked_at timestamp(6) null");
            ddl.execute("alter table warranty_cards add column active_imei_number varchar(40) generated always as (case when revoked_at is null then imei_number else null end)"+(h2?"":" stored"));
            // Enforce active uniqueness before dropping the old global unique constraint.
            ddl.execute("create unique index uq_warranty_active_serial on warranty_cards(active_imei_number)");
            ddl.execute("create unique index uq_warranty_order_serial on warranty_cards(order_id,imei_number)");
            String name=oldUnique.getFirst();
            String quoted=h2?'"'+name.replace("\"","\"\"")+'"':'`'+name.replace("`","``")+'`';
            ddl.execute("alter table warranty_cards drop "+(h2?"constraint ":"index ")+quoted);
        }
    }
}
