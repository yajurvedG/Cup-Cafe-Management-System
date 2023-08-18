package com.inn.cafe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.http.ResponseEntity;

import java.util.Map;


public interface DashboardService {

    public ResponseEntity<Map<String, Object>> getCount();
}
