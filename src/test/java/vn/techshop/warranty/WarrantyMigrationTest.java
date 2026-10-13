package vn.techshop.warranty;

import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class WarrantyMigrationTest {
    @Test void upgradeKeepsExistingCardsAndRepairsAndEnforcesOneActiveWarrantyPerSerial() throws Exception {
        String url="jdbc:h2:mem:warranty-upgrade-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        // H2 check expressions retain their creating session; keep physical connections alive like the app.
        try(var source=new HikariDataSource()) {
        source.setJdbcUrl(url);source.setUsername("sa");source.setPassword("");source.setMaximumPoolSize(2);
        Flyway.configure().dataSource(source).locations("classpath:db/demo").target("21").load().migrate();
        try(var connection=source.getConnection();var sql=connection.createStatement()) {
            sql.execute("insert into users(id,full_name,email,password_hash,role,created_at) values(1,'Migration fixture','migration@test.local','not-a-login-hash','CUSTOMER',current_timestamp)");
            for(int id=1;id<=3;id++)sql.execute("insert into orders(id,user_id,checkout_key,request_hash,status,recipient_name,phone,address,subtotal,shipping_fee,total_amount,created_at,updated_at) values("+id+",1,'"+UUID.randomUUID()+"','"+"0".repeat(64)+"','PENDING','Migration fixture','0901234567','Local test only',0,0,0,current_timestamp,current_timestamp)");
            sql.execute("insert into warranty_cards(id,imei_number,order_id,customer_id,start_date,end_date,warranty_type) values(1,'MIGRATION-SERIAL',1,1,current_date,current_date,'Original warranty')");
            sql.execute("insert into repair_tickets(id,warranty_id,issue,status,created_at) values(1,1,'Original repair','RECEIVED',current_timestamp)");
        }
        var latest=Flyway.configure().dataSource(source).locations("classpath:db/demo").target("23").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(2);
        try(var connection=source.getConnection();var sql=connection.createStatement()) {
            try(var rows=sql.executeQuery("select w.warranty_type,t.issue,w.revoked_at,t.imei_number from warranty_cards w join repair_tickets t on t.warranty_id=w.id where w.id=1")) {
                assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("Original warranty");assertThat(rows.getString(2)).isEqualTo("Original repair");assertThat(rows.getTimestamp(3)).isNull();assertThat(rows.getString(4)).isEqualTo("MIGRATION-SERIAL");
            }
            sql.execute("update warranty_cards set revoked_at=current_timestamp where id=1");
            String insert="insert into warranty_cards(imei_number,order_id,customer_id,start_date,end_date,warranty_type) values('MIGRATION-SERIAL',%d,1,current_date,current_date,'New warranty')";
            sql.execute(insert.formatted(2));
            assertThatThrownBy(()->sql.execute(insert.formatted(3))).isInstanceOf(java.sql.SQLException.class).hasMessageContaining("UQ_WARRANTY_ACTIVE_SERIAL");
            try(var rows=sql.executeQuery("select count(*) from repair_tickets where warranty_id=1")){rows.next();assertThat(rows.getInt(1)).isEqualTo(1);}
        }
        assertThat(latest.validateWithResult().validationSuccessful).isTrue();
        assertThat(latest.migrate().migrationsExecuted).isZero();
        }
    }
}
