package kr.co.firstdayproject.controller.corp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/corp/job")
public class CorpJobController {

    @GetMapping({"", "/list"})
    public String list(Model model) {
        prepare(model);
        return "corp/job-list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        prepare(model);
        return "corp/job-create";
    }

    @GetMapping("/edit")
    public String edit(
            @RequestParam(defaultValue = "false") boolean hidden,
            Model model
    ) {
        prepare(model);
        prepareEditState(model, hidden);
        return "corp/job-edit";
    }

    @GetMapping("/hidden-edit")
    public String hiddenEdit(Model model) {
        prepare(model);
        prepareEditState(model, true);
        return "corp/job-edit";
    }

    private void prepare(Model model) {
        model.addAttribute("activeMenu", "jobs");
        model.addAttribute("companyStatus", "APPROVED");
    }

    private void prepareEditState(Model model, boolean hidden) {
        model.addAttribute("isHidden", hidden);
        if (hidden) {
            model.addAttribute("hiddenReason", "회사 정보와 공고 내용 불일치");
        }
    }
}
