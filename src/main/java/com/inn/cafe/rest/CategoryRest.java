package com.inn.cafe.rest;

import com.inn.cafe.POJO.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequestMapping("/category")
public interface CategoryRest {
    @PostMapping("/add")
    public ResponseEntity<String> addNewCategory(@RequestBody Map<String, String> requestMap);
    @GetMapping("/get")
    public ResponseEntity<List<Category>> getAllCategory(@RequestParam(required = false) String filterValue);
    @PostMapping("/update")
    public ResponseEntity<String> updateCategory(@RequestBody Map<String, String> requestMap);

}
