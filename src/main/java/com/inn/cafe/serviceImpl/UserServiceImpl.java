package com.inn.cafe.serviceImpl;

import com.google.common.base.Strings;
import com.inn.cafe.JWT.CustomerUsersDetailsService;
import com.inn.cafe.JWT.JwtFilter;
import com.inn.cafe.JWT.JwtUtil;
import com.inn.cafe.POJO.User;
import com.inn.cafe.Service.UserService;
import com.inn.cafe.constents.CafeConstants;
import com.inn.cafe.dao.UserDao;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.utils.EmailUtils;
import com.inn.cafe.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    AuthenticationManager  authenticationManager;

    @Autowired
    CustomerUsersDetailsService customerUsersDetailsService;

    @Autowired
    JwtUtil jwtUtil;
    @Autowired
    JwtFilter jwtFilter;
    @Autowired
    EmailUtils emailUtils;
    @Override
    public ResponseEntity<String> signup(Map<String, String> requestMap) {
        log.info("Inside signup {}", requestMap);
        try {
            if(this.validateSignUpMap(requestMap)){
                User user = userDao.findByEmailId(requestMap.get("email"));
                if(Objects.isNull(user)){
                    userDao.save(this.getUserFromMap(requestMap));
                    return CafeUtils.getResponseEntity("Successfully Registered", HttpStatus.OK);
                } else {
                    return CafeUtils.getResponseEntity("Email already exists.", HttpStatus.BAD_REQUEST);
                }

            } else {
                return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> login(Map<String, String> requestMap) {
        log.info("Inside login");
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password")));
            if(authentication.isAuthenticated()){
                if(customerUsersDetailsService.getUserDetail().getStatus().equalsIgnoreCase("true")){
                    return new ResponseEntity<String>("{\"token\":\"" +
                            jwtUtil.generateToken(customerUsersDetailsService.getUserDetail().getEmail(),customerUsersDetailsService.getUserDetail().getRole()) + "\"}",HttpStatus.OK);
                } else {
                    return new ResponseEntity<String>("{\"message\":\""+"Wait for admin approval." + "\"}", HttpStatus.BAD_REQUEST);
                }
            }

        } catch (Exception exception){
            log.error("{}", exception);
        }
        return new ResponseEntity<String>("{\"message\":\""+"Bad credentials." + "\"}", HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try {
            if(jwtFilter.isAdmin()){
                return new ResponseEntity<>(userDao.getAllUser(), HttpStatus.OK);
            }
            else{
                return new ResponseEntity<>(new ArrayList<>(), HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return new ResponseEntity<>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
        try {
            if(jwtFilter.isAdmin()){
                Optional<User> user = userDao.findById(Integer.parseInt(requestMap.get("id")));
                if(!user.isEmpty()){
                    userDao.updateStatus(requestMap.get("status"), Integer.parseInt(requestMap.get("id")));
                    this.sendMailToAllAdmin(requestMap.get("status"), user.get().getEmail(), userDao.getAllAdmin());
                    return CafeUtils.getResponseEntity("User status updated successfully", HttpStatus.OK);
                } else {
//                    return CafeUtils.getResponseEntity("User id does not exist", HttpStatus.NO_CONTENT);
                    return CafeUtils.getResponseEntity("User id does not exist", HttpStatus.OK);
                }
            } else{
                return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS,HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> checkToken() {
        return CafeUtils.getResponseEntity("true", HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> changePassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmail(jwtFilter.getCurrentUser());
            if(!user.equals(null)){
                if(user.getPassword().equals(requestMap.get("oldPassword"))){
                    user.setPassword(requestMap.get("newPassword"));
                    userDao.save(user);
                    return CafeUtils.getResponseEntity("Password has been updated successfully", HttpStatus.OK);
                }
                return CafeUtils.getResponseEntity("Incorrect Old password", HttpStatus.BAD_REQUEST);
            }
            return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<String> forgotPassword(Map<String, String> requestMap) {
        try {
            User user = userDao.findByEmail(requestMap.get("email"));
            if(!Objects.isNull(user) && !Strings.isNullOrEmpty(user.getEmail()))
                emailUtils.forgotMail(user.getEmail(), "Credentials by Cafe Management System", user.getPassword());
            return CafeUtils.getResponseEntity("Check your email for credentials", HttpStatus.BAD_GATEWAY);
        } catch (Exception exception){
            exception.printStackTrace();
        }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateSignUpMap(Map<String, String> requestMap){
        if( requestMap.containsKey("name")
                && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email")
                && requestMap.containsKey("password")){
            return true;
        }else {
            return false;
        }
    }

    private User getUserFromMap(Map<String, String> requestMap){
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }
    private void sendMailToAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if(status!=null && status.equalsIgnoreCase("true"))
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Approved","User:- " + user + "\n is approved by \nADMIN:- " +jwtFilter.getCurrentUser(), allAdmin);
        else
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(), "Account Disabled","User:- " + user + "\n is disabled by \nADMIN:- " +jwtFilter.getCurrentUser(), allAdmin);
    }

}
