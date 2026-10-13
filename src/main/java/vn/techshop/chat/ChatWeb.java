package vn.techshop.chat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatWeb {
    @GetMapping("/chat") String customer(Model model){model.addAttribute("chatAdmin",false);return "chat";}
    @GetMapping("/admin/chat") String admin(Model model){model.addAttribute("chatAdmin",true);return "chat";}
}
