package vn.techshop.content;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import vn.techshop.identity.TechPrincipal;
@Controller
public class NewsController {
 private final NewsService news;public NewsController(NewsService news){this.news=news;}
 @GetMapping("/news") String news(@RequestParam(defaultValue="0") int page,Model model){model.addAttribute("articles",news.list(false,page));return "news";}
 @GetMapping("/news/{slug}") String article(@PathVariable String slug,Model model){model.addAttribute("article",news.published(slug));return "news-detail";}
 @GetMapping("/admin/news") String admin(@RequestParam(defaultValue="0") int page,Model model){model.addAttribute("articles",news.list(true,page));model.addAttribute("article",null);return "admin/news";}
 @GetMapping("/admin/news/{id}") String edit(@PathVariable Long id,Model model){model.addAttribute("article",news.detail(id));model.addAttribute("articles",news.list(true,0));return "admin/news";}
 @PostMapping("/api/v1/admin/news") @ResponseBody NewsService.Article create(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody NewsService.Input input){return news.save(user.id(),null,input);}
 @PatchMapping("/api/v1/admin/news/{id}") @ResponseBody NewsService.Article update(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody NewsService.Input input){return news.save(user.id(),id,input);}
}
