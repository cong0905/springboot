package vn.techshop.shared;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path directory;
    @Test void rejectsMagicHeaderWithoutDecodableImageAndLeavesNoFile() throws Exception {
        var storage = new FileStorageService(directory.toString());
        var fake = new MockMultipartFile("file","../fake.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10});
        assertThatThrownBy(()->storage.store(fake)).isInstanceOf(BusinessException.class);
        try(var files=Files.list(directory)){assertThat(files.count()).isZero();}
    }
    @Test void persistsDecodedImageAcrossServiceRestartAndRejectsTraversal() throws Exception {
        var image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);
        var bytes=new ByteArrayOutputStream(); ImageIO.write(image,"png",bytes);
        var storage=new FileStorageService(directory.toString());
        var path=storage.store(new MockMultipartFile("file","../../x.txt","image/png",bytes.toByteArray()));
        var restarted=new FileStorageService(directory.toString());
        var filename=path.substring(path.lastIndexOf('/')+1);
        assertThat(ImageIO.read(restarted.uploadDirectory().resolve(filename).toFile()).getWidth()).isEqualTo(2);
        assertThat(restarted.delete("/assets/products/uploads/../outside.png")).isFalse();
        assertThat(restarted.delete(path)).isTrue();
    }
    @Test void rejectsPixelBombBeforeDecode() throws Exception {
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",bytes);
        byte[] png=bytes.toByteArray();
        java.nio.ByteBuffer.wrap(png,16,8).putInt(8192).putInt(8192);
        var crc=new java.util.zip.CRC32();crc.update(png,12,17);
        java.nio.ByteBuffer.wrap(png,29,4).putInt((int)crc.getValue());
        assertThatThrownBy(()->new FileStorageService(directory.toString()).store(new MockMultipartFile("file","bomb.png","image/png",png)))
            .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getMessage()).contains("pixel"));
    }
    @Test void rejectsSizeAndMimeMismatch() throws Exception {
        var storage=new FileStorageService(directory.toString());
        assertThatThrownBy(()->storage.store(new MockMultipartFile("file","big.png","image/png",new byte[5*1024*1024+1])))
            .isInstanceOf(BusinessException.class);
        var bytes=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",bytes);
        assertThatThrownBy(()->storage.store(new MockMultipartFile("file","x.jpg","image/jpeg",bytes.toByteArray())))
            .isInstanceOf(BusinessException.class);
    }
    @Test void decodesAndPersistsRealWebp() throws Exception {
        byte[] bytes;try(var resource=getClass().getResourceAsStream("/fixtures/valid.webp")){bytes=resource.readAllBytes();}
        var storage=new FileStorageService(directory.toString());
        var path=storage.store(new MockMultipartFile("file","real.webp","image/webp",bytes));
        var image=ImageIO.read(storage.uploadDirectory().resolve(path.substring(path.lastIndexOf('/')+1)).toFile());
        assertThat(image.getWidth()).isEqualTo(2);assertThat(image.getHeight()).isEqualTo(2);
    }
}
