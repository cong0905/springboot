package vn.techshop.identity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.techshop.shared.*;
@RestController
public class AvatarApi {
 private final UserRepository users;private final FileStorageService files;
 public AvatarApi(UserRepository users,FileStorageService files){this.users=users;this.files=files;}
 @PostMapping("/api/v1/me/avatar") @Transactional
 ResponseEntity<Void> upload(@AuthenticationPrincipal TechPrincipal principal,@RequestParam Long expectedVersion,@RequestParam MultipartFile file){
  var user=users.lockById(principal.id()).orElseThrow(BusinessException::missing);Rules.version(user.getVersion(),expectedVersion,"PROFILE_CHANGED");
  String previous=user.getAvatarPath(),path=files.store(file);
  org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
   public void afterCompletion(int status){if(status==STATUS_COMMITTED){if(previous!=null)files.delete(previous);}else files.delete(path);}
  });
  user.setAvatarPath(path);users.saveAndFlush(user);return ResponseEntity.noContent().build();
 }
}
