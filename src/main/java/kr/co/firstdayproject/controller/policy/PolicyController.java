package kr.co.firstdayproject.controller.policy;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/policy")
public class PolicyController {

    @GetMapping("/terms")
    public String terms() { return "policy/terms"; }

    @GetMapping("/privacy")
    public String privacy() { return "policy/privacy"; }
}
