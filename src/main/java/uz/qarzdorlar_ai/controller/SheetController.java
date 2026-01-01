package uz.qarzdorlar_ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.qarzdorlar_ai.service.sheet.WorkSheetService;

import java.util.List;

@RestController
@RequestMapping("/api/open/work/sheet")
@RequiredArgsConstructor
public class SheetController {

    private final WorkSheetService workSheetService;

    @GetMapping("/{range}")
    public Object range(@PathVariable String range){

        return workSheetService.readRange(range);

    }
}
