package com.inn.cafe.Service;

import com.inn.cafe.POJO.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

public interface CategoryService {
    public ResponseEntity<String> addNewCategory(Map<String, String> requestMap);
    public ResponseEntity<List<Category>> getAllCategory(String filterValue);
    public ResponseEntity<String> updateCategory(Map<String, String> requestMap);
}
