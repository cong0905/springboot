package vn.techshop.order;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.shared.BusinessException;

@RestController
public class PickupQrController {
    private final OrderService orders;
    private final Clock clock;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public PickupQrController(OrderService orders,Clock clock,org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.orders=orders;
        this.clock=clock;
        this.jdbc=jdbc;
    }

    @GetMapping(value={"/api/v1/orders/{id}/pickup-qr.png","/api/v1/admin/orders/{id}/pickup-qr.png"},produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable Long id,@AuthenticationPrincipal TechPrincipal user) {
        var order=orders.detail(id,user.id(),"ADMIN".equals(user.role()));
        if(order.pickupCode()==null||!"CLICK_AND_COLLECT".equals(order.fulfillmentType())
            ||!List.of("PENDING","CONFIRMED","SHIPPED").contains(order.status()))throw BusinessException.missing();
        java.time.Instant hold = order.holdExpiresAt();
        try {
            var ts = jdbc.queryForObject("select hold_expires_at from orders where id=?", java.sql.Timestamp.class, id);
            if (ts != null) hold = ts.toInstant();
        } catch (Exception ignored) {}
        if(!"PAID".equals(order.paymentStatus())&&hold!=null
            &&!clock.instant().isBefore(hold)&&List.of("PENDING","CONFIRMED").contains(order.status()))
            throw BusinessException.conflict("PICKUP_EXPIRED","Đã hết hạn giữ máy.");
        try {
            var matrix=new QRCodeWriter().encode(order.pickupCode(),BarcodeFormat.QR_CODE,256,256,Map.of(EncodeHintType.MARGIN,4));
            var image=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<256;y++)for(int x=0;x<256;x++)image.setRGB(x,y,matrix.get(x,y)?0x000000:0xFFFFFF);
            var bytes=new ByteArrayOutputStream();ImageIO.write(image,"PNG",bytes);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .contentType(MediaType.IMAGE_PNG).body(bytes.toByteArray());
        } catch(WriterException|IOException ex) {throw new IllegalStateException("Không tạo được QR nhận máy.",ex);}
    }
}
